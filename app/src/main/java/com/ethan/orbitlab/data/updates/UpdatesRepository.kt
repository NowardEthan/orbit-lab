package com.ethan.orbitlab.data.updates

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado de updates — espelho do OrbitUpdatesContext do orbit-mobile. */
object UpdatesRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastFetchAt = 0L

    private val _manifest = MutableStateFlow<OrbitManifest?>(null)
    val manifest: StateFlow<OrbitManifest?> = _manifest.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error.asStateFlow()

    private val _seenSignature = MutableStateFlow<String?>(null)
    val seenSignature: StateFlow<String?> = _seenSignature.asStateFlow()

    private val _seenLoaded = MutableStateFlow(false)
    val seenLoaded: StateFlow<Boolean> = _seenLoaded.asStateFlow()

    val currentVersion: String
        get() = ManifestFetcher.currentAppVersion()

    val news: List<OrbitNewsItem>
        get() = _manifest.value?.news.orEmpty()

    val latestVersion: String?
        get() = _manifest.value?.latestVersion

    val apkUrl: String?
        get() = _manifest.value?.apkUrl

    val updateAvailable: Boolean
        get() {
            val manifest = _manifest.value ?: return false
            val installedCode = ManifestFetcher.currentVersionCode()
            val latestCode = manifest.latestVersionCode
            return if (latestCode != null) {
                latestCode > installedCode
            } else {
                isNewer(manifest.latestVersion, currentVersion)
            }
        }

    val mandatory: Boolean
        get() {
            val manifest = _manifest.value ?: return false
            if (!updateAvailable) return false
            if (manifest.mandatory) return true
            val min = manifest.minSupportedVersion ?: return false
            return compareVersions(currentVersion, min) < 0
        }

    val signature: String?
        get() {
            val manifest = _manifest.value ?: return null
            val topNews = manifest.news.firstOrNull()?.id.orEmpty()
            return "${manifest.latestVersion}::$topNews"
        }

    val hasUnseen: Boolean
        get() = _seenLoaded.value && signature != null && signature != _seenSignature.value

    fun init(context: Context) {
        ManifestFetcher.init(context)
        _seenSignature.value = ManifestFetcher.readSeenSignature()
        _seenLoaded.value = true
        scope.launch {
            ManifestFetcher.readCachedManifest()?.let { _manifest.value = it }
            refresh(force = true)
        }
    }

    fun refresh(force: Boolean = false) {
        if (!force && System.currentTimeMillis() - lastFetchAt < 20_000L) return
        lastFetchAt = System.currentTimeMillis()
        scope.launch {
            _error.value = false
            try {
                _manifest.value = ManifestFetcher.fetchManifest()
            } catch (_: Exception) {
                if (_manifest.value == null) {
                    _manifest.value = ManifestFetcher.readCachedManifest()
                    if (_manifest.value == null) _error.value = true
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun markSeen() {
        val sig = signature ?: return
        _seenSignature.value = sig
        ManifestFetcher.writeSeenSignature(sig)
    }

    fun newsSections(): List<OrbitNewsSection> =
        ManifestFetcher.groupNewsByVersion(news)
}
