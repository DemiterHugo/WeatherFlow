# Changelog

## 0.1.0 (unreleased)

First demonstration version of WeatherFlow, comparing Jetpack Glance and
experimental Remote Compose through location-aware weather widgets and historical
precipitation radar. The Android application uses `versionCode = 2`.

### Added

- Four sibling modules: `:app`, `:domain`, `:data` and `:widget`, with a
  platform-independent domain and Hilt dependency injection.
- A Compose weather screen with MVVM and Coroutines/Flow, English and Spanish
  resources, refresh/error states and provider attribution.
- Current Open-Meteo weather backed by a persistent Room cache.
- Barcelona as the default location and an opt-in, approximate foreground location
  fix saved with DataStore and shared by the screen and widgets.
- Resizable Glance and experimental Remote Compose weather widgets with shared
  refresh scheduling, location and cached data.
- Historical RainViewer radar sequences stored atomically, displayed over a shared
  OpenStreetMap basemap with visible provider credits and timestamps.
- A static latest radar frame in large Glance widgets and host-driven historical
  frame playback in large Remote Compose widgets.
- Automated coverage for models, repositories, persistence, ViewModels, widget
  presentation and scheduling, plus local Android instrumented tests.
- GitHub Actions verification for debug/release builds, JVM/Robolectric tests and
  Android Lint, with downloadable verification reports.

### Compatibility and limitations

- The app and Glance require Android 8.0 (API 26) or later. The Remote Compose
  provider is enabled only on Android 17 (API 37) or later and uses pinned
  experimental Remote Compose `1.0.0-alpha22` APIs.
- Radar playback replays historical precipitation frames; it is not a forecast.
  The radar layout requires a widget size of at least 300 × 360 dp.
- Widgets use the saved location. Obtain another foreground fix from the app to
  update it after moving; background location tracking and city search are not
  included.
- Android and the launcher control background execution and playback. The shared
  15-minute schedule does not guarantee exact refresh times or continuous
  animation.
- Hosted CI runs JVM/Robolectric tests. Android instrumented tests remain local.
