# Agent Guide

This repository is designed to be edited by AI agents. Start here before changing code.

## Project Snapshot

Popcorn is a native Android TV / Fire TV client for personal Xtream-compatible live TV, movies, and series. It is a single Android application module written in Kotlin with Jetpack Compose, Room, Media3, OkHttp, coroutines, and kotlinx.serialization.

Credentials are entered in the app at runtime and stored locally in Android `SharedPreferences`. `.env.example` is documentation only; the Gradle build does not read `.env` values.

## Read First

- `README.md`: user-facing overview, build, install, and test commands.
- `docs/development.md`: local setup, validation commands, and release workflow.
- `docs/architecture.md`: current runtime architecture and package map.
- `DESIGN.md`: visual direction for TV UI changes.
- `docs/superpowers/specs/2026-05-02-popcorn-live-tv-design.md`: historical v1 spec. Treat it as background, not current truth for configuration or scope.

## Required Commands

Use these from the repository root:

```bash
./scripts/check.sh
```

On macOS, the script auto-detects `~/Library/Android/sdk` if `ANDROID_HOME` is not already set.

That runs the same default checks as CI:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
```

Useful targeted commands:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

`connectedDebugAndroidTest` needs an Android device or emulator.

## Editing Rules

- Keep app code under `app/src/main/java/com/popcorn/live`.
- Prefer small constructor-injected classes and repositories over global state.
- Keep blocking IO behind repository/API/store boundaries and inject dispatchers for testable async work.
- Do not log Xtream credentials, full playback URLs, or local user secrets.
- Preserve Android TV and Fire TV remote navigation. Every interactive UI change must remain D-pad friendly.
- UI strings are currently mostly French inside the app. Keep new in-app copy consistent unless the feature intentionally changes localization.
- Use the Gradle version catalog in `gradle/libs.versions.toml` for dependency changes.
- Do not add providers, playlists, bundled channels, sample credentials, or media content.
- Do not silently change package name `com.popcorn.live` or application id `com.popcorn.live`.

## Room And Persistence

There are three Room databases:

- `PopcornDatabase`: live categories, live channels, and live metadata.
- `PopcornMediaDatabase`: movie/series categories, media items, and media metadata.
- `PopcornUserDatabase`: favorites, playback progress, and last playback.

Room schema export is enabled and schemas live under `app/schemas`. If you change an entity or DAO contract, update the database version, keep migrations explicit, update exported schemas, and add or adjust android tests.

## Test Style

- Unit tests live in `app/src/test/java/com/popcorn/live`.
- Instrumented Room tests live in `app/src/androidTest/java/com/popcorn/live`.
- Prefer fake stores/APIs and coroutine test dispatchers over real network calls.
- For ViewModels, assert state through exposed `StateFlow` values after advancing the test scheduler.
- For Xtream behavior, test URL construction and DTO normalization before UI integration.

## Handoff Checklist

Before handing work back:

1. Run `git status --short`.
2. Run the smallest relevant Gradle test command.
3. Run `./scripts/check.sh` for changes that touch production code, Gradle, resources, or shared behavior.
4. Update `docs/architecture.md` or `docs/development.md` when commands, architecture, persistence, or runtime configuration change.
5. Mention any checks that could not be run and why.
