package ai.vatio.chat.ui

import ai.vatio.VatioMessage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * One message. The visitor's sit on the right in a bubble, as typed; the
 * agent's run full width with no bubble, rendered as Markdown.
 */
@Composable
fun MessageRow(message: VatioMessage, modifier: Modifier = Modifier) {
    if (message.isFromVisitor) {
        VisitorMessage(message, modifier)
    } else {
        AgentMessage(message, modifier)
    }
}

@Composable
private fun VisitorMessage(message: VatioMessage, modifier: Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            // Not confirmed by the server yet.
            .alpha(if (message.isPending) 0.5f else 1f),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MessageAttachments(message.attachments, alignment = Alignment.End)
        if (message.hasText) {
            Text(
                message.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentWidth(Alignment.End)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun AgentMessage(message: VatioMessage, modifier: Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MessageAttachments(message.attachments, alignment = Alignment.Start)
        if (message.hasText) {
            SelectionContainer { Markdown(message.content) }
        }
    }
}

// With only a file, content is a stand-in for it ("Image", "Audio") and the
// file is drawn instead. A voice note's transcript replaces the stand-in a
// few seconds after it is sent, and shows up here then.
private val VatioMessage.hasText: Boolean
    get() = !isMediaLabel && content.isNotBlank()
