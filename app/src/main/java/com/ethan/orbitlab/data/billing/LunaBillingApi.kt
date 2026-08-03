package com.ethan.orbitlab.data.billing

import com.ethan.orbitlab.data.lunaapi.LunaApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Cliente das rotas de billing da mobile-api — mesma base do chat
 * ([LunaApiConfig.baseUrl]), autenticado com o ID token do Firebase.
 *
 *  - GET  /v1/billing/usage    → retrato de uso (a carteira)
 *  - POST /v1/billing/checkout → link do Asaas pra assinar
 *  - POST /v1/billing/sync     → promove o plano após pagar
 *
 * Todas as POST exigem conta Google com email (o servidor recusa anônimo).
 */
object LunaBillingApi {
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json".toMediaType()

    private fun url(path: String) = "${LunaApiConfig.baseUrl}$path"

    sealed class CheckoutResult {
        data class Ok(val url: String) : CheckoutResult()
        data class Erro(val mensagem: String) : CheckoutResult()
    }

    data class SyncResult(val ok: Boolean, val plano: String?, val erro: String?)

    /** Lê a carteira. `null` = não deu (rede/sem sessão) — o chamador mantém o retrato anterior. */
    suspend fun buscarUso(idToken: String): UsageSnapshot? = withContext(Dispatchers.IO) {
        if (!LunaApiConfig.isConfigured()) return@withContext null
        val req = Request.Builder()
            .url(url("/v1/billing/usage"))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $idToken")
            .get()
            .build()
        try {
            http.newCall(req).execute().use { res ->
                val raw = res.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull() ?: return@use null
                if (!json.optBoolean("ok", false)) return@use null
                val usage = json.optJSONObject("usage") ?: return@use null
                parseUsage(usage)
            }
        } catch (_: IOException) {
            null
        }
    }

    /** Abre o checkout Asaas. `period` = "monthly" | "annual". */
    suspend fun checkout(
        idToken: String,
        planId: PlanId,
        period: String,
        cpfCnpj: String,
    ): CheckoutResult = withContext(Dispatchers.IO) {
        if (!LunaApiConfig.isConfigured()) {
            return@withContext CheckoutResult.Erro("Pagamentos indisponíveis nesta versão.")
        }
        if (planId != PlanId.PLUS && planId != PlanId.PRO) {
            return@withContext CheckoutResult.Erro("Plano sem checkout.")
        }
        val body = JSONObject().apply {
            put("planId", planId.id)
            put("period", period)
            put("cpfCnpj", cpfCnpj)
        }
        val req = Request.Builder()
            .url(url("/v1/billing/checkout"))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $idToken")
            .post(body.toString().toRequestBody(JSON))
            .build()
        try {
            http.newCall(req).execute().use { res ->
                val raw = res.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull()
                    ?: return@use CheckoutResult.Erro("Resposta inesperada (HTTP ${res.code}).")
                val u = json.optString("url")
                if (json.optBoolean("ok", false) && u.isNotBlank()) {
                    CheckoutResult.Ok(u)
                } else {
                    CheckoutResult.Erro(
                        json.optString("error").ifBlank { "Não foi possível iniciar o pagamento." },
                    )
                }
            }
        } catch (e: IOException) {
            CheckoutResult.Erro(e.message?.takeIf { it.isNotBlank() } ?: "Checkout indisponível.")
        }
    }

    /** Confere no Asaas se a assinatura já está paga e promove o plano. */
    suspend fun sync(idToken: String): SyncResult = withContext(Dispatchers.IO) {
        if (!LunaApiConfig.isConfigured()) {
            return@withContext SyncResult(false, null, "Servidor de billing indisponível.")
        }
        val req = Request.Builder()
            .url(url("/v1/billing/sync"))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $idToken")
            .post("{}".toRequestBody(JSON))
            .build()
        try {
            http.newCall(req).execute().use { res ->
                val raw = res.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull()
                    ?: return@use SyncResult(false, null, "Resposta inesperada (HTTP ${res.code}).")
                SyncResult(
                    ok = json.optBoolean("ok", false),
                    plano = json.optString("plan").takeIf { it.isNotBlank() },
                    erro = json.optString("error").takeIf { it.isNotBlank() },
                )
            }
        } catch (e: IOException) {
            SyncResult(false, null, e.message?.takeIf { it.isNotBlank() } ?: "Sync indisponível.")
        }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) optLong(key) else null

    private fun parseUsage(o: JSONObject): UsageSnapshot {
        val planId = PlanId.fromId(o.optString("planId"))
        val weekly = o.optJSONObject("weeklyTokens")?.let { w ->
            WeeklyTokens(
                used = w.optLong("used"),
                limit = w.optLong("limit"),
                remaining = w.optLong("remaining"),
                resetsAtMs = w.optLongOrNull("resetsAtMs"),
            )
        }
        val reduced = o.optJSONObject("reducedMode")?.let { r ->
            ReducedMode(
                available = r.optBoolean("available", false),
                dailyUsed = r.optLong("dailyUsed"),
                dailyLimit = r.optLong("dailyLimit"),
                dailyRemaining = r.optLong("dailyRemaining"),
                resetsAtMs = r.optLongOrNull("resetsAtMs"),
            )
        }
        return UsageSnapshot(
            planId = planId,
            cycle = o.optString("cycle").ifBlank { "window" },
            windowHours = if (o.has("windowHours") && !o.isNull("windowHours")) o.optInt("windowHours") else null,
            usedTokens = o.optLong("usedTokens"),
            windowTokenLimit = o.optLongOrNull("windowTokenLimit"),
            remainingTokens = o.optLongOrNull("remainingTokens"),
            bonusTurns = o.optInt("bonusTurns"),
            resetsAtMs = o.optLongOrNull("resetsAtMs"),
            weeklyTokens = weekly,
            reducedMode = reduced,
            loading = false,
        )
    }
}
