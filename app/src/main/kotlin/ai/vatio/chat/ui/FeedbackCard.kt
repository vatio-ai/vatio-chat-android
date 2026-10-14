package ai.vatio.chat.ui

import ai.vatio.VatioFeedback
import ai.vatio.VatioFeedback.Rating
import ai.vatio.chat.Limits
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Vatio's feedback moment, as this app asks it: Good, So-so or Bad, a comment
 * for the last two, or Not now. The SDK only says when to ask
 * (`chat.feedback`) and takes the answer (`rate`, `dismissFeedback`); the
 * card is the app's to design.
 */
@Composable
fun FeedbackCard(
    feedback: VatioFeedback,
    isSending: Boolean,
    onRate: (Rating, String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var picked by rememberSaveable { mutableStateOf<Rating?>(null) }
    var comment by rememberSaveable { mutableStateOf("") }
    val wantsComment = picked == Rating.NEUTRAL || picked == Rating.BAD

    OutlinedCard(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                // The agent's own words for this conversation, when it has them.
                feedback.question ?: "How did ${feedback.agentName} do?",
                style = MaterialTheme.typography.titleSmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Rating.entries.forEach { rating ->
                    FilterChip(
                        selected = picked == rating,
                        onClick = {
                            picked = rating
                            // Good needs no explanation, so it goes straight out.
                            if (rating == Rating.GOOD) onRate(Rating.GOOD, null)
                        },
                        label = { Text(rating.label, Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                        enabled = !isSending,
                        modifier = Modifier.weight(1f),
                        // In the agent's accent, like the send button: it is the agent asking.
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            labelColor = MaterialTheme.colorScheme.primary,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        border = null,
                    )
                }
            }
            if (wantsComment) {
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it.take(Limits.FEEDBACK_COMMENT_CHARACTERS) },
                    placeholder = { Text("What could have gone better? (optional)") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss, enabled = !isSending) { Text("Not now") }
                picked?.takeIf { wantsComment }?.let { rating ->
                    Button(onClick = { onRate(rating, comment.trim().ifEmpty { null }) }, enabled = !isSending) {
                        Text("Send")
                    }
                }
            }
        }
    }
}

private val Rating.label: String
    get() = when (this) {
        Rating.GOOD -> "Good"
        Rating.NEUTRAL -> "So-so"
        Rating.BAD -> "Bad"
    }
