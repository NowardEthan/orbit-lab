package com.ethan.orbitlab.data.updates

import android.content.Context
import android.content.SharedPreferences
import com.ethan.orbitlab.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val CACHE_KEY = "orbitlab.updates.manifest.v1"
private const val SEEN_KEY = "orbitlab.updates.seen.v1"

private const val MANIFEST_API =
    "https://api.github.com/repos/NowardEthan/orbit-releases/contents/updates-lab.json"
private const val MANIFEST_RAW =
    "https://raw.githubusercontent.com/NowardEthan/orbit-releases/main/updates-lab.json"

private val http by lazy {
    OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}

object ManifestFetcher {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("orbitlab_updates", Context.MODE_PRIVATE)
    }

    fun currentAppVersion(): String = BuildConfig.VERSION_NAME

    fun currentVersionCode(): Int = BuildConfig.VERSION_CODE

    fun readSeenSignature(): String? =
        if (::prefs.isInitialized) prefs.getString(SEEN_KEY, null) else null

    fun writeSeenSignature(signature: String) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(SEEN_KEY, signature).apply()
    }

    suspend fun readCachedManifest(): OrbitManifest? = withContext(Dispatchers.IO) {
        if (!::prefs.isInitialized) return@withContext null
        val raw = prefs.getString(CACHE_KEY, null) ?: return@withContext null
        runCatching { coerceManifest(JSONObject(raw)) }.getOrNull()
    }

    suspend fun fetchManifest(): OrbitManifest = withContext(Dispatchers.IO) {
        val json = runCatching { fetchFromApi() }.getOrElse { fetchFromRaw() }
        val manifest = coerceManifest(json)
            ?: throw IllegalStateException("Manifesto de updates inválido.")
        prefs.edit().putString(CACHE_KEY, json.toString()).apply()
        manifest
    }

    private fun fetchFromApi(): JSONObject {
        val request = Request.Builder()
            .url(MANIFEST_API)
            .header("Accept", "application/vnd.github.raw")
            .header("User-Agent", "OrbitLab")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return JSONObject(response.body?.string().orEmpty())
        }
    }

    private fun fetchFromRaw(): JSONObject {
        val request = Request.Builder()
            .url(MANIFEST_RAW)
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP raw ${response.code}")
            return JSONObject(response.body?.string().orEmpty())
        }
    }

    fun groupNewsByVersion(news: List<OrbitNewsItem>): List<OrbitNewsSection> {
        val byKey = linkedMapOf<String, MutableList<OrbitNewsItem>>()
        val dates = mutableMapOf<String, String>()
        val versions = mutableMapOf<String, String?>()

        for (item in news) {
            val key = item.version ?: "__sem_versao__"
            byKey.getOrPut(key) { mutableListOf() }.add(item)
            versions[key] = item.version
            dates[key] = maxOf(dates.getOrDefault(key, ""), item.date)
        }

        return byKey.keys.map { key ->
            OrbitNewsSection(
                key = key,
                version = versions[key],
                date = dates[key].orEmpty(),
                items = byKey[key].orEmpty(),
            )
        }.sortedBy { it.version == null }
    }

    private fun coerceManifest(raw: JSONObject): OrbitManifest? {
        val latestVersion = raw.optString("latestVersion", "").trim()
        if (latestVersion.isEmpty()) return null
        return OrbitManifest(
            app = raw.optString("app").takeIf { it.isNotBlank() },
            manifestVersion = raw.optInt("manifestVersion").takeIf {
                raw.has("manifestVersion") && !raw.isNull("manifestVersion")
            },
            latestVersion = latestVersion,
            latestVersionCode = raw.optInt("latestVersionCode").takeIf {
                raw.has("latestVersionCode") && !raw.isNull("latestVersionCode")
            },
            publishedAt = raw.optString("publishedAt").takeIf { it.isNotBlank() },
            apkUrl = raw.optString("apkUrl").takeIf { it.isNotBlank() },
            minSupportedVersion = raw.optString("minSupportedVersion").takeIf { it.isNotBlank() },
            mandatory = raw.optBoolean("mandatory", false),
            news = coerceNews(raw.optJSONArray("news")),
        )
    }

    private fun coerceNews(raw: JSONArray?): List<OrbitNewsItem> {
        if (raw == null) return emptyList()
        val items = mutableListOf<OrbitNewsItem>()
        for (i in 0 until raw.length()) {
            val n = raw.optJSONObject(i) ?: continue
            val id = n.optString("id")
            val title = n.optString("title")
            if (id.isBlank() || title.isBlank()) continue
            items += OrbitNewsItem(
                id = id,
                date = n.optString("date"),
                tag = coerceTag(n.optString("tag")),
                title = title,
                body = n.optString("body"),
                version = n.optString("version").takeIf { it.isNotBlank() },
            )
        }
        return items
    }

    private fun coerceTag(raw: String): OrbitNewsTag = when (raw.lowercase()) {
        "correcao", "correção" -> OrbitNewsTag.CORRECAO
        "aviso" -> OrbitNewsTag.AVISO
        else -> OrbitNewsTag.NOVIDADE
    }
}
