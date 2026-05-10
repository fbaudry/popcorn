# Development Guide

## Requirements

- JDK 17.
- Android SDK with platform tools.
- Network access for first Gradle dependency resolution.
- Android TV, Fire TV, emulator, or connected device for manual validation.

The Gradle wrapper is pinned in `gradle/wrapper/gradle-wrapper.properties`. Use `./gradlew` instead of a system Gradle install.

## Setup

From the repository root:

```bash
./gradlew --version
./gradlew :app:testDebugUnitTest
```

No `.env` file is required for builds. Xtream connection values are entered on first app launch.

## Daily Validation

Run the agent-friendly check script:

```bash
./scripts/check.sh
```

On macOS, the script auto-detects the default SDK path at `~/Library/Android/sdk` when `ANDROID_HOME` is not already set.

It runs:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
```

For a full local APK build:

```bash
POPCORN_FULL_CHECK=1 ./scripts/check.sh
```

That adds:

```bash
./gradlew :app:assembleDebug
```

## Targeted Commands

Unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

Lint:

```bash
./gradlew :app:lintDebug
```

Debug APK:

```bash
./gradlew :app:assembleDebug
```

Instrumented tests:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Instrumented tests require a connected Android device or emulator.

## Fire TV Install

Build the APK:

```bash
./gradlew :app:assembleDebug
```

Install with the helper:

```bash
export FIRESTICK_IP=192.168.1.50
./scripts/install-firestick.sh
```

Replace `192.168.1.50` with the Fire TV device IP. More details are in `docs/deployment/firestick.md`.

## Dependency Changes

Add or update dependency versions in `gradle/libs.versions.toml`. Prefer existing AndroidX, Kotlin, Room, Media3, OkHttp, and coroutine patterns before introducing new libraries.

After dependency changes, run:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
```

## Database Changes

Room schemas are exported to `app/schemas`. When changing entities or DAO behavior:

1. Bump the matching database version.
2. Add an explicit migration when existing user data must be preserved.
3. Regenerate exported schemas through a Gradle build/test.
4. Add or update an instrumented test under `app/src/androidTest/java/com/popcorn/live/cache`.

## Documentation Drift

Update this file and `docs/architecture.md` when setup, commands, persistence, runtime configuration, or package ownership changes.
