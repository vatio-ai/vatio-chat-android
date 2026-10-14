package ai.vatio.chat.ui

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.IOException

/**
 * Records a voice note: AAC in an .m4a file, one of the audio formats Vatio
 * transcribes. Each recording gets a file of its own in the cache, so one can
 * be read and sent while the next is being recorded.
 */
class VoiceRecorder(private val context: Context) {
    var isRecording by mutableStateOf(false)
        private set

    /** When the current recording started, on the [SystemClock.elapsedRealtime] clock. */
    var startedAt = 0L
        private set

    private var recorder: MediaRecorder? = null
    private var file: File? = null

    /** Starts recording. False when the microphone could not be opened (another app has it, say). */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(): Boolean {
        if (isRecording) return true
        val file = File(context.cacheDir, "voice-note-${System.currentTimeMillis()}.m4a")
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioChannels(1)
            recorder.setAudioSamplingRate(44_100)
            // Plenty for speech, and about 16 minutes fit in Vatio's 8 MB.
            recorder.setAudioEncodingBitRate(64_000)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
        } catch (_: IOException) {
            return fail(recorder, file)
        } catch (_: RuntimeException) {
            return fail(recorder, file)
        }
        this.recorder = recorder
        this.file = file
        startedAt = SystemClock.elapsedRealtime()
        isRecording = true
        return true
    }

    /** Stops and returns the recording, or null when nothing was captured. */
    fun stop(): File? {
        val recorder = recorder ?: return null
        val file = file
        this.recorder = null
        this.file = null
        isRecording = false
        val captured = try {
            recorder.stop()
            true
        } catch (_: RuntimeException) {
            // stop() throws when no audio was captured: a tap-tap too quick to record.
            false
        } finally {
            recorder.release()
        }
        if (!captured) file?.delete()
        return file.takeIf { captured }
    }

    /** Stops and throws the recording away. */
    fun discard() {
        stop()?.delete()
    }

    private fun fail(recorder: MediaRecorder, file: File): Boolean {
        recorder.release()
        file.delete()
        return false
    }
}
