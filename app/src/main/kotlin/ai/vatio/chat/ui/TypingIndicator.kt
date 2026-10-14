package ai.vatio.chat.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Three dots that pulse in turn while the agent is writing (`chat.isTyping`). */
@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    val color = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier
            .padding(vertical = 6.dp)
            .semantics { contentDescription = "The agent is typing" },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 450, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(offsetMillis = index * 150),
                ),
                label = "dot $index",
            )
            Box(
                Modifier
                    .size(8.dp)
                    // Read in the draw phase, so the animation does not recompose.
                    .graphicsLayer { this.alpha = alpha }
                    .background(color, CircleShape),
            )
        }
    }
}
