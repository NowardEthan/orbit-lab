package com.ethan.orbitlab.ui.chat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

data class AttachMediaLaunchers(
    val requestGalleryPermission: () -> Unit,
    val takePhoto: () -> Unit,
    val recordVideo: () -> Unit,
    val pickFiles: () -> Unit,
    val pickPhotos: () -> Unit,
)

@Composable
fun rememberAttachMediaLaunchers(
    context: Context,
    onPicked: (List<ComposerAttachment>) -> Unit,
    onPermissionResult: (granted: Boolean) -> Unit = {},
): AttachMediaLaunchers {
    val onPickedState = rememberUpdatedState(onPicked)
    val onPermissionState = rememberUpdatedState(onPermissionResult)

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }

    val takePicture =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            val uri = pendingCameraUri
            pendingCameraUri = null
            if (ok && uri != null) {
                onPickedState.value(
                    listOf(
                        ComposerAttachment(
                            id = "cam-${System.currentTimeMillis()}",
                            kind = AttachmentKind.IMAGE,
                            name = "Foto",
                            sizeLabel = "—",
                            mime = "image/jpeg",
                            uri = uri,
                        ),
                    ),
                )
            }
        }

    val captureVideo =
        rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
            val uri = pendingVideoUri
            pendingVideoUri = null
            if (ok && uri != null) {
                onPickedState.value(
                    listOf(
                        ComposerAttachment(
                            id = "vid-${System.currentTimeMillis()}",
                            kind = AttachmentKind.VIDEO,
                            name = "Vídeo",
                            sizeLabel = "—",
                            mime = "video/mp4",
                            uri = uri,
                        ),
                    ),
                )
            }
        }

    val pickVisual =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(maxItems = 12),
        ) { uris ->
            if (uris.isEmpty()) return@rememberLauncherForActivityResult
            onPickedState.value(uris.mapIndexed { i, uri -> uri.toComposerAttachment(context, i) })
        }

    val openDocuments =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris ->
            if (uris.isEmpty()) return@rememberLauncherForActivityResult
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }
            onPickedState.value(uris.mapIndexed { i, uri -> uri.toComposerAttachment(context, i) })
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { result ->
            onPermissionState.value(result.values.any { it })
        }

    val cameraPermission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (!granted) return@rememberLauncherForActivityResult
            val uri = createCaptureUri(context, "jpg") ?: return@rememberLauncherForActivityResult
            pendingCameraUri = uri
            takePicture.launch(uri)
        }

    val videoPermission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { result ->
            if (result.values.any { !it }) return@rememberLauncherForActivityResult
            val uri = createCaptureUri(context, "mp4") ?: return@rememberLauncherForActivityResult
            pendingVideoUri = uri
            captureVideo.launch(uri)
        }

    return remember {
        AttachMediaLaunchers(
            requestGalleryPermission = {
                permissionLauncher.launch(galleryPermissions())
            },
            takePhoto = {
                if (hasPermission(context, Manifest.permission.CAMERA)) {
                    val uri = createCaptureUri(context, "jpg") ?: return@AttachMediaLaunchers
                    pendingCameraUri = uri
                    takePicture.launch(uri)
                } else {
                    cameraPermission.launch(Manifest.permission.CAMERA)
                }
            },
            recordVideo = {
                val needed =
                    buildList {
                        add(Manifest.permission.CAMERA)
                        add(Manifest.permission.RECORD_AUDIO)
                    }
                if (needed.all { hasPermission(context, it) }) {
                    val uri = createCaptureUri(context, "mp4") ?: return@AttachMediaLaunchers
                    pendingVideoUri = uri
                    captureVideo.launch(uri)
                } else {
                    videoPermission.launch(needed.toTypedArray())
                }
            },
            pickFiles = {
                openDocuments.launch(arrayOf("*/*"))
            },
            pickPhotos = {
                pickVisual.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                )
            },
        )
    }
}

private fun createCaptureUri(context: Context, extension: String): Uri? {
    val dir = File(context.cacheDir, "camera").also { it.mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.$extension")
    return runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun Uri.toComposerAttachment(context: Context, index: Int): ComposerAttachment {
    val mime = context.contentResolver.getType(this) ?: guessMime(this)
    val name = queryDisplayName(context) ?: "Anexo ${index + 1}"
    val size = querySize(context)
    val kind =
        when {
            mime.startsWith("image/") -> AttachmentKind.IMAGE
            mime.startsWith("video/") -> AttachmentKind.VIDEO
            else -> AttachmentKind.FILE
        }
    return ComposerAttachment(
        id = "pick-${System.currentTimeMillis()}-$index",
        kind = kind,
        name = name,
        sizeLabel = formatBytes(size ?: 0L),
        sizeBytes = size ?: 0L,
        mime = mime,
        uri = this,
    )
}

private fun Uri.queryDisplayName(context: Context): String? {
    val cursor =
        context.contentResolver.query(
            this,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        ) ?: return null
    return cursor.use {
        if (!it.moveToFirst()) return null
        val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx < 0) null else it.getString(idx)
    }
}

private fun Uri.querySize(context: Context): Long? {
    val cursor =
        context.contentResolver.query(
            this,
            arrayOf(android.provider.OpenableColumns.SIZE),
            null,
            null,
            null,
        ) ?: return null
    return cursor.use {
        if (!it.moveToFirst()) return null
        val idx = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
        if (idx < 0) null else it.getLong(idx).takeIf { s -> s > 0 }
    }
}

private fun guessMime(uri: Uri): String {
    val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString()).orEmpty()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
}
