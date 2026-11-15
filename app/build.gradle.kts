plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.gezgin.zikirmatik"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gezgin.zikirmatik"
        minSdk = 26
        targetSdk = 34
        versionCode = 6
        versionName = "2.1"
    }

    signingConfigs {
        create("zikir") {
            storeFile = file("zikirmatik.jks")
            storePassword = "zikirmatik"
            keyAlias = "zikirmatik"
            keyPassword = "zikirmatik"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Sabit anahtar: yeni sürümler eskisinin üstüne güncelleme olarak kurulur.
            signingConfig = signingConfigs.getByName("zikir")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    testImplementation("junit:junit:4.13.2")
}
