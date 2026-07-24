package com.ethan.orbitlab.data

import android.content.Context
import android.net.Uri
import com.ethan.orbitlab.data.firebase.ChatMediaUpload
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val plan: String = "free",
    val memberSinceLabel: String = "",
    val milestones: ProfileMilestones = ProfileMilestones(),
)

data class ProfileMilestones(
    val voiceMessage: Boolean = false,
    val fileAttachment: Boolean = false,
    val imageAttachment: Boolean = false,
    val documentReference: Boolean = false,
)

enum class ProfileImageKind { AVATAR, COVER }

/**
 * Perfil em `users/{uid}` — espelho do orbit-mobile (+ username / @).
 */
object UserProfileRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage get() = FirebaseStorage.getInstance()

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private var reg: ListenerRegistration? = null
    private var boundUid: String? = null

    private val usernameRegex = Regex("^[a-z0-9_]{3,24}$")

    fun init() {
        scope.launch {
            AuthRepository.uidFlow().collect { uid ->
                reg?.remove()
                reg = null
                boundUid = uid
                if (uid == null) {
                    _profile.value = UserProfile()
                } else {
                    attach(uid)
                }
            }
        }
    }

    private fun attach(uid: String) {
        reg = db.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                val session = AuthRepository.session.value
                if (snap == null || !snap.exists()) {
                    _profile.value = UserProfile(
                        uid = uid,
                        displayName = session?.displayName.orEmpty(),
                        username = defaultUsername(session?.email, session?.displayName),
                        email = session?.email.orEmpty(),
                        avatarUrl = auth.currentUser?.photoUrl?.toString(),
                    )
                    return@addSnapshotListener
                }
                val data = snap.data.orEmpty()
                val avatar = (data["avatarUrl"] as? String)?.takeIf { it.isNotBlank() }
                    ?: (data["photoURL"] as? String)?.takeIf { it.isNotBlank() }
                    ?: auth.currentUser?.photoUrl?.toString()
                val created = data["createdAt"]
                val memberSince = when (created) {
                    is Timestamp -> formatMemberSince(created.toDate())
                    is Number -> formatMemberSince(Date(created.toLong()))
                    else -> ""
                }
                val milestonesRaw = data["milestones"] as? Map<*, *>
                val email = (data["email"] as? String)?.takeIf { it.isNotBlank() }
                    ?: session?.email.orEmpty()
                val displayName = (data["displayName"] as? String)?.takeIf { it.isNotBlank() }
                    ?: session?.displayName.orEmpty()
                val username = (data["username"] as? String)?.trim()?.lowercase(Locale.US)
                    ?.takeIf { it.isNotBlank() }
                    ?: defaultUsername(email, displayName)
                _profile.value = UserProfile(
                    uid = uid,
                    displayName = displayName,
                    username = username,
                    email = email,
                    bio = (data["bio"] as? String).orEmpty(),
                    avatarUrl = avatar,
                    coverUrl = (data["coverUrl"] as? String)?.takeIf { it.isNotBlank() },
                    plan = (data["plan"] as? String)?.ifBlank { null } ?: "free",
                    memberSinceLabel = memberSince,
                    milestones = ProfileMilestones(
                        voiceMessage = milestonesRaw?.get("voiceMessage") as? Boolean ?: false,
                        fileAttachment = milestonesRaw?.get("fileAttachment") as? Boolean ?: false,
                        imageAttachment = milestonesRaw?.get("imageAttachment") as? Boolean ?: false,
                        documentReference = milestonesRaw?.get("documentReference") as? Boolean
                            ?: false,
                    ),
                )
            }
    }

    fun validateUsername(raw: String): String? {
        val u = raw.trim().lowercase(Locale.US).removePrefix("@")
        if (u.length < 3) return "O @ precisa ter pelo menos 3 caracteres."
        if (u.length > 24) return "O @ pode ter no máximo 24 caracteres."
        if (!usernameRegex.matches(u)) {
            return "Use só letras minúsculas, números e _."
        }
        return null
    }

    suspend fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
    ): Result<Unit> {
        val uid = boundUid ?: return Result.failure(IllegalStateException("Sem sessão."))
        val nome = displayName.trim()
        if (nome.length < 2) {
            return Result.failure(IllegalArgumentException("Informe um nome com pelo menos 2 letras."))
        }
        val handle = username.trim().lowercase(Locale.US).removePrefix("@")
        validateUsername(handle)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        return try {
            // Unicidade best-effort (não bloqueia se a query falhar por índice)
            val taken = runCatching {
                db.collection("users")
                    .whereEqualTo("username", handle)
                    .limit(2)
                    .get()
                    .await()
                    .documents
                    .any { it.id != uid }
            }.getOrDefault(false)
            if (taken) {
                return Result.failure(IllegalArgumentException("Esse @ já está em uso."))
            }

            val user = auth.currentUser
            if (user != null && user.displayName != nome) {
                user.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(nome).build(),
                ).await()
            }
            db.collection("users").document(uid).set(
                mapOf(
                    "displayName" to nome,
                    "username" to handle,
                    "bio" to bio.trim().take(160),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfileImage(
        context: Context,
        kind: ProfileImageKind,
        uri: Uri,
    ): Result<String> {
        val uid = boundUid ?: return Result.failure(IllegalStateException("Sem sessão."))
        return try {
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val ext = when {
                mime.contains("png") -> "png"
                mime.contains("webp") -> "webp"
                else -> "jpg"
            }
            val pathKind = if (kind == ProfileImageKind.AVATAR) "avatar" else "cover"
            val path = "users/$uid/profile/$pathKind.$ext"
            // Mesmo caminho do anexo do chat: token explícito, com teimosia (ver ChatMediaUpload).
            val url = ChatMediaUpload.subirArquivo(context, path, uri, mime)
            val patch = when (kind) {
                ProfileImageKind.AVATAR -> mapOf(
                    "avatarUrl" to url,
                    "photoURL" to url,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                ProfileImageKind.COVER -> mapOf(
                    "coverUrl" to url,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            }
            db.collection("users").document(uid).set(patch, SetOptions.merge()).await()
            if (kind == ProfileImageKind.AVATAR) {
                auth.currentUser?.updateProfile(
                    UserProfileChangeRequest.Builder().setPhotoUri(Uri.parse(url)).build(),
                )?.await()
            }
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeProfileImage(kind: ProfileImageKind): Result<Unit> {
        val uid = boundUid ?: return Result.failure(IllegalStateException("Sem sessão."))
        return try {
            val patch = when (kind) {
                ProfileImageKind.AVATAR -> mapOf(
                    "avatarUrl" to null,
                    "photoURL" to null,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                ProfileImageKind.COVER -> mapOf(
                    "coverUrl" to null,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            }
            db.collection("users").document(uid).set(patch, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun defaultUsername(email: String?, displayName: String?): String {
        val fromEmail = email?.substringBefore("@")
            ?.lowercase(Locale.US)
            ?.replace(Regex("[^a-z0-9_]"), "")
            ?.take(24)
        if (!fromEmail.isNullOrBlank() && fromEmail.length >= 3) return fromEmail
        val fromName = displayName
            ?.lowercase(Locale.US)
            ?.replace(Regex("[^a-z0-9_]"), "")
            ?.take(24)
        if (!fromName.isNullOrBlank() && fromName.length >= 3) return fromName
        return "orbit"
    }

    private fun formatMemberSince(date: Date): String {
        val fmt = SimpleDateFormat("MMM 'de' yyyy", Locale("pt", "BR"))
        return "Membro desde ${fmt.format(date)}"
    }

    fun planLabel(plan: String): String = when (plan.lowercase(Locale.US)) {
        "pro", "plus", "premium" -> "Pro"
        "free", "" -> "Grátis"
        else -> plan.replaceFirstChar { it.uppercase() }
    }
}
