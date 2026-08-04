package com.ethan.orbitlab

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.PrefsRepository
import com.ethan.orbitlab.data.UserProfileRepository
import com.ethan.orbitlab.data.captura.CapturaRepository
import com.ethan.orbitlab.data.crash.CrashReporting
import com.ethan.orbitlab.data.financas.FinancasLuzEngine
import com.ethan.orbitlab.data.local.LocationRepository
import com.ethan.orbitlab.data.updates.UpdatesRepository
import com.ethan.orbitlab.data.firebase.FirebaseBootstrap

class OrbitApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        PrefsRepository.init(this)
        FinancasLuzEngine.init(this)
        CapturaRepository.init(this)
        // Firebase (google-services.json e/ou bootstrap manual) antes do Crashlytics.
        FirebaseBootstrap.init(this)
        CrashReporting.init(this)
        AuthRepository.init(this)
        ChatRepository.bindApp(this)
        ChatRepository.init()
        UserProfileRepository.init()
        LocationRepository.init(this)
        UpdatesRepository.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        val maxHeap = Runtime.getRuntime().maxMemory()
        val memoryBytes = (maxHeap * 0.15)
            .toLong()
            .coerceIn(8L * 1024 * 1024, 25L * 1024 * 1024)
            .toInt()
        return ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizeBytes(memoryBytes)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(40L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }
}
