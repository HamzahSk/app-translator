plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.diffplug.spotless")
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.2.1").editorConfigOverride(mapOf("max_line_length" to "140"))
        trimTrailingWhitespace()
        endWithNewline()
    }
}

android {
    namespace = "com.ervareza.screentranslator"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ervareza.screentranslator"
        minSdk = 26
        targetSdk = 34
        versionCode = 7
        versionName = "1.0.6"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // CI-friendly signing: no release keystore is checked into the repo,
            // so the release APK is signed with the local debug key. For Play Store
            // distribution, replace with a real keystore-backed signingConfig.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // ISSUE-014 FIX: Split APK by CPU ABI (arm64-v8a, armeabi-v7a, x86, x86_64) + universal APK.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val abi = output.filters.find { it.filterType == "ABI" }?.identifier ?: "universal"
            output.outputFileName = "Screen-Translator-v${variant.versionName}-${abi}.apk"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // Coroutines for off-main-thread (lazy/async) heavy initialization
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ML Kit Language Identification
    implementation("com.google.mlkit:language-id:17.0.4")
    
    // Google Play Services (For ModuleInstallClient)
    implementation("com.google.android.gms:play-services-base:18.3.0")

    // ML Kit Text Recognition (OCR) via Google Play Services (Thin APK)
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition-japanese:16.0.1")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition-korean:16.0.1")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition-chinese:16.0.1")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition-devanagari:16.0.1")
    
    // ML Kit Translation
    implementation("com.google.mlkit:translate:17.0.2")

    // Online Translation Mode (OpenAI / Gemini compatible APIs)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
