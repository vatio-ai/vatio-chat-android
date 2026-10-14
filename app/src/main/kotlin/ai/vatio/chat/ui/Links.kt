package ai.vatio.chat.ui

import androidx.compose.ui.platform.UriHandler

/**
 * Opens [url] in whichever app handles it (the browser, for a web link).
 * Does nothing when no app on the device can, rather than crashing.
 */
fun UriHandler.openSafely(url: String) {
    try {
        openUri(url)
    } catch (_: IllegalArgumentException) {
        // AndroidUriHandler reports "no app for this" this way.
    }
}
