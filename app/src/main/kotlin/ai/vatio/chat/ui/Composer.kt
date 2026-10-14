package ai.vatio.chat.ui

import ai.vatio.VatioUpload
import ai.vatio.chat.Draft
import ai.vatio.chat.Limits
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.io.File

/**
 * The input at the bottom: the files attached so far, a + button for the
 * photo picker, a growing text field, and one button on the right that
 * sends, or records a voice note when there is nothing to send.
 *
 * It holds no text of its own: [draft] lives in the view model, so it
 * survives rotation and a failed send can put it back.
 */
@Composable
fun Composer(
    draft: Draft,
    onTextChange: (String) -> Unit,
    onAttach: (List<Uri>) -> Unit,
    onRemoveAttachment: (VatioUpload) -> Unit,
    onSend: () -> Unit,
    onVoiceNote: (File) -> Unit,
    onProblem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context.applicationContext) }
    // Leaving the screen mid-recording throws the recording away.
    DisposableEffect(recorder) { onDispose { recorder.discard() } }

    // The system photo picker needs no storage permission: the app only ever
    // sees the photos the person picks.
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(Limits.FILES_PER_MESSAGE),
    ) { uris -> if (uris.isNotEmpty()) onAttach(uris) }

    // Every tap on the mic goes through here. The system asks only the first
    // time; once the permission is granted, the answer comes straight back.
    val recordWithPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            onProblem("Voice notes need the microphone. Allow it in the app's settings.")
        } else if (!recorder.start()) {
            onProblem("Couldn't use the microphone.")
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (draft.attachments.isNotEmpty()) {
            AttachmentTray(draft.attachments, onRemoveAttachment)
        }
        // Near the limit, say how much room is left.
        if (draft.text.length > Limits.MESSAGE_CHARACTERS - 200) {
            Text(
                "${draft.text.length} / ${Limits.MESSAGE_CHARACTERS}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 56.dp),
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            ButtonSlot {
                IconButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !recorder.isRecording && draft.attachments.size < Limits.FILES_PER_MESSAGE,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add photos")
                }
            }

            if (recorder.isRecording) {
                RecordingBar(recorder.startedAt, onDiscard = recorder::discard, modifier = Modifier.weight(1f))
            } else {
                TextField(
                    value = draft.text,
                    onValueChange = onTextChange,
                    placeholder = { Text("Message") },
                    maxLines = 6,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.weight(1f),
                )
            }

            ButtonSlot {
                when {
                    recorder.isRecording -> FilledIconButton(onClick = { recorder.stop()?.let(onVoiceNote) }) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Stop and send the voice note")
                    }
                    !draft.isEmpty -> FilledIconButton(onClick = onSend) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Send")
                    }
                    else -> IconButton(onClick = { recordWithPermission.launch(Manifest.permission.RECORD_AUDIO) }) {
                        Icon(Icons.Outlined.Mic, contentDescription = "Record a voice note")
                    }
                }
            }
        }
    }
}

// As tall as a one-line text field, so the buttons line up with it and stay
// at the bottom as it grows.
@Composable
private fun ButtonSlot(content: @Composable () -> Unit) {
    Box(Modifier.size(width = 48.dp, height = 56.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun RecordingBar(startedAt: Long, onDiscard: () -> Unit, modifier: Modifier = Modifier) {
    var seconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(startedAt) {
        while (true) {
            seconds = (SystemClock.elapsedRealtime() - startedAt) / 1_000
            delay(250)
        }
    }
    Row(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .background(MaterialTheme.colorScheme.error, CircleShape),
        )
        Text(
            "Recording ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
        )
        IconButton(onClick = onDiscard) {
            Icon(Icons.Filled.Close, contentDescription = "Discard the recording")
        }
    }
}

/** The files waiting to go with the next message, each with a remove button. */
@Composable
private fun AttachmentTray(attachments: List<VatioUpload>, onRemove: (VatioUpload) -> Unit) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(start = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        attachments.forEach { upload ->
            Box(Modifier.size(64.dp)) {
                val frame = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                if (upload.contentType.startsWith("image/")) {
                    // From the bytes already read: the picker's grant to the URI
                    // does not outlive the app's process.
                    AsyncImage(
                        model = upload.bytes,
                        contentDescription = upload.filename,
                        contentScale = ContentScale.Crop,
                        modifier = frame,
                    )
                } else {
                    Box(frame, contentAlignment = Alignment.Center) {
                        Icon(
                            if (upload.contentType.startsWith("audio/")) Icons.Outlined.Mic else Icons.Outlined.Description,
                            contentDescription = upload.filename,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.inverseSurface)
                        .clickable { onRemove(upload) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove ${upload.filename}",
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}
