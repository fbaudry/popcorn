# Popcorn Live TV Android App Design

Date: 2026-05-02
Status: Approved for implementation planning

## Goal

Build Popcorn, a personal Android TV / Firestick APK for watching live TV from an Xtream Codes IPTV subscription. The first version focuses only on live TV: category browsing, channel search, cached catalog data, manual refresh, and full-screen playback.

## Non-Goals For Version 1

- VOD movies.
- Series.
- Replay / catch-up TV.
- Full EPG grid.
- In-app account setup or multi-profile account switching.
- Background periodic sync while the app is fully closed.

## Platform And Stack

Popcorn will be a native Android application built from a fresh project:

- Kotlin.
- Jetpack Compose for TV-oriented UI.
- Compose focus management for D-pad navigation. AndroidX TV or Leanback utilities may be used only when they improve focus behavior without replacing the Compose UI layer.
- AndroidX Media3 / ExoPlayer for live stream playback.
- Room for the local catalog cache.
- Gradle build config fields generated from local environment values.
- Android application ID: `com.popcorn.live`.

The target device class is Fire TV / Firestick. Navigation must work with a D-pad remote without requiring touch input.

## Configuration

The app uses build-time credentials from a local `.env` file. Version 1 does not include a login or credential editing screen.

Expected `.env` keys:

```env
XTREAM_BASE_URL=https://example.com:8080
XTREAM_USERNAME=username
XTREAM_PASSWORD=password
```

The `.env` file must stay out of version control. The repository must include a safe `.env.example` with non-secret sample values.

## Xtream Codes API

The app uses the Xtream Codes `player_api.php` live endpoints documented in the Fermata discussion:

- Account validation / server info:
  `GET {baseUrl}/player_api.php?username={username}&password={password}`
- Live categories:
  `GET {baseUrl}/player_api.php?username={username}&password={password}&action=get_live_categories`
- Live streams:
  `GET {baseUrl}/player_api.php?username={username}&password={password}&action=get_live_streams`
- Live streams for one category:
  `GET {baseUrl}/player_api.php?username={username}&password={password}&action=get_live_streams&category_id={categoryId}`

Playback URLs are derived from the stream ID:

- Preferred HLS:
  `{baseUrl}/live/{username}/{password}/{streamId}.m3u8`
- Fallback transport stream:
  `{baseUrl}/live/{username}/{password}/{streamId}.ts`

The implementation must normalize `baseUrl` by trimming trailing slashes. Credentials must not be logged.

Source reference: https://github.com/AndreyPavlenko/Fermata/discussions/434

## Data Model

Room stores the live catalog:

- `LiveCategory`
  - `categoryId`
  - `name`
  - `sortOrder`
  - `lastUpdatedAt`
- `LiveChannel`
  - `streamId`
  - `name`
  - `categoryId`
  - `streamIcon`
  - `streamType`
  - `added`
  - `sortOrder`
  - `lastUpdatedAt`
- `CatalogMetadata`
  - `lastSuccessfulRefreshAt`
  - `lastRefreshError`

Favorites are not part of v1. The schema must not add favorites tables during the first implementation.

## Refresh And Cache Behavior

On app launch:

1. Load cached categories and channels immediately.
2. If no cache exists, show a first-load state.
3. Start a silent refresh in the background while the app is open.
4. Keep displaying the existing cache if refresh fails.

Manual refresh:

- A refresh action is available in the header.
- It fetches categories and live streams from Xtream, normalizes them, and upserts them into Room.
- A successful refresh updates `lastSuccessfulRefreshAt`.
- A failed refresh preserves the previous cache and exposes a concise error state.

The app does not schedule periodic refresh while fully closed in v1. Fire OS background behavior is too device-dependent for the initial scope.

## User Interface

The UI follows the `DESIGN.md` Neon Velocity direction:

- Deep obsidian background.
- Electric cyan for active state, live indicators, progress, and primary actions.
- Glass-like side navigation and persistent controls with blur where the platform supports it.
- Large, high-contrast typography suitable for TV distance.
- No generic SaaS cards or landing page.

Main screen:

- Left sidebar lists live categories.
- Right content area shows a grid of channels in the selected category.
- Header includes Popcorn branding, search entry, refresh action, and refresh status.
- The current focused item has a strong cyan treatment.
- Empty and error states are visible from a sofa distance.

Search:

- Search filters cached live channels locally.
- Search results open directly into playback.
- Search is optimized for remote input and must support Android TV keyboard input.

## Playback

Selecting a channel opens a full-screen Media3 player:

- Start playback with the preferred `.m3u8` URL.
- If playback fails due to source loading, retry once with the `.ts` URL.
- `Back` returns to the channel grid.
- `OK` toggles playback controls.
- When the playback controls are hidden, up/down moves to the previous or next channel in the current visible list. When controls are visible, D-pad focus stays inside the player controls.

The player must show a minimal overlay with:

- Channel name.
- Live indicator.
- Loading state.
- Playback error with a return action.

## Error Handling

Required states:

- Missing `.env` values at build time: fail the build with a clear message.
- Invalid Xtream credentials: show a blocking credential/account error.
- Network failure with cache available: keep the cached catalog and show a non-blocking error.
- Network failure with no cache: show a first-load error with retry.
- Empty category or empty subscription: show a clear empty state.
- Unsupported or failing stream: show playback error and allow returning to the grid.

## Logo And App Assets

Generate a Popcorn launcher logo as part of implementation:

- Cinematic neon style matching the design system.
- Clear popcorn-related mark plus the Popcorn identity.
- Readable at Android launcher sizes.
- Export Android icon assets for foreground, background, monochrome, and a standard launcher icon.

The generated image must be saved into the project, not left only in a temporary generation directory.

## Testing And Validation

Unit tests:

- Xtream URL construction.
- Xtream response parsing / normalization.
- Cache upsert behavior.
- Refresh success and failure behavior.

UI / behavior validation:

- Main screen can render cached categories and channels.
- D-pad focus moves through sidebar and channel grid predictably.
- Search filters cached channels.
- Player receives the expected playback URL.

Manual Firestick validation:

- Build debug APK.
- Install with `adb install`.
- Confirm app opens with remote navigation.
- Confirm live channel playback works with a real subscription.
- Confirm refresh preserves cache after a simulated network failure.

## Deployment Plan

The v1 deployment path is local sideloading through ADB over the local network. This is the recommended path for Firestick iteration because it does not require publishing to the Amazon Appstore and does not require copying the APK through a third-party downloader app.

Build output:

- The implementation must provide a Gradle command to build a debug APK:
  `./gradlew :app:assembleDebug`
- The expected output path is:
  `app/build/outputs/apk/debug/app-debug.apk`
- A release/signed APK can be added after the debug APK is validated on the Firestick. Release signing is not required for the first local sideload workflow.

Firestick preparation:

1. Install Android SDK Platform-Tools on the development computer so the `adb` command is available.
2. On the Firestick, enable Developer Options. On Fire OS versions where the menu is hidden, open `Settings > My Fire TV > About` and press the remote D-pad center button seven times on the device name.
3. Open `Settings > My Fire TV > Developer Options`.
4. Enable `ADB Debugging`.
5. Enable unknown app installs if Fire OS prompts for this during sideloading.
6. Put the Firestick and the development computer on the same network.
7. Find the Firestick IP address in `Settings > My Fire TV > About > Network`.

Install from the development computer:

```bash
export FIRESTICK_IP=192.168.1.50
adb connect "$FIRESTICK_IP:5555"
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Replace `192.168.1.50` with the Firestick IP address from the Fire TV network screen. The first ADB connection requires accepting the debugging prompt on the Firestick. The `-r` flag reinstalls the app while preserving existing app data and cache, which is useful when testing catalog caching between builds.

Launch after install:

- From Fire TV UI: `Settings > Applications > Manage Installed Applications > Popcorn > Launch application`.
- Or from the Apps section, where sideloaded apps appear after installation.

Clean reinstall path:

```bash
adb uninstall com.popcorn.live
adb install app/build/outputs/apk/debug/app-debug.apk
```

Use a clean reinstall when testing first-launch behavior, missing cache behavior, or schema migration issues.

References:

- Amazon Fire TV ADB connection guide: https://www.developer.amazon.com/docs/fire-tv/connecting-adb-to-device.html
- Amazon Fire TV app install guide: https://developer.amazon.com/docs/fire-tv/installing-and-running-your-app.html

## Implementation Boundaries

The implementation starts from a clean Android project and does not restore the previously deleted `mobile/` project. Existing worktree changes outside the new implementation files are left untouched.
