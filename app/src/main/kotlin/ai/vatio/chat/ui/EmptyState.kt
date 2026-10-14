package ai.vatio.chat.ui

import ai.vatio.VatioConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * A conversation with nothing in it yet: who the agent is, its greeting, and
 * the suggestions configured for it, each one a message to send. Until the
 * config arrives (or if it cannot), a neutral avatar and greeting.
 */
@Composable
fun EmptyState(config: VatioConfig?, onSuggestion: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AgentAvatar(config, size = 64.dp)
            Text(
                config?.greeting?.takeIf { it.isNotBlank() } ?: "How can I help?",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            config?.suggestions?.forEach { suggestion ->
                OutlinedCard(
                    onClick = { onSuggestion(suggestion) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

/**
 * The agent's avatar image; without one, its initial on its accent color;
 * without that either, a generic mark.
 */
@Composable
fun AgentAvatar(config: VatioConfig?, size: Dp, modifier: Modifier = Modifier) {
    val circle = modifier
        .size(size)
        .clip(CircleShape)
    val avatarUrl = config?.avatarUrl
    val initial = config?.agentName?.trim()?.firstOrNull()?.uppercase()

    when {
        avatarUrl != null -> AsyncImage(
            model = avatarUrl,
            contentDescription = config?.agentName,
            contentScale = ContentScale.Crop,
            modifier = circle.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        // The theme already uses the accent as its primary color.
        config?.accentColor != null && initial != null -> Box(
            circle.background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(initial, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
        }
        else -> Box(circle.background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size / 2),
            )
        }
    }
}
