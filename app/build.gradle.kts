import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

fun readEnvFile(): Properties {
    val envFile = rootProject.file(".env")
    val properties = Properties()
    if (envFile.exists()) {
        envFile.inputStream().use(properties::load)
    }
    return properties
}

fun Properties.requiredEnv(name: String): String {
    val value = getProperty(name)?.trim().orEmpty()
    require(value.isNotBlank()) {
        "Missing $name in .env. Copy .env.example to .env and set your Xtream value."
    }
    return value
}

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val env = readEnvFile()

android {
    namespace = "com.popcorn.live"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.popcorn.live"
        minSdk = 23
        targetSdk = 36
        versionCode = 3
        versionName = "0.1.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "XTREAM_BASE_URL", env.requiredEnv("XTREAM_BASE_URL").asBuildConfigString())
        buildConfigField("String", "XTREAM_USERNAME", env.requiredEnv("XTREAM_USERNAME").asBuildConfigString())
        buildConfigField("String", "XTREAM_PASSWORD", env.requiredEnv("XTREAM_PASSWORD").asBuildConfigString())
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.tv.material)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.room.testing)
}
