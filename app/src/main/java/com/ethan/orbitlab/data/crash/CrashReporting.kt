package com.ethan.orbitlab.data.crash

import android.content.Context
import android.util.Log
import com.ethan.orbitlab.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Crashlytics — Fase 3 do endurecimento.
 *
 * Política de PII (não negociável):
 * - Identidade: só **uid** opaco do Firebase Auth (nunca e-mail, nome, @).
 * - Nunca logar texto de chat, prompts, URLs de mídia, valores financeiros.
 * - Keys custom: tela atual, versionName, versionCode, canal.
 */
object CrashReporting {
    private const val TAG = "OrbitCrash"
    private const val KEY_TELA = "tela"
    private const val KEY_VERSION = "version_name"
    private const val KEY_CODE = "version_code"
    private const val KEY_CANAL = "canal"

    @Volatile
    private var pronto = false

    fun init(context: Context) {
        if (pronto) return
        val crash = FirebaseCrashlytics.getInstance()
        crash.setCrashlyticsCollectionEnabled(true)
        crash.setCustomKey(KEY_VERSION, BuildConfig.VERSION_NAME)
        crash.setCustomKey(KEY_CODE, BuildConfig.VERSION_CODE)
        crash.setCustomKey(KEY_CANAL, "lab")
        crash.setCustomKey(KEY_TELA, "boot")

        // Log local + encaminha pro handler do Crashlytics (não recordException aqui —
        // senão o fatal seria reportado duas vezes).
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Crash em ${thread.name}", throwable)
            runCatching {
                context.getSharedPreferences("orbit_crash_logs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("last_crash", throwable.stackTraceToString())
                    .apply()
            }
            runCatching { crash.log("uncaught:${thread.name}") }
            anterior?.uncaughtException(thread, throwable)
        }
        pronto = true
        Log.i(TAG, "Crashlytics ativo (${BuildConfig.VERSION_NAME}/${BuildConfig.VERSION_CODE})")
    }

    /** Só o uid — sem e-mail nem displayName. */
    fun setUsuario(uid: String?) {
        if (!pronto) return
        val crash = FirebaseCrashlytics.getInstance()
        if (uid.isNullOrBlank()) {
            crash.setUserId("")
            crash.log("auth:saiu")
        } else {
            crash.setUserId(uid)
            crash.log("auth:entrou")
        }
    }

    /** Breadcrumb de navegação — nomes de aba curtos (INICIO, FINANCAS…). */
    fun setTela(tela: String) {
        if (!pronto) return
        val nome = tela.trim().take(48).ifBlank { "desconhecida" }
        val crash = FirebaseCrashlytics.getInstance()
        crash.setCustomKey(KEY_TELA, nome)
        crash.log("tela:$nome")
    }

    /**
     * Evento leve sem PII. Use rótulos curtos (`cota_parede`, `update_banner`).
     * Nunca passe mensagem do usuário ou valor em reais.
     */
    fun breadcrumb(rotulo: String) {
        if (!pronto) return
        FirebaseCrashlytics.getInstance().log(rotulo.trim().take(100))
    }

    /** Exceção não fatal (sem derrubar o app). */
    fun registrarNaoFatal(erro: Throwable, rotulo: String? = null) {
        if (!pronto) return
        val crash = FirebaseCrashlytics.getInstance()
        rotulo?.let { crash.log(it.take(100)) }
        crash.recordException(erro)
    }

    /**
     * Crash de teste proposital — só pra validar o painel Firebase.
     * Chamado por gesto escondido em Ajustes (não é fluxo de produto).
     */
    fun forcarCrashDeTeste() {
        FirebaseCrashlytics.getInstance().log("teste_crashlytics_manual")
        throw RuntimeException("Crashlytics teste OrbitLab ${BuildConfig.VERSION_NAME}")
    }
}
