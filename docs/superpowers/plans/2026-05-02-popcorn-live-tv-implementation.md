# Popcorn Live TV Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a fresh Android TV / Firestick APK named Popcorn for live TV playback from one Xtream Codes subscription configured at build time.

**Architecture:** The app is a single native Android module with Kotlin, Compose UI, Media3 playback, Room cache, and a small repository layer between Xtream and the UI. The launch screen reads cached live categories/channels immediately, refreshes from Xtream while open, and plays selected channels full-screen.

**Tech Stack:** Android Gradle Plugin 9.2.0, Gradle 9.4.1, Kotlin 2.3.21, KSP 2.3.7, Compose BOM 2026.04.01, AndroidX TV Material 1.0.1, Media3 1.10.0, Room 2.8.4, OkHttp 5.3.2, kotlinx.serialization 1.11.0.

---

## Source References

- Design spec: `docs/superpowers/specs/2026-05-02-popcorn-live-tv-design.md`
- Visual rules: `DESIGN.md`
- Xtream endpoints: https://github.com/AndreyPavlenko/Fermata/discussions/434
- Compose BOM guidance: https://developer.android.com/develop/ui/compose/bom
- Media3 release notes: https://developer.android.com/jetpack/androidx/releases/media3
- Room release notes: https://developer.android.com/jetpack/androidx/releases/room
- Android Gradle Plugin 9.2 release notes: https://developer.android.com/build/releases/agp-9-2-0-release-notes
- Fire TV ADB guide: https://www.developer.amazon.com/docs/fire-tv/connecting-adb-to-device.html
- Fire TV install guide: https://developer.amazon.com/docs/fire-tv/installing-and-running-your-app.html

## File Structure

- `settings.gradle.kts`: Gradle plugin management and module include.
- `build.gradle.kts`: root plugin aliases.
- `gradle/libs.versions.toml`: pinned dependency versions.
- `gradle/wrapper/gradle-wrapper.properties`: Gradle 9.4.1 wrapper distribution.
- `.gitignore`: Android build outputs, local `.env`, IDE output.
- `.env.example`: safe sample Xtream configuration.
- `app/build.gradle.kts`: Android app configuration, Compose, Room, Media3, `.env` build config fields.
- `app/src/main/AndroidManifest.xml`: Fire TV launcher, internet permission, application declaration.
- `app/src/main/java/com/popcorn/live/PopcornApplication.kt`: application class and app container owner.
- `app/src/main/java/com/popcorn/live/MainActivity.kt`: Compose entry point.
- `app/src/main/java/com/popcorn/live/config/AppConfig.kt`: typed build-time config.
- `app/src/main/java/com/popcorn/live/di/AppContainer.kt`: manual dependency graph.
- `app/src/main/java/com/popcorn/live/xtream/XtreamDtos.kt`: serialization models.
- `app/src/main/java/com/popcorn/live/xtream/XtreamUrlFactory.kt`: endpoint and playback URL construction.
- `app/src/main/java/com/popcorn/live/xtream/XtreamApi.kt`: network interface and OkHttp implementation.
- `app/src/main/java/com/popcorn/live/xtream/XtreamNormalizer.kt`: DTO to domain conversion.
- `app/src/main/java/com/popcorn/live/catalog/LiveModels.kt`: domain models.
- `app/src/main/java/com/popcorn/live/catalog/LiveCatalogRepository.kt`: cache-first refresh orchestration.
- `app/src/main/java/com/popcorn/live/cache/LiveEntities.kt`: Room entities.
- `app/src/main/java/com/popcorn/live/cache/LiveCatalogDao.kt`: Room queries.
- `app/src/main/java/com/popcorn/live/cache/PopcornDatabase.kt`: Room database.
- `app/src/main/java/com/popcorn/live/ui/theme/PopcornTheme.kt`: Neon Velocity theme tokens.
- `app/src/main/java/com/popcorn/live/ui/live/LiveUiState.kt`: screen state.
- `app/src/main/java/com/popcorn/live/ui/live/LiveViewModel.kt`: cache loading, refresh, category/search state.
- `app/src/main/java/com/popcorn/live/ui/live/LiveScreen.kt`: sidebar plus grid UI.
- `app/src/main/java/com/popcorn/live/ui/player/PlaybackUrlFactory.kt`: channel playback URL selection.
- `app/src/main/java/com/popcorn/live/ui/player/PlayerScreen.kt`: Media3 full-screen player.
- `app/src/main/res/drawable/popcorn_logo_foreground.png`: generated logo source asset.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`: adaptive launcher icon.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`: round launcher icon.
- `app/src/test/java/com/popcorn/live/xtream/XtreamUrlFactoryTest.kt`: URL tests.
- `app/src/test/java/com/popcorn/live/xtream/XtreamNormalizerTest.kt`: normalization tests.
- `app/src/test/java/com/popcorn/live/catalog/LiveCatalogRepositoryTest.kt`: refresh and cache behavior tests with fakes.
- `app/src/test/java/com/popcorn/live/ui/player/PlaybackUrlFactoryTest.kt`: playback URL tests.
- `app/src/androidTest/java/com/popcorn/live/cache/LiveCatalogDaoTest.kt`: Room upsert/query test.
- `docs/deployment/firestick.md`: user-facing APK installation guide.
- `scripts/install-firestick.sh`: local install helper.

## Task 1: Scaffold The Clean Android Project

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `app/build.gradle.kts`
- Create: `.gitignore`
- Create: `.env.example`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/styles.xml`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/popcorn/live/PopcornApplication.kt`
- Create: `app/src/main/java/com/popcorn/live/MainActivity.kt`

- [ ] **Step 1: Write the Gradle settings**

Create `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Popcorn"
include(":app")
```

- [ ] **Step 2: Write the root build file**

Create `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 3: Write the version catalog**

Create `gradle/libs.versions.toml`:

```toml
[versions]
agp = "9.2.0"
kotlin = "2.3.21"
ksp = "2.3.7"
composeBom = "2026.04.01"
activityCompose = "1.13.0"
coreKtx = "1.18.0"
lifecycle = "2.10.0"
tvMaterial = "1.0.1"
media3 = "1.10.0"
room = "2.8.4"
okhttp = "5.3.2"
serializationJson = "1.11.0"
coroutines = "1.10.2"
junit = "4.13.2"
androidxTestJunit = "1.3.0"
robolectric = "4.16.1"
turbine = "1.2.1"

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }

[libraries]
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-foundation = { module = "androidx.compose.foundation:foundation" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-tv-material = { module = "androidx.tv:tv-material", version.ref = "tvMaterial" }
media3-exoplayer = { module = "androidx.media3:media3-exoplayer", version.ref = "media3" }
media3-exoplayer-hls = { module = "androidx.media3:media3-exoplayer-hls", version.ref = "media3" }
media3-ui = { module = "androidx.media3:media3-ui", version.ref = "media3" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
room-testing = { module = "androidx.room:room-testing", version.ref = "room" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
mockwebserver = { module = "com.squareup.okhttp3:mockwebserver3", version.ref = "okhttp" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serializationJson" }
junit = { module = "junit:junit", version.ref = "junit" }
androidx-test-junit = { module = "androidx.test.ext:junit", version.ref = "androidxTestJunit" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
```

- [ ] **Step 4: Add Gradle wrapper metadata**

Create `gradle/wrapper/gradle-wrapper.properties`:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

Generate the wrapper jar before the first build:

```bash
gradle wrapper --gradle-version 9.4.1
```

Expected: `gradle/wrapper/gradle-wrapper.jar` and executable `gradlew` exist.

- [ ] **Step 5: Add ignore rules and sample environment**

Create `.gitignore`:

```gitignore
.gradle/
.idea/
*.iml
build/
local.properties
.env
.DS_Store
app/build/
.superpowers/
```

Create `.env.example`:

```env
XTREAM_BASE_URL=https://example.com:8080
XTREAM_USERNAME=username
XTREAM_PASSWORD=password
```

- [ ] **Step 6: Write the Android app build file**

Create `app/build.gradle.kts`:

```kotlin
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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

val env = readEnvFile()

android {
    namespace = "com.popcorn.live"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.popcorn.live"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "XTREAM_BASE_URL", "\"${env.requiredEnv("XTREAM_BASE_URL")}\"")
        buildConfigField("String", "XTREAM_USERNAME", "\"${env.requiredEnv("XTREAM_USERNAME")}\"")
        buildConfigField("String", "XTREAM_PASSWORD", "\"${env.requiredEnv("XTREAM_PASSWORD")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
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
```

- [ ] **Step 7: Add base Android resources**

Create `app/src/main/res/values/colors.xml`:

```xml
<resources>
    <color name="obsidian">#0C0E12</color>
    <color name="electric_cyan">#00E5FF</color>
</resources>
```

Create `app/src/main/res/values/styles.xml`:

```xml
<resources>
    <style name="Theme.Popcorn" parent="android:style/Theme.Material.NoActionBar">
        <item name="android:windowNoTitle">true</item>
        <item name="android:windowActionBar">false</item>
        <item name="android:windowBackground">@color/obsidian</item>
        <item name="android:fontFamily">sans</item>
    </style>
</resources>
```

- [ ] **Step 8: Add manifest and first Activity**

Create `app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-feature android:name="android.software.leanback" android:required="false" />
    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />

    <application
        android:name=".PopcornApplication"
        android:allowBackup="false"
        android:label="Popcorn"
        android:supportsRtl="true"
        android:theme="@style/Theme.Popcorn">
        <activity
            android:name=".MainActivity"
            android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenLayout|screenSize|smallestScreenSize"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

Create `app/src/main/java/com/popcorn/live/MainActivity.kt`:

```kotlin
package com.popcorn.live

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Popcorn")
            }
        }
    }
}
```

Create `app/src/main/java/com/popcorn/live/PopcornApplication.kt`:

```kotlin
package com.popcorn.live

import android.app.Application

class PopcornApplication : Application()
```

- [ ] **Step 9: Run the first build**

Create a local `.env` from `.env.example` with safe test strings:

```bash
cp .env.example .env
./gradlew :app:assembleDebug
```

Expected: build creates `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 10: Commit the scaffold**

```bash
git add .gitignore .env.example settings.gradle.kts build.gradle.kts gradle app
git commit -m "chore: scaffold android tv app"
```

## Task 2: Add Configuration And Xtream URL Construction

**Files:**
- Create: `app/src/main/java/com/popcorn/live/config/AppConfig.kt`
- Create: `app/src/main/java/com/popcorn/live/xtream/XtreamUrlFactory.kt`
- Create: `app/src/test/java/com/popcorn/live/xtream/XtreamUrlFactoryTest.kt`
- Modify: `app/src/main/java/com/popcorn/live/MainActivity.kt`

- [ ] **Step 1: Write failing URL tests**

Create `app/src/test/java/com/popcorn/live/xtream/XtreamUrlFactoryTest.kt`:

```kotlin
package com.popcorn.live.xtream

import org.junit.Assert.assertEquals
import org.junit.Test

class XtreamUrlFactoryTest {
    private val factory = XtreamUrlFactory(
        baseUrl = "https://iptv.example.com:8080/",
        username = "Mike",
        password = "1234",
    )

    @Test
    fun accountInfoUrlUsesPlayerApiWithoutAction() {
        assertEquals(
            "https://iptv.example.com:8080/player_api.php?username=Mike&password=1234",
            factory.accountInfoUrl(),
        )
    }

    @Test
    fun liveCategoriesUrlUsesGetLiveCategoriesAction() {
        assertEquals(
            "https://iptv.example.com:8080/player_api.php?username=Mike&password=1234&action=get_live_categories",
            factory.liveCategoriesUrl(),
        )
    }

    @Test
    fun liveStreamsUrlCanTargetOneCategory() {
        assertEquals(
            "https://iptv.example.com:8080/player_api.php?username=Mike&password=1234&action=get_live_streams&category_id=25",
            factory.liveStreamsUrl(categoryId = "25"),
        )
    }

    @Test
    fun playbackUrlsUseLivePathAndStreamId() {
        assertEquals(
            "https://iptv.example.com:8080/live/Mike/1234/55555.m3u8",
            factory.hlsPlaybackUrl(streamId = 55555),
        )
        assertEquals(
            "https://iptv.example.com:8080/live/Mike/1234/55555.ts",
            factory.tsPlaybackUrl(streamId = 55555),
        )
    }
}
```

- [ ] **Step 2: Run the failing test**

```bash
./gradlew :app:testDebugUnitTest --tests "com.popcorn.live.xtream.XtreamUrlFactoryTest"
```

Expected: FAIL because `XtreamUrlFactory` does not exist.

- [ ] **Step 3: Implement config and URL factory**

Create `app/src/main/java/com/popcorn/live/config/AppConfig.kt`:

```kotlin
package com.popcorn.live.config

import com.popcorn.live.BuildConfig

data class AppConfig(
    val xtreamBaseUrl: String,
    val xtreamUsername: String,
    val xtreamPassword: String,
) {
    companion object {
        fun fromBuildConfig(): AppConfig = AppConfig(
            xtreamBaseUrl = BuildConfig.XTREAM_BASE_URL,
            xtreamUsername = BuildConfig.XTREAM_USERNAME,
            xtreamPassword = BuildConfig.XTREAM_PASSWORD,
        )
    }
}
```

Create `app/src/main/java/com/popcorn/live/xtream/XtreamUrlFactory.kt`:

```kotlin
package com.popcorn.live.xtream

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class XtreamUrlFactory(
    baseUrl: String,
    private val username: String,
    private val password: String,
) {
    private val cleanBaseUrl = baseUrl.trim().trimEnd('/')
    private val encodedUsername = username.urlEncode()
    private val encodedPassword = password.urlEncode()

    fun accountInfoUrl(): String =
        "$cleanBaseUrl/player_api.php?username=$encodedUsername&password=$encodedPassword"

    fun liveCategoriesUrl(): String =
        "${accountInfoUrl()}&action=get_live_categories"

    fun liveStreamsUrl(categoryId: String? = null): String =
        buildString {
            append(accountInfoUrl())
            append("&action=get_live_streams")
            if (!categoryId.isNullOrBlank()) {
                append("&category_id=")
                append(categoryId.urlEncode())
            }
        }

    fun hlsPlaybackUrl(streamId: Int): String =
        "$cleanBaseUrl/live/$encodedUsername/$encodedPassword/$streamId.m3u8"

    fun tsPlaybackUrl(streamId: Int): String =
        "$cleanBaseUrl/live/$encodedUsername/$encodedPassword/$streamId.ts"

    private fun String.urlEncode(): String =
        URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
}
```

- [ ] **Step 4: Run the URL tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.popcorn.live.xtream.XtreamUrlFactoryTest"
```

Expected: PASS.

- [ ] **Step 5: Commit URL construction**

```bash
git add app/src/main/java/com/popcorn/live/config/AppConfig.kt app/src/main/java/com/popcorn/live/xtream/XtreamUrlFactory.kt app/src/test/java/com/popcorn/live/xtream/XtreamUrlFactoryTest.kt
git commit -m "feat: add xtream url construction"
```

## Task 3: Add Xtream API Models, Client, And Normalizers

**Files:**
- Create: `app/src/main/java/com/popcorn/live/catalog/LiveModels.kt`
- Create: `app/src/main/java/com/popcorn/live/xtream/XtreamDtos.kt`
- Create: `app/src/main/java/com/popcorn/live/xtream/XtreamApi.kt`
- Create: `app/src/main/java/com/popcorn/live/xtream/XtreamNormalizer.kt`
- Create: `app/src/test/java/com/popcorn/live/xtream/XtreamNormalizerTest.kt`

- [ ] **Step 1: Write normalizer tests**

Create `app/src/test/java/com/popcorn/live/xtream/XtreamNormalizerTest.kt`:

```kotlin
package com.popcorn.live.xtream

import org.junit.Assert.assertEquals
import org.junit.Test

class XtreamNormalizerTest {
    @Test
    fun categoryNamesAreTrimmedAndSortedByApiOrder() {
        val categories = listOf(
            XtreamLiveCategoryDto(categoryId = "2", categoryName = " Sports ", parentId = 0),
            XtreamLiveCategoryDto(categoryId = "1", categoryName = "News", parentId = 0),
        )

        val normalized = XtreamNormalizer.categories(categories)

        assertEquals("2", normalized[0].id)
        assertEquals("Sports", normalized[0].name)
        assertEquals(0, normalized[0].sortOrder)
        assertEquals("1", normalized[1].id)
        assertEquals("News", normalized[1].name)
        assertEquals(1, normalized[1].sortOrder)
    }

    @Test
    fun liveChannelsKeepStreamIdCategoryAndIcon() {
        val channels = listOf(
            XtreamLiveStreamDto(
                num = 1,
                name = " France 24 ",
                streamType = "live",
                streamId = 36475,
                streamIcon = "https://example.com/france24.png",
                epgChannelId = "france24.fr",
                added = "1700000000",
                categoryId = "10",
                customSid = null,
                tvArchive = 0,
                directSource = "",
                tvArchiveDuration = 0,
            ),
        )

        val normalized = XtreamNormalizer.channels(channels)

        assertEquals(36475, normalized.single().streamId)
        assertEquals("France 24", normalized.single().name)
        assertEquals("10", normalized.single().categoryId)
        assertEquals("https://example.com/france24.png", normalized.single().streamIcon)
    }
}
```

- [ ] **Step 2: Run the failing normalizer test**

```bash
./gradlew :app:testDebugUnitTest --tests "com.popcorn.live.xtream.XtreamNormalizerTest"
```

Expected: FAIL because DTO and normalizer classes do not exist.

- [ ] **Step 3: Implement domain models and DTOs**

Create `app/src/main/java/com/popcorn/live/catalog/LiveModels.kt`:

```kotlin
package com.popcorn.live.catalog

data class LiveCategory(
    val id: String,
    val name: String,
    val sortOrder: Int,
)

data class LiveChannel(
    val streamId: Int,
    val name: String,
    val categoryId: String,
    val streamIcon: String?,
    val streamType: String,
    val added: String?,
    val sortOrder: Int,
)
```

Create `app/src/main/java/com/popcorn/live/xtream/XtreamDtos.kt`:

```kotlin
package com.popcorn.live.xtream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XtreamAccountResponseDto(
    @SerialName("user_info") val userInfo: XtreamUserInfoDto? = null,
)

@Serializable
data class XtreamUserInfoDto(
    val auth: Int = 0,
    val status: String = "",
    val username: String = "",
)

@Serializable
data class XtreamLiveCategoryDto(
    @SerialName("category_id") val categoryId: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("parent_id") val parentId: Int? = null,
)

@Serializable
data class XtreamLiveStreamDto(
    val num: Int? = null,
    val name: String,
    @SerialName("stream_type") val streamType: String = "live",
    @SerialName("stream_id") val streamId: Int,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    val added: String? = null,
    @SerialName("category_id") val categoryId: String,
    @SerialName("custom_sid") val customSid: String? = null,
    @SerialName("tv_archive") val tvArchive: Int? = null,
    @SerialName("direct_source") val directSource: String? = null,
    @SerialName("tv_archive_duration") val tvArchiveDuration: Int? = null,
)
```

- [ ] **Step 4: Implement normalizer and network API**

Create `app/src/main/java/com/popcorn/live/xtream/XtreamNormalizer.kt`:

```kotlin
package com.popcorn.live.xtream

import com.popcorn.live.catalog.LiveCategory
import com.popcorn.live.catalog.LiveChannel

object XtreamNormalizer {
    fun categories(input: List<XtreamLiveCategoryDto>): List<LiveCategory> =
        input.mapIndexed { index, dto ->
            LiveCategory(
                id = dto.categoryId,
                name = dto.categoryName.trim(),
                sortOrder = index,
            )
        }

    fun channels(input: List<XtreamLiveStreamDto>): List<LiveChannel> =
        input.mapIndexed { index, dto ->
            LiveChannel(
                streamId = dto.streamId,
                name = dto.name.trim(),
                categoryId = dto.categoryId,
                streamIcon = dto.streamIcon?.takeIf { it.isNotBlank() },
                streamType = dto.streamType,
                added = dto.added,
                sortOrder = index,
            )
        }
}
```

Create `app/src/main/java/com/popcorn/live/xtream/XtreamApi.kt`:

```kotlin
package com.popcorn.live.xtream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

interface XtreamApi {
    suspend fun account(): XtreamAccountResponseDto
    suspend fun liveCategories(): List<XtreamLiveCategoryDto>
    suspend fun liveStreams(categoryId: String? = null): List<XtreamLiveStreamDto>
}

class OkHttpXtreamApi(
    private val urlFactory: XtreamUrlFactory,
    private val client: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : XtreamApi {
    override suspend fun account(): XtreamAccountResponseDto =
        getJson(urlFactory.accountInfoUrl())

    override suspend fun liveCategories(): List<XtreamLiveCategoryDto> =
        getJson(urlFactory.liveCategoriesUrl())

    override suspend fun liveStreams(categoryId: String?): List<XtreamLiveStreamDto> =
        getJson(urlFactory.liveStreamsUrl(categoryId))

    private suspend inline fun <reified T> getJson(url: String): T = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Xtream request failed with HTTP ${response.code}")
            }
            val body = response.body.string()
            json.decodeFromString<T>(body)
        }
    }
}
```

- [ ] **Step 5: Run normalizer tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.popcorn.live.xtream.XtreamNormalizerTest"
```

Expected: PASS.

- [ ] **Step 6: Commit Xtream API layer**

```bash
git add app/src/main/java/com/popcorn/live/catalog/LiveModels.kt app/src/main/java/com/popcorn/live/xtream app/src/test/java/com/popcorn/live/xtream/XtreamNormalizerTest.kt
git commit -m "feat: add xtream api layer"
```

## Task 4: Add Room Cache And Repository Refresh

**Files:**
- Create: `app/src/main/java/com/popcorn/live/cache/LiveEntities.kt`
- Create: `app/src/main/java/com/popcorn/live/cache/LiveCatalogDao.kt`
- Create: `app/src/main/java/com/popcorn/live/cache/PopcornDatabase.kt`
- Create: `app/src/main/java/com/popcorn/live/catalog/LiveCatalogRepository.kt`
- Create: `app/src/test/java/com/popcorn/live/catalog/LiveCatalogRepositoryTest.kt`
- Create: `app/src/androidTest/java/com/popcorn/live/cache/LiveCatalogDaoTest.kt`

- [ ] **Step 1: Write repository tests with fakes**

Create `app/src/test/java/com/popcorn/live/catalog/LiveCatalogRepositoryTest.kt`:

```kotlin
package com.popcorn.live.catalog

import com.popcorn.live.xtream.XtreamApi
import com.popcorn.live.xtream.XtreamLiveCategoryDto
import com.popcorn.live.xtream.XtreamLiveStreamDto
import com.popcorn.live.xtream.XtreamAccountResponseDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveCatalogRepositoryTest {
    @Test
    fun refreshStoresNormalizedCategoriesAndChannels() = runTest {
        val store = FakeLiveCatalogStore()
        val api = FakeXtreamApi()
        val repository = LiveCatalogRepository(api, store)

        val result = repository.refresh()

        assertTrue(result is RefreshResult.Success)
        assertEquals("News", store.categories.value.single().name)
        assertEquals("France 24", store.channels.value.single().name)
    }

    @Test
    fun refreshFailurePreservesExistingCache() = runTest {
        val store = FakeLiveCatalogStore(
            initialCategories = listOf(LiveCategory("cached", "Cached", 0)),
            initialChannels = listOf(LiveChannel(1, "Cached Channel", "cached", null, "live", null, 0)),
        )
        val repository = LiveCatalogRepository(FailingXtreamApi(), store)

        val result = repository.refresh()

        assertTrue(result is RefreshResult.Failure)
        assertEquals("Cached", store.categories.value.single().name)
        assertEquals("Cached Channel", store.channels.value.single().name)
    }
}

private class FakeXtreamApi : XtreamApi {
    override suspend fun account() = XtreamAccountResponseDto()
    override suspend fun liveCategories() = listOf(
        XtreamLiveCategoryDto(categoryId = "10", categoryName = "News"),
    )
    override suspend fun liveStreams(categoryId: String?) = listOf(
        XtreamLiveStreamDto(name = "France 24", streamId = 36475, categoryId = "10"),
    )
}

private class FailingXtreamApi : XtreamApi {
    override suspend fun account() = error("network")
    override suspend fun liveCategories(): List<XtreamLiveCategoryDto> = error("network")
    override suspend fun liveStreams(categoryId: String?): List<XtreamLiveStreamDto> = error("network")
}

private class FakeLiveCatalogStore(
    initialCategories: List<LiveCategory> = emptyList(),
    initialChannels: List<LiveChannel> = emptyList(),
) : LiveCatalogStore {
    override val categories = MutableStateFlow(initialCategories)
    override val channels = MutableStateFlow(initialChannels)
    override val metadata = MutableStateFlow(CatalogMetadata(lastSuccessfulRefreshAt = null, lastRefreshError = null))

    override suspend fun replaceCatalog(categories: List<LiveCategory>, channels: List<LiveChannel>, refreshedAtMillis: Long) {
        this.categories.value = categories
        this.channels.value = channels
        metadata.value = CatalogMetadata(lastSuccessfulRefreshAt = refreshedAtMillis, lastRefreshError = null)
    }

    override suspend fun recordRefreshError(message: String) {
        metadata.value = metadata.value.copy(lastRefreshError = message)
    }
}
```

- [ ] **Step 2: Run failing repository tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.popcorn.live.catalog.LiveCatalogRepositoryTest"
```

Expected: FAIL because repository and store interfaces do not exist.

- [ ] **Step 3: Implement repository interfaces and refresh result**

Create `app/src/main/java/com/popcorn/live/catalog/LiveCatalogRepository.kt`:

```kotlin
package com.popcorn.live.catalog

import com.popcorn.live.xtream.XtreamApi
import com.popcorn.live.xtream.XtreamNormalizer
import kotlinx.coroutines.flow.StateFlow

data class CatalogMetadata(
    val lastSuccessfulRefreshAt: Long?,
    val lastRefreshError: String?,
)

sealed interface RefreshResult {
    data class Success(val refreshedAtMillis: Long) : RefreshResult
    data class Failure(val message: String) : RefreshResult
}

interface LiveCatalogStore {
    val categories: StateFlow<List<LiveCategory>>
    val channels: StateFlow<List<LiveChannel>>
    val metadata: StateFlow<CatalogMetadata>
    suspend fun replaceCatalog(categories: List<LiveCategory>, channels: List<LiveChannel>, refreshedAtMillis: Long)
    suspend fun recordRefreshError(message: String)
}

class LiveCatalogRepository(
    private val api: XtreamApi,
    private val store: LiveCatalogStore,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    val categories: StateFlow<List<LiveCategory>> = store.categories
    val channels: StateFlow<List<LiveChannel>> = store.channels
    val metadata: StateFlow<CatalogMetadata> = store.metadata

    suspend fun refresh(): RefreshResult = runCatching {
        val categories = XtreamNormalizer.categories(api.liveCategories())
        val channels = XtreamNormalizer.channels(api.liveStreams())
        val refreshedAt = clock()
        store.replaceCatalog(categories, channels, refreshedAt)
        RefreshResult.Success(refreshedAt)
    }.getOrElse { throwable ->
        val message = throwable.message ?: "Xtream refresh failed"
        store.recordRefreshError(message)
        RefreshResult.Failure(message)
    }
}
```

- [ ] **Step 4: Implement Room entities and DAO**

Create `app/src/main/java/com/popcorn/live/cache/LiveEntities.kt`:

```kotlin
package com.popcorn.live.cache

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.popcorn.live.catalog.CatalogMetadata
import com.popcorn.live.catalog.LiveCategory
import com.popcorn.live.catalog.LiveChannel

@Entity(tableName = "live_categories")
data class LiveCategoryEntity(
    @PrimaryKey val categoryId: String,
    val name: String,
    val sortOrder: Int,
    val lastUpdatedAt: Long,
)

@Entity(tableName = "live_channels")
data class LiveChannelEntity(
    @PrimaryKey val streamId: Int,
    val name: String,
    val categoryId: String,
    val streamIcon: String?,
    val streamType: String,
    val added: String?,
    val sortOrder: Int,
    val lastUpdatedAt: Long,
)

@Entity(tableName = "catalog_metadata")
data class CatalogMetadataEntity(
    @PrimaryKey val id: String = "catalog",
    val lastSuccessfulRefreshAt: Long?,
    val lastRefreshError: String?,
)

fun LiveCategory.toEntity(lastUpdatedAt: Long) = LiveCategoryEntity(id, name, sortOrder, lastUpdatedAt)
fun LiveChannel.toEntity(lastUpdatedAt: Long) = LiveChannelEntity(streamId, name, categoryId, streamIcon, streamType, added, sortOrder, lastUpdatedAt)
fun LiveCategoryEntity.toDomain() = LiveCategory(categoryId, name, sortOrder)
fun LiveChannelEntity.toDomain() = LiveChannel(streamId, name, categoryId, streamIcon, streamType, added, sortOrder)
fun CatalogMetadataEntity.toDomain() = CatalogMetadata(lastSuccessfulRefreshAt, lastRefreshError)
```

Create `app/src/main/java/com/popcorn/live/cache/LiveCatalogDao.kt`:

```kotlin
package com.popcorn.live.cache

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveCatalogDao {
    @Query("SELECT * FROM live_categories ORDER BY sortOrder ASC, name ASC")
    fun observeCategories(): Flow<List<LiveCategoryEntity>>

    @Query("SELECT * FROM live_channels ORDER BY sortOrder ASC, name ASC")
    fun observeChannels(): Flow<List<LiveChannelEntity>>

    @Query("SELECT * FROM catalog_metadata WHERE id = 'catalog'")
    fun observeMetadata(): Flow<CatalogMetadataEntity?>

    @Upsert
    suspend fun upsertCategories(categories: List<LiveCategoryEntity>)

    @Upsert
    suspend fun upsertChannels(channels: List<LiveChannelEntity>)

    @Upsert
    suspend fun upsertMetadata(metadata: CatalogMetadataEntity)

    @Query("DELETE FROM live_categories")
    suspend fun deleteCategories()

    @Query("DELETE FROM live_channels")
    suspend fun deleteChannels()

    @Transaction
    suspend fun replaceCatalog(categories: List<LiveCategoryEntity>, channels: List<LiveChannelEntity>, metadata: CatalogMetadataEntity) {
        deleteCategories()
        deleteChannels()
        upsertCategories(categories)
        upsertChannels(channels)
        upsertMetadata(metadata)
    }
}
```

Create `app/src/main/java/com/popcorn/live/cache/PopcornDatabase.kt`:

```kotlin
package com.popcorn.live.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LiveCategoryEntity::class, LiveChannelEntity::class, CatalogMetadataEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PopcornDatabase : RoomDatabase() {
    abstract fun liveCatalogDao(): LiveCatalogDao

    companion object {
        fun create(context: Context): PopcornDatabase =
            Room.databaseBuilder(context, PopcornDatabase::class.java, "popcorn.db")
                .build()
    }
}
```

- [ ] **Step 5: Add Room-backed store**

Append to `app/src/main/java/com/popcorn/live/cache/PopcornDatabase.kt`:

```kotlin
class RoomLiveCatalogStore(
    private val dao: LiveCatalogDao,
) : com.popcorn.live.catalog.LiveCatalogStore {
    override val categories = dao.observeCategories()
        .map { rows -> rows.map(LiveCategoryEntity::toDomain) }
        .stateInCache(emptyList())

    override val channels = dao.observeChannels()
        .map { rows -> rows.map(LiveChannelEntity::toDomain) }
        .stateInCache(emptyList())

    override val metadata = dao.observeMetadata()
        .map { row -> row?.toDomain() ?: com.popcorn.live.catalog.CatalogMetadata(null, null) }
        .stateInCache(com.popcorn.live.catalog.CatalogMetadata(null, null))

    override suspend fun replaceCatalog(categories: List<com.popcorn.live.catalog.LiveCategory>, channels: List<com.popcorn.live.catalog.LiveChannel>, refreshedAtMillis: Long) {
        dao.replaceCatalog(
            categories = categories.map { it.toEntity(refreshedAtMillis) },
            channels = channels.map { it.toEntity(refreshedAtMillis) },
            metadata = CatalogMetadataEntity(lastSuccessfulRefreshAt = refreshedAtMillis, lastRefreshError = null),
        )
    }

    override suspend fun recordRefreshError(message: String) {
        dao.upsertMetadata(CatalogMetadataEntity(lastSuccessfulRefreshAt = metadata.value.lastSuccessfulRefreshAt, lastRefreshError = message))
    }
}
```

Then add the required imports at the top of `PopcornDatabase.kt`:

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
```

Add this private helper in the same file:

```kotlin
private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private fun <T> Flow<T>.stateInCache(initial: T) =
    stateIn(cacheScope, SharingStarted.Eagerly, initial)
```

- [ ] **Step 6: Run repository tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.popcorn.live.catalog.LiveCatalogRepositoryTest"
```

Expected: PASS.

- [ ] **Step 7: Add Room DAO instrumentation test**

Create `app/src/androidTest/java/com/popcorn/live/cache/LiveCatalogDaoTest.kt`:

```kotlin
package com.popcorn.live.cache

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveCatalogDaoTest {
    @Test
    fun replaceCatalogStoresCategoriesChannelsAndMetadata() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PopcornDatabase::class.java,
        ).build()

        db.liveCatalogDao().replaceCatalog(
            categories = listOf(LiveCategoryEntity("10", "News", 0, 1000)),
            channels = listOf(LiveChannelEntity(36475, "France 24", "10", null, "live", null, 0, 1000)),
            metadata = CatalogMetadataEntity(lastSuccessfulRefreshAt = 1000, lastRefreshError = null),
        )

        assertEquals("News", db.liveCatalogDao().observeCategories().first().single().name)
        assertEquals("France 24", db.liveCatalogDao().observeChannels().first().single().name)
        assertEquals(1000, db.liveCatalogDao().observeMetadata().first()?.lastSuccessfulRefreshAt)
        db.close()
    }
}
```

Run when an emulator or Firestick is connected:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Expected: PASS on the connected Android device.

- [ ] **Step 8: Commit cache and repository**

```bash
git add app/src/main/java/com/popcorn/live/cache app/src/main/java/com/popcorn/live/catalog app/src/test/java/com/popcorn/live/catalog app/src/androidTest/java/com/popcorn/live/cache
git commit -m "feat: add live catalog cache"
```

## Task 5: Wire Dependencies And Live ViewModel

**Files:**
- Modify: `app/src/main/java/com/popcorn/live/PopcornApplication.kt`
- Create: `app/src/main/java/com/popcorn/live/di/AppContainer.kt`
- Create: `app/src/main/java/com/popcorn/live/ui/live/LiveUiState.kt`
- Create: `app/src/main/java/com/popcorn/live/ui/live/LiveViewModel.kt`
- Create: `app/src/test/java/com/popcorn/live/ui/live/LiveViewModelTest.kt`
- Modify: `app/src/main/java/com/popcorn/live/MainActivity.kt`

- [ ] **Step 1: Add dependency container**

Replace `app/src/main/java/com/popcorn/live/PopcornApplication.kt` with:

```kotlin
package com.popcorn.live

import android.app.Application
import com.popcorn.live.di.AppContainer

class PopcornApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

Create `app/src/main/java/com/popcorn/live/di/AppContainer.kt`:

```kotlin
package com.popcorn.live.di

import android.content.Context
import com.popcorn.live.cache.PopcornDatabase
import com.popcorn.live.cache.RoomLiveCatalogStore
import com.popcorn.live.catalog.LiveCatalogRepository
import com.popcorn.live.config.AppConfig
import com.popcorn.live.xtream.OkHttpXtreamApi
import com.popcorn.live.xtream.XtreamUrlFactory
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class AppContainer(context: Context) {
    private val appConfig = AppConfig.fromBuildConfig()
    private val database = PopcornDatabase.create(context)
    private val urlFactory = XtreamUrlFactory(
        baseUrl = appConfig.xtreamBaseUrl,
        username = appConfig.xtreamUsername,
        password = appConfig.xtreamPassword,
    )
    private val httpClient = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }
    private val xtreamApi = OkHttpXtreamApi(urlFactory, httpClient, json)
    val liveCatalogRepository = LiveCatalogRepository(
        api = xtreamApi,
        store = RoomLiveCatalogStore(database.liveCatalogDao()),
    )
    val xtreamUrlFactory: XtreamUrlFactory = urlFactory
}
```

- [ ] **Step 2: Write ViewModel tests**

Create `app/src/test/java/com/popcorn/live/ui/live/LiveViewModelTest.kt`:

```kotlin
package com.popcorn.live.ui.live

import app.cash.turbine.test
import com.popcorn.live.catalog.CatalogMetadata
import com.popcorn.live.catalog.LiveCategory
import com.popcorn.live.catalog.LiveChannel
import com.popcorn.live.catalog.LiveCatalogRepository
import com.popcorn.live.catalog.LiveCatalogStore
import com.popcorn.live.catalog.RefreshResult
import com.popcorn.live.xtream.XtreamApi
import com.popcorn.live.xtream.XtreamAccountResponseDto
import com.popcorn.live.xtream.XtreamLiveCategoryDto
import com.popcorn.live.xtream.XtreamLiveStreamDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveViewModelTest {
    @Test
    fun selectedCategoryDefaultsToFirstCachedCategory() = runTest {
        val repository = repositoryWith(
            categories = listOf(LiveCategory("news", "News", 0)),
            channels = listOf(LiveChannel(1, "France 24", "news", null, "live", null, 0)),
        )
        val viewModel = LiveViewModel(repository, StandardTestDispatcher(testScheduler))

        viewModel.uiState.test {
            assertEquals("news", awaitItem().selectedCategoryId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchFiltersChannelsAcrossCategories() = runTest {
        val repository = repositoryWith(
            categories = listOf(LiveCategory("news", "News", 0), LiveCategory("sports", "Sports", 1)),
            channels = listOf(
                LiveChannel(1, "France 24", "news", null, "live", null, 0),
                LiveChannel(2, "Eurosport", "sports", null, "live", null, 1),
            ),
        )
        val viewModel = LiveViewModel(repository, StandardTestDispatcher(testScheduler))

        viewModel.onSearchChanged("euro")

        viewModel.uiState.test {
            assertEquals(listOf("Eurosport"), awaitItem().visibleChannels.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private fun repositoryWith(categories: List<LiveCategory>, channels: List<LiveChannel>): LiveCatalogRepository {
    val store = object : LiveCatalogStore {
        override val categories = MutableStateFlow(categories)
        override val channels = MutableStateFlow(channels)
        override val metadata = MutableStateFlow(CatalogMetadata(null, null))
        override suspend fun replaceCatalog(categories: List<LiveCategory>, channels: List<LiveChannel>, refreshedAtMillis: Long) = Unit
        override suspend fun recordRefreshError(message: String) = Unit
    }
    val api = object : XtreamApi {
        override suspend fun account(): XtreamAccountResponseDto = XtreamAccountResponseDto()
        override suspend fun liveCategories(): List<XtreamLiveCategoryDto> = emptyList()
        override suspend fun liveStreams(categoryId: String?): List<XtreamLiveStreamDto> = emptyList()
    }
    return LiveCatalogRepository(api, store)
}
```

- [ ] **Step 3: Run failing ViewModel tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.popcorn.live.ui.live.LiveViewModelTest"
```

Expected: FAIL because `LiveViewModel` and `LiveUiState` do not exist.

- [ ] **Step 4: Implement UI state and ViewModel**

Create `app/src/main/java/com/popcorn/live/ui/live/LiveUiState.kt`:

```kotlin
package com.popcorn.live.ui.live

import com.popcorn.live.catalog.CatalogMetadata
import com.popcorn.live.catalog.LiveCategory
import com.popcorn.live.catalog.LiveChannel

data class LiveUiState(
    val categories: List<LiveCategory> = emptyList(),
    val visibleChannels: List<LiveChannel> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val metadata: CatalogMetadata = CatalogMetadata(null, null),
    val isRefreshing: Boolean = false,
    val blockingError: String? = null,
)
```

Create `app/src/main/java/com/popcorn/live/ui/live/LiveViewModel.kt`:

```kotlin
package com.popcorn.live.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorn.live.catalog.LiveCatalogRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LiveViewModel(
    private val repository: LiveCatalogRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val isRefreshing = MutableStateFlow(false)

    val uiState = combine(
        repository.categories,
        repository.channels,
        repository.metadata,
        selectedCategoryId,
        searchQuery,
        isRefreshing,
    ) { categories, channels, metadata, selectedId, query, refreshing ->
        val effectiveCategoryId = selectedId ?: categories.firstOrNull()?.id
        if (selectedId == null && effectiveCategoryId != null) {
            selectedCategoryId.value = effectiveCategoryId
        }
        val visibleChannels = channels
            .filter { channel -> query.isNotBlank() || channel.categoryId == effectiveCategoryId }
            .filter { channel -> query.isBlank() || channel.name.contains(query, ignoreCase = true) }
        LiveUiState(
            categories = categories,
            visibleChannels = visibleChannels,
            selectedCategoryId = effectiveCategoryId,
            searchQuery = query,
            metadata = metadata,
            isRefreshing = refreshing,
            blockingError = if (categories.isEmpty() && channels.isEmpty()) metadata.lastRefreshError else null,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LiveUiState())

    init {
        refresh(silent = true)
    }

    fun onCategorySelected(categoryId: String) {
        selectedCategoryId.value = categoryId
    }

    fun onSearchChanged(query: String) {
        searchQuery.value = query
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch(dispatcher) {
            isRefreshing.value = true
            repository.refresh()
            isRefreshing.value = false
        }
    }
}
```

- [ ] **Step 5: Run ViewModel tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.popcorn.live.ui.live.LiveViewModelTest"
```

Expected: PASS.

- [ ] **Step 6: Commit dependency wiring and ViewModel**

```bash
git add app/src/main/java/com/popcorn/live/PopcornApplication.kt app/src/main/java/com/popcorn/live/di app/src/main/java/com/popcorn/live/ui/live app/src/test/java/com/popcorn/live/ui/live
git commit -m "feat: add live view model"
```

## Task 6: Build The TV Live Screen

**Files:**
- Create: `app/src/main/java/com/popcorn/live/ui/theme/PopcornTheme.kt`
- Create: `app/src/main/java/com/popcorn/live/ui/live/LiveScreen.kt`
- Modify: `app/src/main/java/com/popcorn/live/MainActivity.kt`

- [ ] **Step 1: Add Neon Velocity theme tokens**

Create `app/src/main/java/com/popcorn/live/ui/theme/PopcornTheme.kt`:

```kotlin
package com.popcorn.live.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Obsidian = Color(0xFF0C0E12)
val SurfaceLow = Color(0xFF121720)
val SurfaceHigh = Color(0xFF1B2230)
val ElectricCyan = Color(0xFF00E5FF)
val CyanDeep = Color(0xFF145CFF)
val TextPrimary = Color(0xFFF4F7FB)
val TextSecondary = Color(0xFFAAABB0)

private val PopcornColors = darkColorScheme(
    background = Obsidian,
    surface = SurfaceLow,
    primary = ElectricCyan,
    secondary = CyanDeep,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onPrimary = Color.Black,
)

@Composable
fun PopcornTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PopcornColors, content = content)
}
```

- [ ] **Step 2: Implement sidebar plus grid screen**

Create `app/src/main/java/com/popcorn/live/ui/live/LiveScreen.kt`:

```kotlin
package com.popcorn.live.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.popcorn.live.catalog.LiveCategory
import com.popcorn.live.catalog.LiveChannel
import com.popcorn.live.ui.theme.ElectricCyan
import com.popcorn.live.ui.theme.Obsidian
import com.popcorn.live.ui.theme.SurfaceHigh
import com.popcorn.live.ui.theme.SurfaceLow
import com.popcorn.live.ui.theme.TextPrimary
import com.popcorn.live.ui.theme.TextSecondary

@Composable
fun LiveScreen(
    state: LiveUiState,
    onCategorySelected: (String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onChannelSelected: (LiveChannel) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .padding(32.dp),
    ) {
        CategorySidebar(
            categories = state.categories,
            selectedCategoryId = state.selectedCategoryId,
            onCategorySelected = onCategorySelected,
        )
        Spacer(Modifier.width(28.dp))
        Column(Modifier.fillMaxSize()) {
            Header(
                query = state.searchQuery,
                isRefreshing = state.isRefreshing,
                error = state.metadata.lastRefreshError,
                onSearchChanged = onSearchChanged,
                onRefresh = onRefresh,
            )
            Spacer(Modifier.height(24.dp))
            when {
                state.blockingError != null -> BlockingMessage(state.blockingError)
                state.visibleChannels.isEmpty() -> BlockingMessage("Aucune chaîne disponible")
                else -> ChannelGrid(state.visibleChannels, onChannelSelected)
            }
        }
    }
}

@Composable
private fun CategorySidebar(categories: List<LiveCategory>, selectedCategoryId: String?, onCategorySelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceLow.copy(alpha = 0.82f))
            .padding(18.dp),
    ) {
        Text("POPCORN", color = ElectricCyan, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(30.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(categories) { category ->
                val selected = category.id == selectedCategoryId
                Text(
                    text = category.name,
                    color = if (selected) TextPrimary else TextSecondary,
                    fontSize = 18.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) ElectricCyan.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable { onCategorySelected(category.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun Header(query: String, isRefreshing: Boolean, error: String?, onSearchChanged: (String) -> Unit, onRefresh: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Live TV", color = TextPrimary, fontSize = 44.sp, fontWeight = FontWeight.Black)
            Text(error ?: "Catalogue synchronisé avec Xtream", color = if (error == null) TextSecondary else ElectricCyan, fontSize = 14.sp)
        }
        OutlinedTextField(
            value = query,
            onValueChange = onSearchChanged,
            singleLine = true,
            label = { Text("Recherche") },
            modifier = Modifier.width(320.dp),
        )
        Spacer(Modifier.width(16.dp))
        Button(onClick = onRefresh) {
            if (isRefreshing) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Rafraîchir")
            }
        }
    }
}

@Composable
private fun ChannelGrid(channels: List<LiveChannel>, onChannelSelected: (LiveChannel) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Adaptive(220.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(channels, key = { it.streamId }) { channel ->
            ChannelCard(channel, onChannelSelected)
        }
    }
}

@Composable
private fun ChannelCard(channel: LiveChannel, onChannelSelected: (LiveChannel) -> Unit) {
    FocusedBox(
        modifier = Modifier
            .height(108.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceHigh)
            .clickable { onChannelSelected(channel) }
            .padding(16.dp),
    ) {
        Text(channel.name, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("LIVE", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun FocusedBox(modifier: Modifier, content: @Composable Column.() -> Unit) {
    val focused = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Column(
        modifier = modifier
            .onFocusChanged { focused.value = it.isFocused }
            .drawBehind {
                if (focused.value) {
                    drawRoundRect(
                        brush = Brush.linearGradient(listOf(ElectricCyan, Color(0xFF145CFF))),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    )
                }
            }
            .padding(if (focused.value) 3.dp else 0.dp),
        content = content,
    )
}

@Composable
private fun BlockingMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = TextSecondary, fontSize = 24.sp)
    }
}
```

- [ ] **Step 3: Wire MainActivity to ViewModel**

Replace `app/src/main/java/com/popcorn/live/MainActivity.kt` with:

```kotlin
package com.popcorn.live

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.popcorn.live.ui.live.LiveScreen
import com.popcorn.live.ui.live.LiveViewModel
import com.popcorn.live.ui.theme.PopcornTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<LiveViewModel> {
        val repository = (application as PopcornApplication).container.liveCatalogRepository
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LiveViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.uiState.collectAsState()
            PopcornTheme {
                LiveScreen(
                    state = state,
                    onCategorySelected = viewModel::onCategorySelected,
                    onSearchChanged = viewModel::onSearchChanged,
                    onRefresh = { viewModel.refresh() },
                    onChannelSelected = { },
                )
            }
        }
    }
}
```

- [ ] **Step 4: Build and run unit tests**

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Expected: PASS and debug APK generated.

- [ ] **Step 5: Commit live screen**

```bash
git add app/src/main/java/com/popcorn/live/MainActivity.kt app/src/main/java/com/popcorn/live/ui/theme app/src/main/java/com/popcorn/live/ui/live
git commit -m "feat: add live tv screen"
```

## Task 7: Add Playback URL Logic And Media3 Player

**Files:**
- Create: `app/src/main/java/com/popcorn/live/ui/player/PlaybackUrlFactory.kt`
- Create: `app/src/main/java/com/popcorn/live/ui/player/PlayerScreen.kt`
- Create: `app/src/test/java/com/popcorn/live/ui/player/PlaybackUrlFactoryTest.kt`
- Modify: `app/src/main/java/com/popcorn/live/MainActivity.kt`

- [ ] **Step 1: Write playback URL tests**

Create `app/src/test/java/com/popcorn/live/ui/player/PlaybackUrlFactoryTest.kt`:

```kotlin
package com.popcorn.live.ui.player

import com.popcorn.live.xtream.XtreamUrlFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackUrlFactoryTest {
    @Test
    fun createsPreferredHlsAndFallbackTsUrls() {
        val urls = PlaybackUrlFactory(
            XtreamUrlFactory("https://iptv.example.com", "Mike", "1234"),
        ).urlsFor(streamId = 42)

        assertEquals("https://iptv.example.com/live/Mike/1234/42.m3u8", urls.preferred)
        assertEquals("https://iptv.example.com/live/Mike/1234/42.ts", urls.fallback)
    }
}
```

- [ ] **Step 2: Run failing playback test**

```bash
./gradlew :app:testDebugUnitTest --tests "com.popcorn.live.ui.player.PlaybackUrlFactoryTest"
```

Expected: FAIL because playback URL classes do not exist.

- [ ] **Step 3: Implement playback URL factory**

Create `app/src/main/java/com/popcorn/live/ui/player/PlaybackUrlFactory.kt`:

```kotlin
package com.popcorn.live.ui.player

import com.popcorn.live.xtream.XtreamUrlFactory

data class PlaybackUrls(
    val preferred: String,
    val fallback: String,
)

class PlaybackUrlFactory(
    private val xtreamUrlFactory: XtreamUrlFactory,
) {
    fun urlsFor(streamId: Int): PlaybackUrls = PlaybackUrls(
        preferred = xtreamUrlFactory.hlsPlaybackUrl(streamId),
        fallback = xtreamUrlFactory.tsPlaybackUrl(streamId),
    )
}
```

- [ ] **Step 4: Implement player screen**

Create `app/src/main/java/com/popcorn/live/ui/player/PlayerScreen.kt`:

```kotlin
package com.popcorn.live.ui.player

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.popcorn.live.catalog.LiveChannel
import com.popcorn.live.ui.theme.ElectricCyan
import com.popcorn.live.ui.theme.TextPrimary

@Composable
fun PlayerScreen(
    channel: LiveChannel,
    urls: PlaybackUrls,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var usingFallback by remember(channel.streamId) { mutableStateOf(false) }
    var errorMessage by remember(channel.streamId) { mutableStateOf<String?>(null) }
    val player = remember(channel.streamId) {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (!usingFallback) {
                    usingFallback = true
                    player.setMediaItem(MediaItem.fromUri(urls.fallback))
                    player.prepare()
                    player.playWhenReady = true
                } else {
                    errorMessage = error.message ?: "Flux illisible"
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(channel.streamId, usingFallback) {
        if (!usingFallback) {
            player.setMediaItem(MediaItem.fromUri(urls.preferred))
            player.prepare()
            player.playWhenReady = true
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = true
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(32.dp),
        ) {
            Text(channel.name, color = TextPrimary, fontSize = 28.sp)
            Text("LIVE", color = ElectricCyan, fontSize = 14.sp)
            if (errorMessage != null) {
                Text(errorMessage ?: "Flux illisible", color = ElectricCyan, fontSize = 18.sp)
            }
        }
    }
}
```

- [ ] **Step 5: Route from grid to player in MainActivity**

Update `MainActivity` to keep selected channel in Compose state and render `PlayerScreen` when a channel is selected:

```kotlin
var selectedChannel by remember { mutableStateOf<LiveChannel?>(null) }
val playbackUrlFactory = remember {
    PlaybackUrlFactory((application as PopcornApplication).container.xtreamUrlFactory)
}
val channel = selectedChannel
if (channel == null) {
    LiveScreen(
        state = state,
        onCategorySelected = viewModel::onCategorySelected,
        onSearchChanged = viewModel::onSearchChanged,
        onRefresh = { viewModel.refresh() },
        onChannelSelected = { selectedChannel = it },
    )
} else {
    PlayerScreen(
        channel = channel,
        urls = playbackUrlFactory.urlsFor(channel.streamId),
        onBack = { selectedChannel = null },
    )
}
```

Add imports:

```kotlin
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.popcorn.live.catalog.LiveChannel
import com.popcorn.live.ui.player.PlaybackUrlFactory
import com.popcorn.live.ui.player.PlayerScreen
```

- [ ] **Step 6: Run playback tests and build**

```bash
./gradlew :app:testDebugUnitTest --tests "com.popcorn.live.ui.player.PlaybackUrlFactoryTest"
./gradlew :app:assembleDebug
```

Expected: both commands PASS.

- [ ] **Step 7: Commit player**

```bash
git add app/src/main/java/com/popcorn/live/MainActivity.kt app/src/main/java/com/popcorn/live/ui/player app/src/test/java/com/popcorn/live/ui/player
git commit -m "feat: add live player"
```

## Task 8: Generate Logo And Android Launcher Assets

**Files:**
- Create: `app/src/main/res/drawable/ic_launcher_background.xml`
- Create: `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- Create: `app/src/main/res/drawable/popcorn_logo_foreground.png`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Generate the Popcorn logo image**

Use the image generation tool with this prompt:

```text
Use case: logo-brand
Asset type: Android TV launcher icon foreground
Primary request: Create a cinematic neon logo for an app named Popcorn.
Subject: A compact popcorn bucket mark with glowing electric cyan highlights, deep obsidian shadows, and subtle cinema-light energy.
Style: Premium Android launcher logo, vector-friendly, simple silhouette, high contrast, readable at small sizes.
Constraints: No movie studio references, no copyrighted characters, no watermark, no extra text except the word Popcorn only if it remains legible.
Background: Solid obsidian square background that can be separated into Android adaptive icon foreground/background assets.
```

Save the selected generated PNG as `app/src/main/res/drawable/popcorn_logo_foreground.png`.

- [ ] **Step 2: Add adaptive icon XML**

Create `app/src/main/res/drawable/ic_launcher_background.xml`:

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/obsidian" />
</shape>
```

Create `app/src/main/res/drawable/ic_launcher_monochrome.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M24,70 L84,70 L78,94 L30,94 Z M30,32 C32,22 46,18 54,28 C62,18 78,22 80,34 C92,40 88,58 74,58 L34,58 C20,58 16,40 30,32 Z" />
</vector>
```

Create `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`:

```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/popcorn_logo_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
```

Create `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`:

```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/popcorn_logo_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
```

- [ ] **Step 3: Register launcher icons in the manifest**

Update the `<application>` tag in `app/src/main/AndroidManifest.xml` so it contains these attributes:

```xml
android:banner="@mipmap/ic_launcher"
android:icon="@mipmap/ic_launcher"
android:roundIcon="@mipmap/ic_launcher_round"
```

The full opening tag becomes:

```xml
<application
    android:name=".PopcornApplication"
    android:allowBackup="false"
    android:banner="@mipmap/ic_launcher"
    android:icon="@mipmap/ic_launcher"
    android:label="Popcorn"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.Popcorn">
```

- [ ] **Step 4: Build with icon resources**

```bash
./gradlew :app:assembleDebug
```

Expected: PASS and no missing resource errors.

- [ ] **Step 5: Commit assets**

```bash
git add app/src/main/res
git commit -m "feat: add popcorn launcher assets"
```

## Task 9: Add Firestick Deployment Docs And Install Script

**Files:**
- Create: `docs/deployment/firestick.md`
- Create: `scripts/install-firestick.sh`

- [ ] **Step 1: Write deployment guide**

Create `docs/deployment/firestick.md`:

````markdown
# Installing Popcorn On Firestick

## Build The APK

From the repository root:

```bash
cp .env.example .env
./gradlew :app:assembleDebug
```

Edit `.env` with the real Xtream values before building a real APK.

The debug APK is created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Prepare The Firestick

1. Put the Firestick and Mac on the same network.
2. On the Firestick, open `Settings > My Fire TV > About > Network`.
3. Write down the IP address.
4. If Developer Options is hidden, open `Settings > My Fire TV > About` and press the center button seven times on the device name.
5. Open `Settings > My Fire TV > Developer Options`.
6. Enable `ADB Debugging`.
7. Enable unknown app installs when Fire OS asks for it.

## Install The APK

```bash
export FIRESTICK_IP=192.168.1.50
adb connect "$FIRESTICK_IP:5555"
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Replace `192.168.1.50` with the Firestick IP address.

Accept the debug prompt on the Firestick during the first connection.

## Launch Popcorn

Open `Settings > Applications > Manage Installed Applications > Popcorn > Launch application`.

## Clean Reinstall

Use this when validating first launch without cache:

```bash
adb uninstall com.popcorn.live
adb install app/build/outputs/apk/debug/app-debug.apk
```
````

- [ ] **Step 2: Add install helper**

Create `scripts/install-firestick.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${FIRESTICK_IP:-}" ]]; then
  echo "Set FIRESTICK_IP first, for example: export FIRESTICK_IP=192.168.1.50" >&2
  exit 1
fi

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found at $APK_PATH. Run ./gradlew :app:assembleDebug first." >&2
  exit 1
fi

adb connect "$FIRESTICK_IP:5555"
adb install -r "$APK_PATH"
```

Make it executable:

```bash
chmod +x scripts/install-firestick.sh
```

- [ ] **Step 3: Validate script syntax**

```bash
bash -n scripts/install-firestick.sh
```

Expected: no output and exit code 0.

- [ ] **Step 4: Commit deployment docs**

```bash
git add docs/deployment/firestick.md scripts/install-firestick.sh
git commit -m "docs: add firestick install guide"
```

## Task 10: Final APK Verification

**Files:**
- Modify only files required by failing checks from this task.

- [ ] **Step 1: Run all local unit tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 2: Build debug APK**

```bash
./gradlew :app:assembleDebug
```

Expected: `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 3: Install on Firestick**

```bash
export FIRESTICK_IP=192.168.1.50
scripts/install-firestick.sh
```

Expected: ADB prints `Success`.

- [ ] **Step 4: Manual Firestick acceptance test**

Use the Firestick remote:

1. Launch Popcorn.
2. Confirm sidebar focus is visible.
3. Move through categories with the D-pad.
4. Search for a known channel.
5. Select a channel.
6. Confirm video playback starts.
7. Press Back and confirm the grid returns.
8. Use Refresh and confirm the UI remains usable during refresh.

- [ ] **Step 5: Commit final verification fixes**

If the verification task required changes:

```bash
git add app docs scripts
git commit -m "fix: complete firestick verification"
```

If no files changed:

```bash
git status --short
```

Expected: no implementation files modified by the verification task.

## Self-Review Result

- Spec coverage: live-only scope, `.env` configuration, Xtream live endpoints, Room cache, launch refresh, manual refresh, sidebar plus grid UI, Media3 playback, logo assets, APK build, and Firestick ADB install are assigned to tasks.
- Completion scan: no unresolved implementation notes remain.
- Type consistency: domain models, DTO names, repository interfaces, ViewModel state, playback URL classes, and package name `com.popcorn.live` are consistent across tasks.
