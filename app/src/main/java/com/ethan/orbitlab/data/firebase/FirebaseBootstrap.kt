package com.ethan.orbitlab.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Bootstrap do projeto Firebase `luna-8787d` (mesmo do orbit-mobile).
 * Inicialização manual — não depende do package name bater com google-services.json.
 */
object FirebaseBootstrap {
    const val PROJECT_ID = "luna-8787d"
    const val WEB_CLIENT_ID =
        "1068126871324-pdg1e88q4j69cnmrd8fomntujoc0cm4c.apps.googleusercontent.com"

    fun init(context: Context) {
        if (FirebaseApp.getApps(context).isNotEmpty()) return
        val options = FirebaseOptions.Builder()
            .setProjectId(PROJECT_ID)
            .setApplicationId("1:1068126871324:android:51217dcd0047b3cf3564d9")
            .setApiKey("AIzaSyBIAkgQeiuFx_UwxFPVtT5UOTa-z14hBGs")
            .setStorageBucket("luna-8787d.firebasestorage.app")
            .build()
        FirebaseApp.initializeApp(context.applicationContext, options)
    }
}
