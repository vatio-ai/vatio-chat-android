package ai.vatio.chat.ui

import ai.vatio.VatioAttachment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * The files on a message: images as thumbnails, anything else as a chip
 * with its name. Each opens its signed URL when tapped.
 *
 * While the message is pending its files have no URL yet, so an image shows
 * a placeholder until the server has stored it.
 */
@Composable
fun MessageAttachments(
    attachments: List<VatioAttachment>,
    alignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return
    val (images, files) = attachments.partition { it.kind == VatioAttachment.Kind.IMAGE }

    Column(modifier, horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (images.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                images.forEach { ImageThumbnail(it) }
            }
        }
        files.forEach { FileChip(it) }
    }
}

@Composable
private fun ImageThumbnail(attachment: VatioAttachment) {
    val uriHandler = LocalUriHandler.current
    val url = attachment.url
    val shape = RoundedCornerShape(14.dp)
    val frame = Modifier
        .size(160.dp)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)

    if (url == null) {
        Box(frame, contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.Image,
                contentDescription = attachment.filename,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = attachment.filename,
            contentScale = ContentScale.Crop,
            modifier = frame.clickable { uriHandler.openSafely(url) },
        )
    }
}

@Composable
private fun FileChip(attachment: VatioAttachment) {
    val uriHandler = LocalUriHandler.current
    val url = attachment.url
    val isVoiceNote = attachment.kind == VatioAttachment.Kind.AUDIO

    Row(
        Modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = url != null) { url?.let(uriHandler::openSafely) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            if (isVoiceNote) Icons.Outlined.Mic else Icons.Outlined.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (isVoiceNote) "Voice note" else attachment.filename,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
