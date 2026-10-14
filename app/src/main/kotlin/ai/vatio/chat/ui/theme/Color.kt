package ai.vatio.chat.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Neutral grays everywhere, so the agent's accent (primary) is the only
// color on screen. Vatio's orange stands in until the config says otherwise.
private val VatioOrange = Color(0xFFDD5D00)

internal val LightColors = lightColorScheme(
    primary = VatioOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFECECEC),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFF5E5E5E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = Color(0xFF5E5E5E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8E8E8),
    onTertiaryContainer = Color(0xFF1A1A1A),
    background = Color.White,
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF5E5E5E),
    surfaceTint = Color(0xFF5E5E5E),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFDCDCDC),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9F9F9),
    surfaceContainer = Color(0xFFF4F4F4),
    surfaceContainerHigh = Color(0xFFEFEFEF),
    surfaceContainerHighest = Color(0xFFE8E8E8),
    outline = Color(0xFF8E8E8E),
    outlineVariant = Color(0xFFE0E0E0),
)

internal val DarkColors = darkColorScheme(
    primary = VatioOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2F2F2F),
    onPrimaryContainer = Color(0xFFECECEC),
    secondary = Color(0xFFB4B4B4),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color(0xFFECECEC),
    tertiary = Color(0xFFB4B4B4),
    onTertiary = Color(0xFF1A1A1A),
    tertiaryContainer = Color(0xFF333333),
    onTertiaryContainer = Color(0xFFECECEC),
    background = Color(0xFF171717),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF171717),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB4B4B4),
    surfaceTint = Color(0xFFB4B4B4),
    surfaceBright = Color(0xFF3A3A3A),
    surfaceDim = Color(0xFF171717),
    surfaceContainerLowest = Color(0xFF101010),
    surfaceContainerLow = Color(0xFF1C1C1C),
    surfaceContainer = Color(0xFF212121),
    surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Color(0xFF333333),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFF3A3A3A),
)
