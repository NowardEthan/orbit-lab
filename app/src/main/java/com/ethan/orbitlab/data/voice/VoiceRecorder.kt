package com.ethan.orbitlab.data.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Clip de voz gravado no aparelho — arquivo local até a transcrição. */
data class VoiceClip(
    val file: File,
    val durationMs: Long,
    val mimeType: String = "audio/mp4",
)

/**
 * Gravação de mensagem de voz via [MediaRecorder] (AAC em .m4a).
 *
 * O UI do composer já tinha o hold-to-talk; isto é o que faltava pra Luna
 * ouvir de verdade — sem isto o Lab mandava só o texto «Áudio (mm:ss)».
 */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAtMs: Long = 0L

    val isRecording: Boolean get() = recorder != null

    fun start(): Boolean {
        cancel()
        val out = File(context.cacheDir, "voz_${System.currentTimeMillis()}.m4a")
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return try {
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(128_000)
            mr.setAudioSamplingRate(44_100)
            mr.setOutputFile(out.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            file = out
            startedAtMs = System.currentTimeMillis()
            true
        } catch (_: Exception) {
            runCatching { mr.release() }
            out.delete()
            recorder = null
            file = null
            startedAtMs = 0L
            false
        }
    }

    /** Para e apaga o arquivo — cancelamento / gesto «deslizar pra cancelar». */
    fun cancel() {
        val mr = recorder
        val out = file
        recorder = null
        file = null
        startedAtMs = 0L
        if (mr != null) {
            runCatching { mr.stop() }
            runCatching { mr.release() }
        }
        out?.delete()
    }

    /**
     * Para a gravação e devolve o clip. `null` se curta demais, vazia ou falhou.
     * O chamador é dono do [VoiceClip.file] (apagar depois de ler).
     */
    fun finish(): VoiceClip? {
        val mr = recorder ?: return null
        val out = file
        val duration = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)
        recorder = null
        file = null
        startedAtMs = 0L
        try {
            mr.stop()
        } catch (_: Exception) {
            runCatching { mr.release() }
            out?.delete()
            return null
        }
        runCatching { mr.release() }
        if (out == null || !out.exists() || out.length() < 256L) {
            out?.delete()
            return null
        }
        if (duration < MIN_DURATION_MS) {
            out.delete()
            return null
        }
        return VoiceClip(file = out, durationMs = duration, mimeType = "audio/mp4")
    }

    companion object {
        const val MIN_DURATION_MS = 500L
    }
}
