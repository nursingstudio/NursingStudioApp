plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.nursingstudio"
    compileSdk = 37 // Professional stable version

    defaultConfig {
        applicationId = "com.example.nursingstudio"
        minSdk = 31
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += "lib/x86/libc++_shared.so"
            pickFirsts += "lib/x86_64/libc++_shared.so"
            pickFirsts += "lib/armeabi-v7a/libc++_shared.so"
            pickFirsts += "lib/arm64-v8a/libc++_shared.so"
        }
    }

    signingConfigs {
        create("releaseTest") {
            storeFile = file("D:\\Keys\\nursing_studio.jks")
            storePassword = "Rakshit@3294"
            keyAlias = "nursing_key"
            keyPassword = "Rakshit@3294"
        }
    }

    buildTypes {
        getByName("release") {
            //noinspection AppBundleCredentials
            signingConfig = signingConfigs.getByName("releaseTest")

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // 🚀 CRITICAL FOR 2026 AGP BUILD CONFIG GENERATION
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

ksp {
    arg("correctErrorTypes", "true")
}

dependencies {
    // 1. Android & UI Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.hilt.common)
    implementation(libs.androidx.pdf.viewer.fragment)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.material)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.lottie)
    implementation(libs.coil)

    // 2. Firebase BoM Control
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)

    // 3. Authentication & Security
    implementation(libs.androidx.biometric)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.auth.api.phone)
    implementation(libs.integrity)

    // 4. Lifecycle & Navigation
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.kotlinx.coroutines.play.services)

    // 5. App Utilities
    implementation(libs.app.update)
    implementation(libs.app.update.ktx)
    implementation(libs.ccp)
    implementation(libs.circleimageview)

    // 6. COMPOSE CORE
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)

    // 7. Jetpack DataStore (SharedPreferences ka replacement)
    implementation(libs.androidx.datastore.preferences)

    // 8. Retrofit & OkHttp (2026 Gold Standard Networking Stack)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // 9. Media & PDF Utilities
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.core)
    implementation(libs.ucrop)
    implementation(libs.glide)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.pdf.viewer)

    // 10. Dependency Injection (Hilt + KSP)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // 11. Debug tools
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // 12. Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // 13. Others
    implementation(libs.firebase.appcheck.ktx)
    implementation(libs.google.firebase.appcheck.ktx)

    // 14. Firebase Cloud Core & AI Jetpack Components
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.firestore.ktx)

    // 15. Firestore Engines
    implementation(libs.google.firebase.firestore.ktx)

    // 16. Core Engine Layer for Protected YouTube Streams
    implementation(libs.core.v1210)

    annotationProcessor(libs.compiler)

}