plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

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
        // Continua a sequência do orbit-mobile (78) — quando o Lab substituir o Expo,
        // o versionCode tem de ser estritamente maior que o instalado.
        versionCode = 79
        versionName = "2.25.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("debugKey") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    // Canais: lab (dev local), stable e beta (mesmos pacotes do Orbit de produção).
    // O Lab local não colide com o Orbit instalado; stable/beta preparam a substituição.
    flavorDimensions += "canal"
    productFlavors {
        create("lab") {
            dimension = "canal"
            applicationId = "com.ethan.orbitlab"
            buildConfigField("String", "ORBIT_CHANNEL", "\"lab\"")
        }
        create("stable") {
            dimension = "canal"
            applicationId = "com.luna.orbitmobile"
            buildConfigField("String", "ORBIT_CHANNEL", "\"stable\"")
        }
        create("beta") {
            dimension = "canal"
            applicationId = "com.luna.orbitmobile.beta"
            versionNameSuffix = "-beta"
            buildConfigField("String", "ORBIT_CHANNEL", "\"beta\"")
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debugKey")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debugKey")
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
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
