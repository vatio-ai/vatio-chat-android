package ai.vatio.chat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Material 3 in light or dark, following the system, with the agent's
 * [accentColor] (`#RRGGBB` from `vatio.config()`) as the primary color: the
 * send button, links, selected controls.
 */
@Composable
fun VatioChatTheme(
    accentColor: String?,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) DarkColors else LightColors
    val colors = remember(base, accentColor) {
        val accent = accentColor?.let(::parseHexColor) ?: return@remember base
        base.copy(primary = accent, onPrimary = contentColorFor(accent))
    }
    MaterialTheme(colorScheme = colors, content = content)
}

// `#RRGGBB` as a color; null for anything else, so a typo falls back to the default.
private fun parseHexColor(hex: String): Color? {
    val digits = hex.trim().removePrefix("#")
    if (digits.length != 6) return null
    val rgb = digits.toLongOrNull(16) ?: return null
    return Color(0xFF000000 or rgb)
}

// Black or white, whichever reads on the accent.
private fun contentColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color.Black else Color.White
