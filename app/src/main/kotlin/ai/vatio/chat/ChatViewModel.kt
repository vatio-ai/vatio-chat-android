package ai.vatio.chat

import ai.vatio.Vatio
import ai.vatio.VatioChat
import ai.vatio.VatioConfig
import ai.vatio.VatioConversation
import ai.vatio.VatioException
import ai.vatio.VatioFeedback
import ai.vatio.VatioUpload
import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Owns everything the screens show: the Vatio client, the current chat, the
 * agent's config, this visitor's conversations, the composer's draft and the
 * settings.
 *
 * The screens collect the chat's own StateFlows (messages, isTyping, status,
 * feedback) straight from [chat]; this class holds what the SDK does not.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = SettingsStore(application)

    var settings by mutableStateOf(settingsStore.load())
        private set

    private var vatio = makeVatio(settings)

    var chat by mutableStateOf(VatioChat(vatio, visitorToken = settings.visitorToken))
        private set

    /** The agent's name, avatar, accent color, greeting and suggestions. Null until loaded. */
    var config by mutableStateOf<VatioConfig?>(null)
        private set

    /** This visitor's conversations, newest first. Null until first loaded. */
    var conversations by mutableStateOf<List<VatioConversation>?>(null)
        private set

    var conversationsError by mutableStateOf<String?>(null)
        private set

    /** The conversation on screen, highlighted in the drawer. */
    var currentConversationId by mutableStateOf<Long?>(null)
        private set

    /** A stored or picked conversation is loading and has nothing to show yet. */
    var isLoadingConversation by mutableStateOf(false)
        private set

    var isAnsweringFeedback by mutableStateOf(false)
        private set

    /** The composer's contents, here so a failed send can put them back. */
    var draft by mutableStateOf(Draft())
        private set

    /** One line for the snackbar. The screen calls [noticeShown] once it is up. */
    var notice by mutableStateOf<String?>(null)
        private set

    /** What Vatio checks against the token's allowed origins. */
    val origin: String get() = "android-app://${vatio.appId.lowercase()}"

    private var resuming: Job? = null
    private var loadingConfig: Job? = null
    private var watchingErrors: Job? = null

    init {
        watchErrors()
    }

    // The conversation

    /**
     * Reopens the visitor's stored conversation, if there is one, and loads
     * the agent's config if it is missing. The chat screen calls it whenever
     * it appears and whenever the chat is replaced.
     */
    fun resume() {
        if (config == null) loadConfig()
        if (resuming?.isActive == true) return
        val chat = chat
        resuming = viewModelScope.launch {
            isLoadingConversation = true
            try {
                // Failures land in chat.lastError rather than being thrown.
                chat.resume()
            } finally {
                if (chat === this@ChatViewModel.chat) {
                    isLoadingConversation = false
                    currentConversationId = chat.conversationId
                }
            }
        }
    }

    fun updateText(text: String) {
        draft = draft.copy(text = text.take(Limits.MESSAGE_CHARACTERS))
    }

    /** Adds picked photos to the draft. Reading them is file I/O, so it is off the main thread. */
    fun attach(uris: List<Uri>) {
        viewModelScope.launch {
            val room = Limits.FILES_PER_MESSAGE - draft.attachments.size
            val picked = uris.take(room)
            val uploads = withContext(Dispatchers.IO) { picked.mapNotNull(::readUpload) }
            val (fitting, tooLarge) = uploads.partition { it.bytes.size <= Limits.FILE_BYTES }
            when {
                uris.size > room -> notice = "Up to ${Limits.FILES_PER_MESSAGE} files per message."
                uploads.size < picked.size -> notice = "Couldn't read one of the photos."
                tooLarge.isNotEmpty() -> notice = "${tooLarge.first().filename} is over 8 MB."
            }
            draft = draft.copy(attachments = (draft.attachments + fitting).take(Limits.FILES_PER_MESSAGE))
        }
    }

    fun removeAttachment(upload: VatioUpload) {
        draft = draft.copy(attachments = draft.attachments - upload)
    }

    /** Sends the draft. The composer empties at once; the message shows as pending. */
    fun send() {
        val message = draft
        if (message.isEmpty) return
        draft = Draft()
        deliver(message)
    }

    fun sendSuggestion(text: String) = deliver(Draft(text = text))

    /** Sends a finished recording as a voice note, read off the main thread. */
    fun sendVoiceNote(recording: File) {
        viewModelScope.launch {
            val upload = withContext(Dispatchers.IO) {
                try {
                    VatioUpload.fromFile(recording)
                } catch (_: IOException) {
                    null
                } finally {
                    recording.delete()
                }
            }
            if (upload == null) notice = "Couldn't read the recording." else deliver(Draft(attachments = listOf(upload)))
        }
    }

    /** Forgets the current conversation; the next message starts a new one. */
    fun startNewChat() {
        chat.startOver()
        currentConversationId = null
    }

    private fun deliver(message: Draft) {
        val chat = chat
        val startsConversation = chat.conversationId == null
        viewModelScope.launch {
            try {
                chat.send(message.text, message.attachments)
            } catch (e: VatioException) {
                // The SDK has already taken the pending message out of the
                // list, so the text and files go back in the composer.
                if (chat === this@ChatViewModel.chat) draft = message.restoredInto(draft)
                show(e)
                return@launch
            }
            if (chat !== this@ChatViewModel.chat) return@launch
            currentConversationId = chat.conversationId
            if (startsConversation) loadConversations()
        }
    }

    // Conversations

    fun loadConversations() {
        val chat = chat
        viewModelScope.launch {
            try {
                // The chat's own visitor token: a signed-in person's
                // conversations are listed only alongside it.
                val list = chat.vatio.conversations(chat.visitorToken)
                if (chat === this@ChatViewModel.chat) {
                    conversations = list
                    conversationsError = null
                }
            } catch (e: VatioException) {
                if (chat === this@ChatViewModel.chat) conversationsError = e.userMessage(origin)
            }
        }
    }

    fun open(conversation: VatioConversation) {
        if (conversation.id == currentConversationId) return
        val chat = chat
        viewModelScope.launch {
            isLoadingConversation = true
            try {
                chat.open(conversation)
            } catch (e: VatioException) {
                // Nothing of it loaded, and the chat would sit half-connected:
                // start clean instead. It can be picked again from the drawer.
                chat.startOver()
                show(e)
            } finally {
                if (chat === this@ChatViewModel.chat) {
                    isLoadingConversation = false
                    currentConversationId = chat.conversationId
                }
            }
        }
    }

    // Feedback

    fun rate(rating: VatioFeedback.Rating, comment: String?) = answerFeedback { chat ->
        chat.rate(rating, comment)
        notice = "Thanks for the feedback."
    }

    fun dismissFeedback() = answerFeedback { chat -> chat.dismissFeedback() }

    private fun answerFeedback(answer: suspend (VatioChat) -> Unit) {
        val chat = chat
        viewModelScope.launch {
            isAnsweringFeedback = true
            try {
                answer(chat)
            } catch (e: VatioException) {
                show(e)
            } finally {
                isAnsweringFeedback = false
            }
        }
    }

    // Settings

    /** Saves new settings and starts over with a client and a chat for them. */
    fun saveSettings(new: Settings) {
        settingsStore.save(new)
        replaceChat(new)
    }

    /** Drops every override and goes back to Vatio's own agent. */
    fun useVatioAgent() {
        settingsStore.clear()
        replaceChat(Settings())
    }

    // A VatioChat takes its visitor token when it is created, so new settings
    // mean a new chat. The chat screen resumes it when it next appears.
    private fun replaceChat(new: Settings) {
        chat.close()
        resuming?.cancel()
        loadingConfig?.cancel()
        settings = new
        vatio = makeVatio(new)
        chat = VatioChat(vatio, visitorToken = new.visitorToken)
        config = null
        conversations = null
        conversationsError = null
        currentConversationId = null
        isLoadingConversation = false
        watchErrors()
    }

    // Notices

    fun showNotice(message: String) {
        notice = message
    }

    fun noticeShown() {
        notice = null
    }

    override fun onCleared() {
        chat.close()
    }

    private fun loadConfig() {
        if (loadingConfig?.isActive == true) return
        val vatio = vatio
        loadingConfig = viewModelScope.launch {
            try {
                val loaded = vatio.config()
                if (vatio === this@ChatViewModel.vatio) config = loaded
            } catch (e: VatioException) {
                show(e)
            }
        }
    }

    // What goes wrong outside a send() (a reply that could not be fetched, a
    // credential that expired) lands in chat.lastError instead of a throw.
    private fun watchErrors() {
        watchingErrors?.cancel()
        val chat = chat
        watchingErrors = viewModelScope.launch {
            chat.lastError.collect { error ->
                // resume() reports a stored conversation that turned out to
                // belong to someone else this way. To the person using the
                // app, that is simply a new chat.
                if (error != null && error.code != "no_conversation") show(error)
            }
        }
    }

    private fun show(error: VatioException) {
        notice = error.userMessage(origin)
    }

    private fun readUpload(uri: Uri): VatioUpload? = try {
        VatioUpload.fromUri(getApplication(), uri)
    } catch (_: VatioException) {
        null
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }

    private fun makeVatio(settings: Settings) = Vatio(
        getApplication(),
        workspace = settings.workspace,
        token = settings.token,
        baseUrl = settings.baseUrl,
    )
}
