package ai.vatio.chat

import ai.vatio.VatioUpload

/** What the composer holds before it is sent: text and up to four files. */
data class Draft(
    val text: String = "",
    val attachments: List<VatioUpload> = emptyList(),
) {
    val isEmpty: Boolean get() = text.isBlank() && attachments.isEmpty()

    /**
     * This draft back in the composer after its send failed, ahead of
     * whatever was typed in the meantime, so neither is lost.
     */
    fun restoredInto(current: Draft): Draft = Draft(
        text = listOf(text, current.text).filter { it.isNotBlank() }.joinToString("\n").take(Limits.MESSAGE_CHARACTERS),
        attachments = (attachments + current.attachments).take(Limits.FILES_PER_MESSAGE),
    )
}
