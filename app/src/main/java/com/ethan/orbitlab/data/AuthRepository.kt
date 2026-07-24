package com.ethan.orbitlab.data

import android.app.Activity
import android.content.Context
import com.ethan.orbitlab.data.firebase.FirebaseBootstrap
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

enum class AuthProvider {
    AURA,
    GOOGLE,
}

data class AuthSession(
    val uid: String,
    val email: String,
    val displayName: String,
    val provider: AuthProvider,
)

/**
 * Auth real via Firebase (projeto luna-8787d).
 * Conta Aura = email/senha; Google = Credential Manager → Firebase.
 * Sem anônimo / visitante.
 */
object AuthRepository {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private val _session = MutableStateFlow<AuthSession?>(null)
    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    private val _authReady = MutableStateFlow(false)
    val authReady: StateFlow<Boolean> = _authReady.asStateFlow()

    /**
     * Tempo que damos ao Firebase pra ler a conta do disco antes de mostrar o login.
     *
     * Curto de propósito: o SDK carrega o usuário guardado ao criar o [FirebaseAuth], então
     * se ele não veio no primeiro instante, esperar não o faz aparecer. Os 8s de antes eram
     * 8s de tela parada à toa — a reentrada é que resolve, e ela corre em paralelo.
     */
    private const val GRACA_RESTAURO_MS = 1_500L

    /** Enquanto true, `currentUser == null` significa «ainda restaurando», não «saiu». */
    private var restaurando = false

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun init(@Suppress("UNUSED_PARAMETER") context: Context) {
        val jaEntrouAqui = PrefsRepository.sessaoUid != null
        val agora = auth.currentUser
        AuthDiag.anota(
            "abriu · marcador=${if (jaEntrouAqui) "sim" else "não"} " +
                "· firebase=${if (agora != null) "conta em mãos" else "vazio"}",
        )

        when {
            // Conta já em mãos: entra direto, sem piscar login.
            agora != null -> entrou(agora)
            // Havia sessão neste aparelho — segura o splash até o Firebase responder.
            jaEntrouAqui -> {
                restaurando = true
                escopo.launch {
                    delay(GRACA_RESTAURO_MS)
                    if (restaurando) {
                        restaurando = false
                        AuthDiag.anota(
                            "prazo de ${GRACA_RESTAURO_MS / 1000}s estourou · firebase=" +
                                if (auth.currentUser != null) "chegou" else "continua vazio",
                        )
                        // Estourou o prazo: destrava a tela, mas NÃO conclui que ele saiu.
                        // Apagar o marcador aqui era um efeito dominó — uma restauração
                        // lenta apagava a pista e TODA abertura seguinte ia direto pro
                        // login. Só sair de verdade limpa o marcador.
                        if (auth.currentUser == null) _authReady.value = true
                    }
                }
            }
            // Nunca entrou: login na hora.
            else -> _authReady.value = true
        }

        // Listener único — restaura sessão persistida pelo Firebase Auth
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                if (_session.value == null) AuthDiag.anota("firebase devolveu a conta")
                restaurando = false
                entrou(user)
            } else if (!restaurando) {
                AuthDiag.anota("firebase avisou: sem conta")
                _session.value = null
                _authReady.value = true
            }
        }
    }

    /** Já houve conta neste aparelho? (marcador local, sobrevive ao processo) */
    fun tinhaSessaoAqui(): Boolean = PrefsRepository.sessaoUid != null

    /**
     * Reentra sozinho na conta Google já autorizada neste aparelho.
     *
     * Quando o Firebase perde a sessão guardada (o app é morto no meio, o cache é
     * limpo, o token vence), a conta do Google continua autorizada aqui — dá pra voltar
     * sem pedir nada a ele. Só usa credencial já concedida: se precisar de interação,
     * desiste e o login normal aparece.
     */
    suspend fun restaurarGoogleSilencioso(activity: Activity): Boolean {
        auth.currentUser?.let {
            AuthDiag.anota("reentrada dispensada · a conta chegou a tempo")
            entrou(it)
            return true
        }
        if (!tinhaSessaoAqui()) {
            AuthDiag.anota("reentrada pulada · sem marcador (saiu de propósito?)")
            return false
        }
        // 1ª: contas já autorizadas aqui, sem nenhum toque.
        // 2ª: qualquer conta Google do aparelho — vira uma folha de escolha (1 toque),
        //     que ainda é bem melhor que refazer o login inteiro.
        for (soAutorizadas in listOf(true, false)) {
            val etapa = if (soAutorizadas) "silenciosa" else "escolha de conta"
            AuthDiag.anota("reentrada $etapa começou")
            try {
                val opcao = GetGoogleIdOption.Builder()
                    .setServerClientId(FirebaseBootstrap.WEB_CLIENT_ID)
                    .setFilterByAuthorizedAccounts(soAutorizadas)
                    .setAutoSelectEnabled(true)
                    .build()
                val pedido = GetCredentialRequest.Builder().addCredentialOption(opcao).build()
                val resposta = CredentialManager.create(activity).getCredential(activity, pedido)
                val cred = resposta.credential
                if (cred is CustomCredential &&
                    cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleId = GoogleIdTokenCredential.createFrom(cred.data)
                    val firebaseCred = GoogleAuthProvider.getCredential(googleId.idToken, null)
                    val user = auth.signInWithCredential(firebaseCred).await().user
                    if (user != null) {
                        AuthDiag.anota("reentrada $etapa deu certo")
                        entrou(user)
                        return true
                    }
                    AuthDiag.anota("reentrada $etapa: Firebase aceitou sem usuário")
                } else {
                    AuthDiag.anota("reentrada $etapa: credencial de outro tipo")
                }
            } catch (e: Exception) {
                AuthDiag.anota("reentrada $etapa falhou · ${e.javaClass.simpleName}: ${e.message?.take(90)}")
            }
        }
        return false
    }

    private fun entrou(user: FirebaseUser) {
        PrefsRepository.sessaoUid = user.uid
        _session.value = user.toSession()
        _authReady.value = true
    }

    private fun saiu() {
        PrefsRepository.sessaoUid = null
        // Sair zera o «onde eu estava»: quem entrar depois não cai na conversa do anterior.
        PrefsRepository.ultimaConversa = null
        PrefsRepository.ultimaAba = null
        PrefsRepository.setAncora("", null)
        _session.value = null
        _authReady.value = true
    }

    fun validateEmail(email: String): String? {
        val t = email.trim()
        if (t.isEmpty()) return "Informe o email."
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(t).matches()) {
            return "Email inválido."
        }
        return null
    }

    fun validatePassword(password: String, criarConta: Boolean): String? {
        if (password.isEmpty()) return "Informe a senha."
        if (password.length < 6) return "Mínimo 6 caracteres."
        if (criarConta && password.length < 8) {
            return "Pra criar conta, use pelo menos 8 caracteres."
        }
        return null
    }

    suspend fun entrarComAura(email: String, password: String): Result<AuthSession> {
        validateEmail(email)?.let { return Result.failure(IllegalArgumentException(it)) }
        validatePassword(password, criarConta = false)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
                ?: return Result.failure(IllegalStateException("Login sem usuário."))
            ensureUserProfile(user)
            Result.success(user.toSession())
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException(mapAuthError(e)))
        }
    }

    suspend fun criarContaAura(
        nome: String,
        email: String,
        password: String,
    ): Result<AuthSession> {
        val n = nome.trim()
        if (n.length < 2) {
            return Result.failure(IllegalArgumentException("Informe como podemos te chamar."))
        }
        validateEmail(email)?.let { return Result.failure(IllegalArgumentException(it)) }
        validatePassword(password, criarConta = true)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
                ?: return Result.failure(IllegalStateException("Conta criada sem usuário."))
            user.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(n).build(),
            ).await()
            ensureUserProfile(user)
            Result.success(user.toSession())
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException(mapAuthError(e)))
        }
    }

    suspend fun entrarComGoogle(activity: Activity): Result<AuthSession> {
        return try {
            val credentialManager = CredentialManager.create(activity)
            // Botão explícito → Sign in with Google (não One Tap / GetGoogleIdOption)
            val googleOption = GetSignInWithGoogleOption.Builder(FirebaseBootstrap.WEB_CLIENT_ID)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()
            val response = credentialManager.getCredential(activity, request)
            val cred = response.credential
            if (cred is CustomCredential &&
                cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleId = GoogleIdTokenCredential.createFrom(cred.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleId.idToken, null)
                val result = auth.signInWithCredential(firebaseCred).await()
                val user = result.user
                    ?: return Result.failure(IllegalStateException("Google sem usuário."))
                ensureUserProfile(user)
                Result.success(user.toSession())
            } else {
                Result.failure(IllegalArgumentException("Resposta do Google inesperada."))
            }
        } catch (e: GetCredentialCancellationException) {
            // Muitas vezes NÃO é o utilizador a cancelar — é package/SHA-1 em falta
            // no cliente Android OAuth (com.ethan.orbitlab). Ver AGENTS.md.
            val detail = e.message?.takeIf { it.isNotBlank() }
            Result.failure(
                IllegalArgumentException(
                    if (detail != null) {
                        "Login Google cancelado ($detail). Se não cancelaste, falta " +
                            "registar o package com.ethan.orbitlab + SHA-1 do debug.keystore " +
                            "no Firebase / Google Cloud."
                    } else {
                        "Login Google cancelado. Se não cancelaste, falta registar " +
                            "com.ethan.orbitlab + SHA-1 no Firebase / Google Cloud."
                    },
                ),
            )
        } catch (e: GetCredentialException) {
            Result.failure(
                IllegalArgumentException(
                    "Google Sign-In falhou. ${e.message ?: "Tenta de novo em alguns minutos " +
                        "(OAuth do Firebase pode demorar a propagar)."}",
                ),
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException(mapAuthError(e)))
        }
    }

    suspend fun getIdToken(): String? {
        return auth.currentUser?.getIdToken(false)?.await()?.token
    }

    suspend fun sair() {
        restaurando = false
        AuthDiag.anota("saiu no botão")
        auth.signOut()
        saiu()
    }

    /** Apagar conta no lab = signOut (delete server-side fica no fluxo de privacidade do mobile). */
    suspend fun apagarTudo() {
        sair()
    }

    private suspend fun ensureUserProfile(user: FirebaseUser) {
        val ref = db.collection("users").document(user.uid)
        val snap = ref.get().await()
        val patch = hashMapOf<String, Any?>(
            "displayName" to user.displayName,
            "email" to user.email,
            "photoURL" to user.photoUrl?.toString(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (!snap.exists()) {
            patch["plan"] = "free"
            patch["createdAt"] = FieldValue.serverTimestamp()
            ref.set(patch).await()
        } else {
            ref.set(patch, SetOptions.merge()).await()
        }
    }

    private fun FirebaseUser.toSession(): AuthSession {
        val isGoogle = providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
        return AuthSession(
            uid = uid,
            email = email.orEmpty().ifBlank { "sem-email" },
            displayName = displayName?.takeIf { it.isNotBlank() }
                ?: email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                ?: "Você",
            provider = if (isGoogle) AuthProvider.GOOGLE else AuthProvider.AURA,
        )
    }

    private fun mapAuthError(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            "ERROR_INVALID_EMAIL" in msg || "invalid-email" in msg -> "Email inválido."
            "ERROR_WRONG_PASSWORD" in msg || "wrong-password" in msg ||
                "INVALID_LOGIN_CREDENTIALS" in msg || "invalid-credential" in msg ->
                "Email ou senha incorretos."
            "ERROR_USER_NOT_FOUND" in msg || "user-not-found" in msg ->
                "Nenhuma conta com este email."
            "ERROR_EMAIL_ALREADY_IN_USE" in msg || "email-already-in-use" in msg ->
                "Já existe uma Conta Aura com este email."
            "ERROR_WEAK_PASSWORD" in msg || "weak-password" in msg ->
                "Senha fraca demais."
            "ERROR_NETWORK_REQUEST_FAILED" in msg || "network" in msg.lowercase() ->
                "Sem rede. Tenta de novo."
            else -> e.message?.takeIf { it.isNotBlank() } ?: "Não deu pra autenticar."
        }
    }
}

/** Observa o usuário Firebase atual (uid) — útil pra sync. */
fun AuthRepository.uidFlow(): Flow<String?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
    FirebaseAuth.getInstance().addAuthStateListener(listener)
    awaitClose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
}
