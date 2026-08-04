import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

fun loadSecret(name: String, envKey: String? = null): String {
    val local = Properties()
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { local.load(it) }
    }
    local.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

    val envFile = rootProject.file("../core/src/luna-core/.env")
    if (envFile.exists() && envKey != null) {
        envFile.readLines().forEach { line ->
            val t = line.trim()
            if (t.startsWith("#") || !t.contains("=")) return@forEach
            val key = t.substringBefore("=").trim()
            val value = t.substringAfter("=").trim().trim('"')
            if (key == envKey && value.isNotEmpty()) return value
        }
    }
    return ""
}

val openRouterKey = loadSecret("openrouter.api.key", "OPENROUTER_API_KEY")
val openRouterChatModel = loadSecret("openrouter.model.chat", "P0_MODEL_MENOR")
    .ifBlank { "deepseek/deepseek-v4-flash" }
val openRouterVisionModel = loadSecret("openrouter.model.vision", "OPENROUTER_VISION_MODEL")
    .ifBlank { "qwen/qwen3.5-flash-02-23" }
val openRouterVideoModel = loadSecret("openrouter.model.video", "OPENROUTER_VIDEO_MODEL")
    .ifBlank { openRouterVisionModel }
val openRouterSttModel = loadSecret("openrouter.model.stt", "LUNA_STT_MODEL")
    .ifBlank { "openai/whisper-large-v3" }

fun loadLunaApiUrl(): String {
    val local = Properties()
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { local.load(it) }
    }
    local.getProperty("luna.api.url")?.trim()?.takeIf { it.isNotEmpty() }?.let {
        return it.trimEnd('/')
    }

    val mobileEnv = rootProject.file("../orbit-mobile/.env")
    if (mobileEnv.exists()) {
        mobileEnv.readLines().forEach { line ->
            val t = line.trim()
            if (t.startsWith("#") || !t.contains("=")) return@forEach
            val key = t.substringBefore("=").trim()
            val value = t.substringAfter("=").trim().trim('"')
            if (key == "EXPO_PUBLIC_LUNA_API_URL" && value.isNotEmpty()) {
                return value.trimEnd('/')
            }
        }
    }
    return "https://luna-core-production-330b.up.railway.app"
}

val lunaApiUrl = loadLunaApiUrl()

/** Credenciais da keystore de release (local: keystore.properties; CI: env LAB_*). */
fun loadReleaseSigning(): Properties {
    val props = Properties()
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use { props.load(it) }
    }
    fun envOrProp(env: String, prop: String): String? =
        System.getenv(env)?.trim()?.takeIf { it.isNotEmpty() }
            ?: props.getProperty(prop)?.trim()?.takeIf { it.isNotEmpty() }

    val storeFile = envOrProp("LAB_KEYSTORE_FILE", "storeFile")
    val storePassword = envOrProp("LAB_KEYSTORE_PASSWORD", "storePassword")
    val keyAlias = envOrProp("LAB_KEY_ALIAS", "keyAlias")
    val keyPassword = envOrProp("LAB_KEY_PASSWORD", "keyPassword")
    if (
        !storeFile.isNullOrBlank() &&
        !storePassword.isNullOrBlank() &&
        !keyAlias.isNullOrBlank() &&
        !keyPassword.isNullOrBlank()
    ) {
        props.setProperty("storeFile", storeFile)
        props.setProperty("storePassword", storePassword)
        props.setProperty("keyAlias", keyAlias)
        props.setProperty("keyPassword", keyPassword)
    }
    return props
}

val releaseSigningProps = loadReleaseSigning()
val releaseStorePath = releaseSigningProps.getProperty("storeFile")
val releaseStoreFile = releaseStorePath?.let { path ->
    val fromRoot = rootProject.file(path)
    if (fromRoot.exists()) fromRoot else file(path)
}
val hasReleaseKeystore =
    releaseStoreFile?.exists() == true &&
        !releaseSigningProps.getProperty("storePassword").isNullOrBlank() &&
        !releaseSigningProps.getProperty("keyAlias").isNullOrBlank() &&
        !releaseSigningProps.getProperty("keyPassword").isNullOrBlank()

android {
    namespace = "com.ethan.orbitlab"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.ethan.orbitlab"
        minSdk = 24
        targetSdk = 36
        versionCode = 81
        versionName = "0.26.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // OpenRouter: chave vazia por padrão. Só o buildType debug pode embutir
        // (local.properties / luna-core .env). Release força "" — ver HARDENING Fase 5.
        buildConfigField("String", "OPENROUTER_API_KEY", "\"\"")
        buildConfigField("String", "OPENROUTER_MODEL_CHAT", "\"$openRouterChatModel\"")
        buildConfigField("String", "OPENROUTER_MODEL_VISION", "\"$openRouterVisionModel\"")
        buildConfigField("String", "OPENROUTER_MODEL_VIDEO", "\"$openRouterVideoModel\"")
        buildConfigField("String", "OPENROUTER_MODEL_STT", "\"$openRouterSttModel\"")
        buildConfigField("String", "LUNA_API_URL", "\"${lunaApiUrl.replace("\"", "\\\"")}\"")
    }

    signingConfigs {
        create("debugKey") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseSigningProps.getProperty("storePassword")
                keyAlias = releaseSigningProps.getProperty("keyAlias")
                keyPassword = releaseSigningProps.getProperty("keyPassword")
            }
        }
    }

    // Canal lab = o auto-update do OrbitLab. O CI sobe a versão com -PlabVersionCode / -PlabVersionName.
    flavorDimensions += "canal"
    productFlavors {
        create("lab") {
            dimension = "canal"
            applicationId = "com.ethan.orbitlab"
            versionCode = (findProperty("labVersionCode") as String?)?.toIntOrNull() ?: 97
            versionName = (findProperty("labVersionName") as String?) ?: "0.31.1"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debugKey")
            // Fase 5: chave OpenRouter só em debug local — nunca no APK sideload.
            buildConfigField(
                "String",
                "OPENROUTER_API_KEY",
                "\"${openRouterKey.replace("\"", "\\\"")}\"",
            )
        }
        release {
            // Fase 2: R8 ligado. Modelos em data.** têm keep (+ @Keep em finanças/perfil).
            // Firestore do Lab usa Map manual — ver HARDENING.md § Fase 2.
            isMinifyEnabled = true
            isShrinkResources = true
            // Produção sideload: release key quando existir.
            // Sem keystore local → fallback debug (só dev); no CI a Fase 1 exige a release key.
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "labRelease sem keystore de release — assinando com debug. " +
                        "Ver SIGNING.md (Fase 1). No CI isso deve falhar.",
                )
                signingConfigs.getByName("debugKey")
            }
            // Fase 5: força vazio mesmo se o Gradle leu chave do .env local.
            buildConfigField("String", "OPENROUTER_API_KEY", "\"\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.exifinterface)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.crashlytics)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
