package ai.vatio.chat.ui

import ai.vatio.VatioChat
import ai.vatio.chat.ChatViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * The main screen: the conversation, the composer under it, and the
 * conversations drawer to its left.
 */
@Composable
fun ChatScreen(viewModel: ChatViewModel, onOpenSettings: () -> Unit) {
    // The SDK keeps the conversation as StateFlows; collecting them is all it
    // takes for the screen to follow the chat.
    val chat = viewModel.chat
    val messages by chat.messages.collectAsStateWithLifecycle()
    val isTyping by chat.isTyping.collectAsStateWithLifecycle()
    val status by chat.status.collectAsStateWithLifecycle()
    val feedback by chat.feedback.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val agentName = viewModel.config?.agentName ?: "Vatio Chat"

    // When the screen appears, and again whenever Settings replaced the chat.
    LaunchedEffect(chat) { viewModel.resume() }

    // Fresh each time the drawer opens, so a conversation started on another
    // screen or a moment ago is there.
    LaunchedEffect(drawerState.targetValue) {
        if (drawerState.targetValue == DrawerValue.Open) viewModel.loadConversations()
    }

    viewModel.notice?.let { notice ->
        LaunchedEffect(notice) {
            snackbarHostState.showSnackbar(notice, withDismissAction = true)
            viewModel.noticeShown()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationsDrawer(
                drawerState = drawerState,
                conversations = viewModel.conversations,
                error = viewModel.conversationsError,
                currentConversationId = viewModel.currentConversationId,
                onNewChat = {
                    viewModel.startNewChat()
                    scope.launch { drawerState.close() }
                },
                onOpen = { conversation ->
                    viewModel.open(conversation)
                    scope.launch { drawerState.close() }
                },
                onOpenSettings = onOpenSettings,
            )
        },
    ) {
        Scaffold(
            topBar = {
                ChatTopBar(
                    title = agentName,
                    status = status,
                    canStartOver = messages.isNotEmpty(),
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNewChat = viewModel::startNewChat,
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding(),
            ) {
                val content = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                when {
                    messages.isNotEmpty() -> MessageList(
                        messages = messages,
                        isTyping = isTyping,
                        feedback = feedback,
                        isAnsweringFeedback = viewModel.isAnsweringFeedback,
                        onRate = viewModel::rate,
                        onDismissFeedback = viewModel::dismissFeedback,
                        modifier = content,
                    )
                    viewModel.isLoadingConversation -> Box(content, contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    else -> EmptyState(viewModel.config, onSuggestion = viewModel::sendSuggestion, modifier = content)
                }
                Composer(
                    draft = viewModel.draft,
                    onTextChange = viewModel::updateText,
                    onAttach = viewModel::attach,
                    onRemoveAttachment = viewModel::removeAttachment,
                    onSend = viewModel::send,
                    onVoiceNote = viewModel::sendVoiceNote,
                    onProblem = viewModel::showNotice,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    title: String,
    status: VatioChat.Status,
    canStartOver: Boolean,
    onOpenDrawer: () -> Unit,
    onNewChat: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // Only while there is something to wait for; a live or idle
                // chat needs no label.
                val connection = when (status) {
                    VatioChat.Status.CONNECTING -> "Connecting…"
                    VatioChat.Status.RECONNECTING -> "Reconnecting…"
                    else -> null
                }
                if (connection != null) {
                    Text(
                        connection,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) { Icon(Icons.Filled.Menu, contentDescription = "Conversations") }
        },
        actions = {
            IconButton(onClick = onNewChat, enabled = canStartOver) {
                Icon(Icons.Outlined.Edit, contentDescription = "New chat")
            }
        },
    )
}
