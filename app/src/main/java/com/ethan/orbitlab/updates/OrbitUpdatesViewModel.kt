package com.ethan.orbitlab.updates

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OrbitUpdatesUiState(
    val news: List<OrbitNewsItem> = emptyList(),
    val currentVersion: String = ManifestFetcher.currentAppVersion(),
    val currentVersionCode: Int = ManifestFetcher.currentVersionCode(),
    val latestVersion: String? = null,
    val apkUrl: String? = null,
    val updateAvailable: Boolean = false,
    val mandatory: Boolean = false,
    val loading: Boolean = true,
    val error: Boolean = false,
    val hasUnseen: Boolean = false,
    val canalBeta: Boolean = ManifestFetcher.isCanalBeta(),
    val canal: String = ManifestFetcher.canalDoBuild(),
    val downloading: Boolean = false,
    val downloadProgress: Float = 0f,
)

class OrbitUpdatesViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(OrbitUpdatesUiState())
    val state: StateFlow<OrbitUpdatesUiState> = _state.asStateFlow()

    private var lastFetchAt = 0L
    private var seenSignature: String? = prefs.getString(SEEN_KEY, null)
    private var seenLoaded = true

    init {
        // Cache na hora, depois fresco.
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                ManifestFetcher.readCachedManifest(getApplication())
            }
            if (cached != null) applyManifest(cached, loading = true)
            refresh(force = true)
        }
    }

    fun refresh(force: Boolean = false) {
        if (!force && System.currentTimeMillis() - lastFetchAt < 20_000L) return
        lastFetchAt = System.currentTimeMillis()
        viewModelScope.launch {
            _state.update { it.copy(error = false) }
            try {
                val fresh = withContext(Dispatchers.IO) {
                    ManifestFetcher.fetchManifest(getApplication())
                }
                applyManifest(fresh, loading = false)
            } catch (_: Exception) {
                val cached = withContext(Dispatchers.IO) {
                    ManifestFetcher.readCachedManifest(getApplication())
                }
                if (cached != null) {
                    applyManifest(cached, loading = false)
                } else {
                    _state.update { it.copy(loading = false, error = true) }
                }
            }
        }
    }

    fun onAppForeground() {
        refresh(force = false)
    }

    fun markSeen() {
        val signature = currentSignature() ?: return
        seenSignature = signature
        prefs.edit().putString(SEEN_KEY, signature).apply()
        _state.update { it.copy(hasUnseen = false) }
    }

    fun baixarEInstalar() {
        val url = _state.value.apkUrl ?: return
        if (_state.value.downloading) return
        viewModelScope.launch {
            _state.update { it.copy(downloading = true, downloadProgress = 0f) }
            try {
                withContext(Dispatchers.IO) {
                    ApkInstaller.downloadAndInstallApk(
                        context = getApplication(),
                        url = url,
                        onProgress = { p ->
                            _state.update { it.copy(downloadProgress = p) }
                        },
                    )
                }
            } catch (_: Exception) {
                try {
                    ApkInstaller.openUpdateInBrowser(getApplication(), url)
                } catch (_: Exception) {
                    // Sem plano C — o utilizador tenta de novo.
                }
            } finally {
                _state.update { it.copy(downloading = false, downloadProgress = 0f) }
            }
        }
    }

    private fun applyManifest(manifest: OrbitManifest, loading: Boolean) {
        val installedCode = ManifestFetcher.currentVersionCode()
        val latestCode = manifest.latestVersionCode
        val currentVersion = ManifestFetcher.currentAppVersion()
        val updateAvailable = when {
            latestCode != null -> latestCode > installedCode
            else -> isNewer(manifest.latestVersion, currentVersion)
        }
        val mandatory = updateAvailable && (
            manifest.mandatory ||
                (manifest.minSupportedVersion != null &&
                    compareVersions(currentVersion, manifest.minSupportedVersion) < 0)
            )
        val signature = "${manifest.latestVersion}::${manifest.news.firstOrNull()?.id.orEmpty()}"
        val hasUnseen = seenLoaded && signature != seenSignature

        _state.update {
            it.copy(
                news = manifest.news,
                currentVersion = currentVersion,
                currentVersionCode = installedCode,
                latestVersion = manifest.latestVersion,
                apkUrl = manifest.apkUrl,
                updateAvailable = updateAvailable,
                mandatory = mandatory,
                loading = loading,
                error = false,
                hasUnseen = hasUnseen,
                canalBeta = ManifestFetcher.isCanalBeta(),
                canal = ManifestFetcher.canalDoBuild(),
            )
        }
    }

    private fun currentSignature(): String? {
        val s = _state.value
        val latest = s.latestVersion ?: return null
        val topNews = s.news.firstOrNull()?.id.orEmpty()
        return "$latest::$topNews"
    }

    companion object {
        private const val PREFS = "orbit.updates"
        private const val SEEN_KEY = "seen.v1"
    }
}
