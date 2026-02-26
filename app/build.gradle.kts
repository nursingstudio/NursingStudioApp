plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.nursingstudio"
    compileSdk = 36 // Professional stable version

    defaultConfig {
        applicationId = "com.example.nursingstudio"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    // Fixed: Dono features ko ek hi block mein aur sahi format mein dala hai
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Standard Android Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)

    // --- FIREBASE (BoM managed) ---
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.appcheck.ktx)
    implementation(platform(libs.firebase.bom.v3490))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.google.firebase.appcheck.ktx)
    implementation(libs.play.services.safetynet)
    // Firebase App Check (Optional but recommended)
    implementation(libs.firebase.appcheck.playintegrity)
    // Firebase Tasks ko Coroutines (await) ke sath chalane ke liye
    implementation(libs.kotlinx.coroutines.play.services)
    // Play Integrity API ke liye
    implementation(libs.integrity)
    // Biometric Authentication Library
    implementation(libs.androidx.biometric)
    // Encrypted Storage ke liye
    implementation(libs.androidx.security.crypto)

    // --- OTHER LIBRARIES ---
    implementation(libs.ccp)
    implementation(libs.circleimageview)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.lottie)

    // Compose
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // ViewModel & LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.fragment.ktx)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}