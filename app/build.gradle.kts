plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.afaq.life"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hassansyrn.afaqlife"
        minSdk = 23
        targetSdk = 36
        versionCode = 5
        versionName = "1.0.4"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
