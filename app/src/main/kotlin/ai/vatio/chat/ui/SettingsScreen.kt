package ai.vatio.chat.ui

import ai.vatio.Vatio
import ai.vatio.chat.AppConfig
import ai.vatio.chat.ChatViewModel
import ai.vatio.chat.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Which agent to talk to, and as whom. Saving replaces the client and the
 * chat, since a VatioChat is bound to its workspace, token and visitor token
 * from the moment it is created.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ChatViewModel, onClose: () -> Unit) {
    val saved = viewModel.settings
    var workspace by rememberSaveable { mutableStateOf(saved.workspace) }
    var token by rememberSaveable { mutableStateOf(saved.token) }
    var baseUrl by rememberSaveable { mutableStateOf(saved.baseUrl) }
    var visitorToken by rememberSaveable { mutableStateOf(saved.visitorToken.orEmpty()) }
    var showsVisitorToken by rememberSaveable { mutableStateOf(false) }

    // Only publishable tokens can ship in an app; the SDK refuses anything else.
    val isPublishable = token.trim().startsWith("vatpub_")
    val canSave = workspace.isNotBlank() && isPublishable

    BackHandler(onBack = onClose)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Agent")
            OutlinedTextField(
                value = workspace,
                onValueChange = { workspace = it },
                label = { Text("Workspace") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Publishable token") },
                singleLine = true,
                isError = token.isNotBlank() && !isPublishable,
                supportingText = {
                    Text(
                        if (token.isNotBlank() && !isPublishable) {
                            "A publishable token starts with vatpub_. Never put a vat_ token in an app."
                        } else {
                            "It must allow ${viewModel.origin}."
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("Signed-in user")
            OutlinedTextField(
                value = visitorToken,
                onValueChange = { visitorToken = it },
                label = { Text("Visitor token (optional)") },
                singleLine = true,
                visualTransformation = if (showsVisitorToken) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showsVisitorToken = !showsVisitorToken }) {
                        Icon(
                            if (showsVisitorToken) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showsVisitorToken) "Hide token" else "Show token",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Footnote(
                "A JWT your backend signs for a signed-in user, so the agent knows who they are and can " +
                    "use private tools. In a real app it comes from your backend after your own sign-in; " +
                    "paste one here to try private tools. Leave it empty to chat anonymously.",
            )

            Button(
                onClick = {
                    viewModel.saveSettings(
                        Settings(
                            workspace = workspace.trim(),
                            token = token.trim(),
                            baseUrl = baseUrl.trim().ifEmpty { AppConfig.BASE_URL },
                            visitorToken = visitorToken.trim().ifEmpty { null },
                        ),
                    )
                    onClose()
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
            OutlinedButton(
                onClick = {
                    viewModel.useVatioAgent()
                    onClose()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Use Vatio's agent")
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            About(origin = viewModel.origin)
        }
    }
}

@Composable
private fun About(origin: String) {
    SectionTitle("About")
    Text(
        "Vatio Chat is an open-source chat app for a Vatio agent, and a reference for putting one in " +
            "your own Android app. Everything it does goes through the public Vatio Android SDK.",
        style = MaterialTheme.typography.bodyMedium,
    )
    LinkRow("Source code", AppConfig.REPOSITORY_URL)
    LinkRow("Android SDK docs", AppConfig.DOCS_URL)

    Text("Origin", style = MaterialTheme.typography.titleSmall)
    SelectionContainer {
        Text(origin, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
    }
    Footnote("Vatio accepts requests from this app only with a token whose allowed origins include this.")
    Footnote("Vatio Android SDK ${Vatio.VERSION}")
}

@Composable
private fun LinkRow(title: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openSafely(url) }
            .padding(vertical = 8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                url.removePrefix("https://"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(16.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Footnote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
