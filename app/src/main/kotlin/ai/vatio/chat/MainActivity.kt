package ai.vatio.chat

import ai.vatio.chat.ui.ChatScreen
import ai.vatio.chat.ui.SettingsScreen
import ai.vatio.chat.ui.theme.VatioChatTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { VatioChatApp() }
    }
}

/** Two screens, the chat and Settings, both in the agent's accent color. */
@Composable
fun VatioChatApp(viewModel: ChatViewModel = viewModel()) {
    var showingSettings by rememberSaveable { mutableStateOf(false) }

    VatioChatTheme(accentColor = viewModel.config?.accentColor) {
        if (showingSettings) {
            SettingsScreen(viewModel, onClose = { showingSettings = false })
        } else {
            ChatScreen(viewModel, onOpenSettings = { showingSettings = true })
        }
    }
}
