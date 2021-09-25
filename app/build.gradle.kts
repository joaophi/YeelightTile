import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "me.pedro.yeelighttile"
        minSdk = 24
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

        val prop = loadProperties("api.properties")
        buildConfigField("String", "HA_URL", "\"${prop.getProperty("HA_URL")}\"")
        buildConfigField("String", "HA_TOKEN", "\"${prop.getProperty("HA_TOKEN")}\"")
        buildConfigField("String", "HA_DOMAIN", "\"${prop.getProperty("HA_DOMAIN")}\"")
        buildConfigField("String", "HA_ENTITY", "\"${prop.getProperty("HA_ENTITY")}\"")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")

    implementation("com.tinder.scarlet:scarlet:0.1.12")
    implementation("com.tinder.scarlet:websocket-okhttp:0.1.12")
    implementation("com.tinder.scarlet:lifecycle-android:0.1.12")
    implementation("com.tinder.scarlet:stream-adapter-coroutines:0.1.12")
    implementation("com.tinder.scarlet:message-adapter-moshi:0.1.12")

    implementation("com.squareup.moshi:moshi-kotlin:1.12.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.12.0")
}