package ai.vatio.chat

import ai.vatio.VatioUpload

/**
 * What Vatio accepts in one message (vatio.ai/docs/sdk/limits). The SDK and
 * the server enforce them anyway; checking here lets the composer say so
 * before anything is sent.
 */
object Limits {
    const val MESSAGE_CHARACTERS = 4_000
    const val FILES_PER_MESSAGE = 4
    const val FILE_BYTES = VatioUpload.MAX_BYTES
    const val FEEDBACK_COMMENT_CHARACTERS = 2_000
}
