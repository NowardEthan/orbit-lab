package com.ethan.orbitlab.data.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

private const val APK_MIME = "application/vnd.android.package-archive"

private val downloadClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
}

object ApkInstaller {
    suspend fun downloadAndInstall(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val dest = File(context.cacheDir, "orbit-lab-update.apk")
        val request = Request.Builder().url(url).build()
        downloadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body ?: error("Resposta vazia")
            val total = body.contentLength().coerceAtLeast(0L)
            body.byteStream().use { input ->
                dest.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var written = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) onProgress(written.toFloat() / total.toFloat())
                    }
                }
            }
        }
        withContext(Dispatchers.Main) {
            launchInstaller(context, dest)
        }
    }

    fun openInBrowser(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun launchInstaller(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
