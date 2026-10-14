package ai.vatio.chat.ui

import ai.vatio.VatioConversation
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant

/**
 * The side panel, as in ChatGPT: start a new chat, switch to one of this
 * visitor's past conversations (`vatio.conversations()`), or open Settings.
 *
 * [conversations] is null until the first load; [error] is the last load's
 * failure, if any.
 */
@Composable
fun ConversationsDrawer(
    drawerState: DrawerState,
    conversations: List<VatioConversation>?,
    error: String?,
    currentConversationId: Long?,
    onNewChat: () -> Unit,
    onOpen: (VatioConversation) -> Unit,
    onOpenSettings: () -> Unit,
) {
    // Given the drawer's state, the sheet also closes on the back gesture.
    ModalDrawerSheet(drawerState) {
        NavigationDrawerItem(
            label = { Text("New chat") },
            icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
            selected = false,
            onClick = onNewChat,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).padding(top = 12.dp),
        )
        Text(
            "Chats",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                conversations == null && error == null -> CircularProgressIndicator(
                    Modifier
                        .padding(28.dp)
                        .size(24.dp),
                    strokeWidth = 2.dp,
                )
                conversations.isNullOrEmpty() -> Text(
                    error ?: "No conversations yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp),
                )
                else -> LazyColumn {
                    items(conversations, key = { it.id }) { conversation ->
                        ConversationItem(
                            conversation,
                            selected = conversation.id == currentConversationId,
                            onClick = { onOpen(conversation) },
                        )
                    }
                }
            }
        }

        HorizontalDivider(Modifier.padding(horizontal = 28.dp))
        NavigationDrawerItem(
            label = { Text("Settings") },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            selected = false,
            onClick = onOpenSettings,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun ConversationItem(conversation: VatioConversation, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = {
            Column {
                Text(
                    conversation.title.ifBlank { conversation.preview }.ifBlank { "New conversation" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                (conversation.updatedAt ?: conversation.startedAt)?.let { time ->
                    Text(
                        relativeTime(time),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}

// "Just now", "5 min. ago", "Yesterday", "Oct 3": in the device's language.
private fun relativeTime(time: Instant): String {
    val now = System.currentTimeMillis()
    val then = time.toEpochMilli()
    if (now - then < DateUtils.MINUTE_IN_MILLIS) return "Just now"
    return DateUtils.getRelativeTimeSpanString(then, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
}
