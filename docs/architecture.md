# Popcorn Architecture

This document describes the current codebase. The older files under `docs/superpowers` are useful history, but this file and the code are the current reference for agents.

## Runtime Flow

`PopcornApplication` owns one `AppContainer`. `MainActivity` creates ViewModels from that container and renders either the runtime Xtream configuration screen or the main TV experience.

On first launch, `ConfigScreen` collects the Xtream base URL, username, and password. `ConfigViewModel` saves a normalized `XtreamConnectionConfig` through `AppContainer.saveXtreamConnectionConfig`. The container then creates Xtream services for catalog refresh and playback URL construction.

Once configured, `PopcornAppContent` switches between three sections:

- Live TV: `LiveViewModel` plus `LiveScreen`.
- Movies: `MoviesViewModel` plus `MediaScreen`.
- Series: `SeriesViewModel` plus `MediaScreen` with details and episodes.

`PlaybackViewModel` owns the selected playback item. When it has a value, `MainActivity` renders `PlayerScreen` full-screen instead of a catalog screen.

## Package Map

- `com.popcorn.live`: app entry points.
- `config`: runtime Xtream configuration and SharedPreferences persistence.
- `di`: manual dependency graph and service construction.
- `xtream`: Xtream API DTOs, URL factory, OkHttp client, and normalizers.
- `catalog`: domain models plus live/media repository orchestration.
- `cache`: Room entities, DAOs, database classes, and repository stores.
- `user`: favorites, playback progress, and last-playback domain logic.
- `ui.config`: first-launch Xtream credential screen.
- `ui.live`: live TV state, ViewModel, and screen.
- `ui.media`: movie/series state, ViewModel, and shared catalog screen.
- `ui.player`: Media3 playback screen and playback selection logic.
- `ui.navigation`: app section and catalog menu models.
- `ui.components`: reusable UI components.
- `ui.theme`: Neon Velocity color and typography tokens.

## Data Flow

Catalog refresh follows this path:

```text
ViewModel.refresh()
  -> LiveCatalogRepository or MediaCatalogRepository
  -> XtreamApi
  -> XtreamNormalizer
  -> Room-backed store
  -> StateFlow-backed UI state
```

Catalog screens read cached Room data first and keep showing it if refresh fails. Refresh errors are blocking only when the relevant cache is empty.

Playback follows this path:

```text
Catalog item selection
  -> PlaybackViewModel
  -> PlaybackUrlFactory
  -> XtreamUrlFactory
  -> PlayerScreen
```

Movies and series episodes record playback progress through `UserLibraryRepository`. Live TV records the last played channel.

## Persistence

The app intentionally uses separate Room databases:

- `popcorn.db`: live catalog.
- `popcorn-media.db`: movie and series catalog.
- `popcorn-user.db`: user library state.

Schemas are exported in `app/schemas`. Keep these files in sync with entity changes. Add migrations when a schema changes after data may already exist on a device.

Runtime Xtream credentials are stored in `SharedPreferences` named `popcorn_xtream_config`. They are not part of Room and are not build-time values.

## UI And TV Constraints

Popcorn is built for TV distance and D-pad input. Catalog screens use a sidebar, section selector, search, refresh action, and grid content. The focus state is part of the visual design, not an accessibility afterthought.

When changing UI:

- Preserve clear focused, selected, empty, loading, and error states.
- Keep text readable at TV distance.
- Avoid touch-only interactions.
- Check behavior with remote-style navigation, not only mouse clicks.
- Follow `DESIGN.md` for the dark Neon Velocity visual language.

## Security And Content Boundaries

The repository must not contain media providers, playlists, real credentials, channels, movies, series, or media content. Xtream credentials and generated playback URLs are sensitive because URLs include credentials.

Do not log, snapshot, or hard-code full credential-bearing URLs. Unit tests may use fake domains and fake credentials only.

