package com.ethan.orbitlab.data.firebase

import android.content.Context
import android.net.Uri
import com.ethan.orbitlab.ui.chat.AttachmentKind
import com.ethan.orbitlab.ui.chat.ComposerAttachment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Upload de anexos de chat para o mesmo bucket/paths do orbit-mobile.
 *
 * Path:
 * `users/{uid}/conversations/{conversationId}/messages/{messageId}/attachments/{id}/{name}`
 */
object ChatMediaUpload {
    private val storage: FirebaseStorage get() = FirebaseStorage.getInstance()

    fun isRemoteUri(uri: Uri?): Boolean {
        val s = uri?.toString().orEmpty()
        return s.startsWith("https://", ignoreCase = true) ||
            s.startsWith("http://", ignoreCase = true)
    }

    suspend fun uploadAttachments(
        context: Context,
        uid: String,
        conversationId: String,
        messageId: String,
        attachments: List<ComposerAttachment>,
    ): List<ComposerAttachment> {
        if (attachments.isEmpty()) return attachments
        return attachments.map { att ->
            uploadOne(context, uid, conversationId, messageId, att)
        }
    }

    private suspend fun uploadOne(
        context: Context,
        uid: String,
        conversationId: String,
        messageId: String,
        attachment: ComposerAttachment,
    ): ComposerAttachment {
        val local = attachment.uri
        if (local == null || isRemoteUri(local)) return attachment

        // Garante token fresco (regras Storage exigem auth)
        FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()

        val safeName = sanitizeFileName(attachment.name)
        val path =
            "users/$uid/conversations/$conversationId/messages/$messageId/" +
                "attachments/${attachment.id}/$safeName"
        val ref = storage.reference.child(path)
        val mime = attachment.mime.ifBlank { "application/octet-stream" }

        withContext(Dispatchers.IO) {
            // putFile aceita content:// e file:// no Android
            ref.putFile(local).await()
        }

        val downloadUrl = ref.downloadUrl.await().toString()
        return attachment.copy(uri = Uri.parse(downloadUrl))
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("""[^\w.\-()+@]"""), "_").trim()
        return cleaned.takeIf { it.isNotEmpty() }?.take(120) ?: "arquivo"
    }
}

/** kind Firestore: image | file (vídeo = file, como no mobile). */
fun ComposerAttachment.toFirestoreKind(): String = when (kind) {
    AttachmentKind.IMAGE -> "image"
    AttachmentKind.VIDEO, AttachmentKind.FILE -> "file"
}

fun firestoreKindToAttachment(
    kind: String?,
    mime: String,
): AttachmentKind {
    if (kind == "image") return AttachmentKind.IMAGE
    val m = mime.lowercase(Locale.US)
    if (m.startsWith("video/")) return AttachmentKind.VIDEO
    return AttachmentKind.FILE
}
