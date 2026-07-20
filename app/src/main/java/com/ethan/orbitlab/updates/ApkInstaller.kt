package com.ethan.orbitlab.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Baixa o APK para a cache e dispara o instalador nativo (sem abrir o browser).
 * `onProgress` recebe 0..1. Em falha, o chamador pode cair em [openUpdateInBrowser].
 */
object ApkInstaller {

    private const val APK_MIME = "application/vnd.android.package-archive"

    fun openUpdateInBrowser(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun downloadAndInstallApk(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit = {},
    ) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val dest = File(dir, "orbit-update.apk")
        if (dest.exists()) dest.delete()

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Orbit-Lab")
            instanceFollowRedirects = true
        }
        try {
            if (conn.responseCode !in 200..299) {
                throw IllegalStateException("Falha ao baixar atualização (HTTP ${conn.responseCode}).")
            }
            val total = conn.contentLengthLong.coerceAtLeast(0L)
            conn.inputStream.use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var written = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0L) {
                            onProgress((written.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                    output.flush()
                }
            }
            onProgress(1f)
        } finally {
            conn.disconnect()
        }

        if (!dest.exists() || dest.length() == 0L) {
            throw IllegalStateException("Falha ao baixar a atualização.")
        }

        // Android 8+: precisa de permissão para instalar pacotes desconhecidos.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settings = Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(settings)
                throw IllegalStateException("Permita instalar apps desconhecidas e tente de novo.")
            }
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            dest,
        )
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(install)
    }
}
