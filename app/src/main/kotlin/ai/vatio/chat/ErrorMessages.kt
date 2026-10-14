package ai.vatio.chat

import ai.vatio.VatioException

/**
 * What to tell the person using the app when a call fails. The SDK's
 * `message` is written for developers, so the codes a person can act on get
 * words of their own. [origin] is what the token has to allow.
 */
fun VatioException.userMessage(origin: String): String = when (code) {
    "origin_not_allowed" -> "This token doesn't allow this app. Add $origin to its allowed origins."
    "wrong_token_kind" -> "That isn't a publishable token. Use a vatpub_ token in Settings."
    "no_agent_deployed" -> "This workspace has no agent published for this token yet."
    "rate_limited" -> "Too many messages for now. Wait a minute and try again."
    "network_error" -> "Can't reach Vatio. Check your connection and try again."
    "subscription_rejected" -> "This conversation expired. Your next message starts a new one."
    "unsupported_file_type" -> "The agent can't read that kind of file."
    else -> message
}
