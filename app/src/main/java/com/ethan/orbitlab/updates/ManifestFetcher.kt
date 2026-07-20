package com.ethan.orbitlab.updates

import android.content.Context
import com.ethan.orbitlab.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lê o manifesto público do orbit-releases.
 *
 * Canal pelo **build** (`BuildConfig.ORBIT_CHANNEL`):
 * - `stable` → updates.json
 * - `beta` / `lab` → updates-beta.json (lab testa o mesmo canal que vai herdar)
 *
 * API do GitHub primeiro (fresca); `raw` de reserva (sem rate-limit de 60/h).
 */
object ManifestFetcher {

    private const val API_STABLE =
        "https://api.github.com/repos/NowardEthan/orbit-releases/contents/updates.json"
    private const val API_BETA =
        "https://api.github.com/repos/NowardEthan/orbit-releases/contents/updates-beta.json"
    private const val RAW_STABLE =
        "https://raw.githubusercontent.com/NowardEthan/orbit-releases/main/updates.json"
    private const val RAW_BETA =
        "https://raw.githubusercontent.com/NowardEthan/orbit-releases/main/updates-beta.json"

    private const val PREFS = "orbit.updates"
    private const val CACHE_KEY_STABLE = "manifest.v1"
    private const val CACHE_KEY_BETA = "manifest.beta.v1"
    private const val USER_AGENT = "Orbit-Lab"

    private val validTags = setOf("novidade", "correcao", "aviso")

    fun canalDoBuild(): String = BuildConfig.ORBIT_CHANNEL

    fun isCanalBeta(): Boolean {
        val canal = canalDoBuild()
        return canal == "beta" || canal == "lab"
    }

    fun currentAppVersion(): String = BuildConfig.VERSION_NAME

    fun currentVersionCode(): Int = BuildConfig.VERSION_CODE

    fun fetchManifest(context: Context): OrbitManifest {
        val beta = isCanalBeta()
        val json = try {
            httpGetJson(
                url = if (beta) API_BETA else API_STABLE,
                accept = "application/vnd.github.raw",
            )
        } catch (_: Exception) {
            httpGetJson(url = if (beta) RAW_BETA else RAW_STABLE)
        }

        val manifest = coerceManifest(json)
            ?: throw IllegalStateException("Manifesto de updates inválido.")
        writeCache(context, beta, manifest)
        return manifest
    }

    fun readCachedManifest(context: Context): OrbitManifest? {
        return try {
            val beta = isCanalBeta()
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(if (beta) CACHE_KEY_BETA else CACHE_KEY_STABLE, null)
                ?: return null
            coerceManifest(JSONObject(raw))
        } catch (_: Exception) {
            null
        }
    }

    private fun writeCache(context: Context, beta: Boolean, manifest: OrbitManifest) {
        try {
            val key = if (beta) CACHE_KEY_BETA else CACHE_KEY_STABLE
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(key, toJson(manifest).toString())
                .apply()
        } catch (_: Exception) {
            // Cache é best-effort.
        }
    }

    private fun httpGetJson(url: String, accept: String? = null): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            if (accept != null) setRequestProperty("Accept", accept)
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }
            val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            return JSONObject(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun coerceManifest(raw: JSONObject): OrbitManifest? {
        val latestVersion = raw.optString("latestVersion", "").takeIf { it.isNotBlank() }
            ?: return null
        return OrbitManifest(
            app = raw.optString("app").takeIf { it.isNotBlank() },
            manifestVersion = raw.optInt("manifestVersion").takeIf { raw.has("manifestVersion") },
            latestVersion = latestVersion,
            latestVersionCode = if (raw.has("latestVersionCode")) raw.optInt("latestVersionCode") else null,
            publishedAt = raw.optString("publishedAt").takeIf { it.isNotBlank() },
            apkUrl = raw.optString("apkUrl").takeIf { it.isNotBlank() },
            minSupportedVersion = raw.optString("minSupportedVersion").takeIf { it.isNotBlank() },
            mandatory = raw.optBoolean("mandatory", false),
            news = coerceNews(raw.optJSONArray("news")),
        )
    }

    private fun coerceNews(arr: JSONArray?): List<OrbitNewsItem> {
        if (arr == null) return emptyList()
        val items = mutableListOf<OrbitNewsItem>()
        for (i in 0 until arr.length()) {
            val n = arr.optJSONObject(i) ?: continue
            val id = n.optString("id").takeIf { it.isNotBlank() } ?: continue
            val title = n.optString("title").takeIf { it.isNotBlank() } ?: continue
            val tagRaw = n.optString("tag", "aviso")
            val tag = when (if (tagRaw in validTags) tagRaw else "aviso") {
                "novidade" -> OrbitNewsTag.NOVIDADE
                "correcao" -> OrbitNewsTag.CORRECAO
                else -> OrbitNewsTag.AVISO
            }
            items.add(
                OrbitNewsItem(
                    id = id,
                    date = n.optString("date", ""),
                    tag = tag,
                    title = title,
                    body = n.optString("body", ""),
                    version = n.optString("version").takeIf { it.isNotBlank() },
                ),
            )
        }
        return items
    }

    private fun toJson(m: OrbitManifest): JSONObject {
        val news = JSONArray()
        for (item in m.news) {
            news.put(
                JSONObject()
                    .put("id", item.id)
                    .put("date", item.date)
                    .put("tag", when (item.tag) {
                        OrbitNewsTag.NOVIDADE -> "novidade"
                        OrbitNewsTag.CORRECAO -> "correcao"
                        OrbitNewsTag.AVISO -> "aviso"
                    })
                    .put("title", item.title)
                    .put("body", item.body)
                    .put("version", item.version),
            )
        }
        return JSONObject()
            .put("app", m.app)
            .put("manifestVersion", m.manifestVersion)
            .put("latestVersion", m.latestVersion)
            .put("latestVersionCode", m.latestVersionCode)
            .put("publishedAt", m.publishedAt)
            .put("apkUrl", m.apkUrl)
            .put("minSupportedVersion", m.minSupportedVersion)
            .put("mandatory", m.mandatory)
            .put("news", news)
    }
}
