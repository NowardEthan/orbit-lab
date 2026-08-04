# OrbitLab — regras R8 / ProGuard (Fase 2 do HARDENING.md)
#
# Firestore neste app grava/lê via Map + parsers manuais (toCarteira, etc.),
# NÃO via .toObject(dataClass). Mesmo assim mantemos keep de data.** —
# cinto se alguém voltar a POJO, e evita surpresa em campos só lidos via reflection.

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Modelos / repositórios — finanças, chat, billing, auth…
-keep class com.ethan.orbitlab.data.** { *; }
-keepclassmembers class com.ethan.orbitlab.data.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# Compose — o plugin AGP já injeta boa parte; reforço mínimo
-dontwarn androidx.compose.**
