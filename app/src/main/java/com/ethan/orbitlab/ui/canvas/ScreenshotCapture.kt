package com.ethan.orbitlab.ui.canvas

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Resultado de um screenshot.
 */
sealed class ScreenshotResult {
    data class Success(
        val uri: String,
        val path: String,
        val width: Int,
        val height: Int,
    ) : ScreenshotResult()

    data class Error(
        val message: String,
        val exception: Throwable? = null,
    ) : ScreenshotResult()
}

/**
 * Utilitário para capturar screenshots de views.
 */
object ScreenshotCapture {

    /**
     * Captura screenshot de uma View como bitmap.
     */
    fun capture(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    /**
     * Captura screenshot de um ComposeView.
     */
    suspend fun captureCompose(composeView: ComposeView): Bitmap {
        return suspendCancellableCoroutine { continuation ->
            try {
                val bitmap = Bitmap.createBitmap(
                    composeView.width.coerceAtLeast(1),
                    composeView.height.coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888,
                )
                val canvas = Canvas(bitmap)

                // Dispatch no main thread para renderizar
                composeView.context.mainExecutor.execute {
                    composeView.draw(canvas)

                    continuation.resume(bitmap)
                }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    /**
     * Salva bitmap na galeria.
     */
    suspend fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        filename: String = "mockup_${System.currentTimeMillis()}",
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    ): ScreenshotResult {
        return withContext(Dispatchers.IO) {
            try {
                val extension = when (format) {
                    Bitmap.CompressFormat.PNG -> "png"
                    Bitmap.CompressFormat.JPEG -> "jpg"
                    else -> "png"
                }

                val mimeType = when (format) {
                    Bitmap.CompressFormat.PNG -> "image/png"
                    Bitmap.CompressFormat.JPEG -> "image/jpeg"
                    else -> "image/png"
                }

                val fullFilename = "$filename.$extension"

                val uri: String
                val path: String

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+: usar MediaStore
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fullFilename)
                        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Orbit")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }

                    val resolver = context.contentResolver
                    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        ?: throw Exception("Failed to create media store entry")

                    resolver.openOutputStream(imageUri)?.use { outputStream ->
                        bitmap.compress(format, 100, outputStream)
                    } ?: throw Exception("Failed to open output stream")

                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)

                    uri = imageUri.toString()
                    path = "${Environment.DIRECTORY_PICTURES}/Orbit/$fullFilename"
                } else {
                    // Android 9 e anteriores
                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val orbitDir = File(picturesDir, "Orbit")
                    if (!orbitDir.exists()) orbitDir.mkdirs()

                    val file = File(orbitDir, fullFilename)
                    FileOutputStream(file).use { outputStream ->
                        bitmap.compress(format, 100, outputStream)
                    }

                    uri = file.absolutePath
                    path = file.absolutePath
                }

                ScreenshotResult.Success(
                    uri = uri,
                    path = path,
                    width = bitmap.width,
                    height = bitmap.height,
                )
            } catch (e: Exception) {
                ScreenshotResult.Error(
                    message = e.message ?: "Erro desconhecido",
                    exception = e,
                )
            }
        }
    }

    /**
     * Salva bitmap em arquivo temporário.
     */
    suspend fun saveToTemp(
        context: Context,
        bitmap: Bitmap,
        filename: String = "mockup_${System.currentTimeMillis()}",
    ): ScreenshotResult {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, "$filename.png")
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                ScreenshotResult.Success(
                    uri = file.absolutePath,
                    path = file.absolutePath,
                    width = bitmap.width,
                    height = bitmap.height,
                )
            } catch (e: Exception) {
                ScreenshotResult.Error(
                    message = e.message ?: "Erro desconhecido",
                    exception = e,
                )
            }
        }
    }

    /**
     * Salva bitmap em OutputStream customizado.
     */
    suspend fun saveToStream(
        bitmap: Bitmap,
        outputStream: OutputStream,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                bitmap.compress(format, 100, outputStream)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Redimensiona bitmap mantendo aspect ratio.
     */
    fun resize(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Adiciona watermark ao bitmap.
     */
    fun addWatermark(
        bitmap: Bitmap,
        text: String = "Criado com Orbit",
        position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    ): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(128, 255, 255, 255)
            textSize = bitmap.width * 0.03f
            isAntiAlias = true
        }

        val textBounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)

        val padding = bitmap.width * 0.02f
        val x = when (position) {
            WatermarkPosition.TOP_LEFT -> padding
            WatermarkPosition.TOP_RIGHT -> bitmap.width - textBounds.width() - padding
            WatermarkPosition.BOTTOM_LEFT -> padding
            WatermarkPosition.BOTTOM_RIGHT -> bitmap.width - textBounds.width() - padding
        }
        val y = when (position) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> textBounds.height() + padding
            WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> bitmap.height - padding
        }

        canvas.drawText(text, x, y, paint)
        return mutableBitmap
    }
}

enum class WatermarkPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

/**
 * Composable que captura screenshot de um Compose content.
 */
@Composable
fun ScreenshotCaptureView(
    content: @Composable () -> Unit,
    onViewReady: (View) -> Unit,
) {
    val composeView = androidx.compose.ui.platform.ComposeView(
        context = androidx.compose.ui.platform.LocalContext.current,
    ).apply {
        setContent { content() }
    }

    androidx.compose.runtime.LaunchedEffect(composeView) {
        onViewReady(composeView)
    }
}

/**
 * Diálogo de screenshot capturado com opções de compartilhar/salvar.
 */
@Composable
fun ScreenshotSuccessDialog(
    result: ScreenshotResult.Success,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Screenshot salvo!",
                style = com.ethan.orbitlab.ui.theme.OrbitType.Headline.SM,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Tamanho: ${result.width} x ${result.height}",
                    style = com.ethan.orbitlab.ui.theme.OrbitType.Body.SM,
                    color = com.ethan.orbitlab.ui.theme.OrbitTokens.textMid,
                )
                Spacer(
                    modifier = Modifier.height(8.dp),
                )
                Text(
                    text = result.path,
                    style = com.ethan.orbitlab.ui.theme.OrbitType.Body.XXS,
                    color = com.ethan.orbitlab.ui.theme.OrbitTokens.textLow,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onShare) {
                Text("Compartilhar")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("Fechar")
                }
                TextButton(onClick = onSave) {
                    Text("Salvar")
                }
            }
        },
        containerColor = com.ethan.orbitlab.ui.theme.OrbitTokens.graphiteSurf,
    )
}

private fun Modifier.fillMaxWidth(): Modifier = this.then(
    Modifier.fillMaxWidth(1f)
)
