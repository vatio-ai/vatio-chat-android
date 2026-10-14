package ai.vatio.chat.ui

import ai.vatio.VatioFeedback
import ai.vatio.VatioMessage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The conversation, newest at the bottom, then the feedback card and the
 * typing dots when there are any.
 *
 * It is laid out bottom-up (`reverseLayout`), so the latest message stays in
 * view when the keyboard opens; the items go in newest first. A conversation
 * shorter than the screen still starts at the top, under the bar.
 */
@Composable
fun MessageList(
    messages: List<VatioMessage>,
    isTyping: Boolean,
    feedback: VatioFeedback?,
    isAnsweringFeedback: Boolean,
    onRate: (VatioFeedback.Rating, String?) -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val openFeedback = feedback?.takeIf { it.isOpen }

    // Follow the conversation: a message sent or received, the agent starting
    // to type, the feedback card turning up.
    LaunchedEffect(messages.lastOrNull()?.id, isTyping, openFeedback != null) {
        listState.animateScrollToItem(0)
    }

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
    ) {
        if (isTyping) {
            item(key = "typing") { TypingIndicator() }
        }
        if (openFeedback != null) {
            item(key = "feedback") {
                FeedbackCard(
                    feedback = openFeedback,
                    isSending = isAnsweringFeedback,
                    onRate = onRate,
                    onDismiss = onDismissFeedback,
                )
            }
        }
        items(messages.asReversed(), key = { it.id }) { message ->
            MessageRow(message)
        }
    }
}
