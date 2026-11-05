# WeatherFlow

Android project for comparing Jetpack Glance and Remote Compose through weather
widgets, including historical precipitation radar: a static latest frame in Glance
and a locally replayed sequence in Remote Compose.

## Project status

Version **0.1.0** (`versionCode = 2`) is the first demonstration release.
See the [changelog](CHANGELOG.md) for its scope and compatibility limits.

The project currently contains a Compose weather screen with MVVM, a modular
build structure, Hilt dependency injection, tested domain models, and an
Open-Meteo weather repository backed by a persistent Room cache, and a resizable
Glance weather widget and an experimental Remote Compose weather widget.
Barcelona is the default location; the app can save an approximate device location
for the screen and both widget providers. A RainViewer repository now downloads
and caches complete historical radar sequences. The large widgets overlay these
images on the same OpenStreetMap basemap.

## Modules

All four modules are siblings in the repository root:

```text
WeatherFlow/
├── app/
├── domain/
├── data/
├── widget/
└── settings.gradle.kts
```

| Module | Type | Intended responsibility | Direct project dependencies |
| --- | --- | --- | --- |
| `:app` | Android application | Compose screens, ViewModels and application dependency wiring | `:domain`, `:data`, `:widget` |
| `:domain` | Kotlin/JVM library | Weather models, repository contracts and use cases where useful | None |
| `:data` | Android library | Repository implementations, network access and local storage | `:domain` |
| `:widget` | Android library | Glance and Remote Compose widget providers, presentation and updates | `:domain` |

Project dependencies use Gradle's `implementation` configuration. The domain
module is independent of Android and the other project modules. Data and widget
implementations do not depend on each other. Both widget implementations will
initially live in separate packages within `:widget`.

## Weather screen

`MainActivity` obtains a Hilt-injected `WeatherViewModel`. The ViewModel observes
the repository and requests one refresh when created. It survives activity
recreation and ignores additional refresh taps while a request is in progress.
`WeatherRoute` collects its `StateFlow` with `collectAsStateWithLifecycle` and
passes state and callbacks to the stateless `WeatherScreen`.

The screen shows temperature and feels-like temperature in Celsius, humidity,
wind converted to km/h, condition and the provider's valid time formatted in the
device locale and time zone. English and Spanish resources are included. The
layout scrolls and constrains its content width for larger displays, with
previews for loading, content, error and enlarged text.

Saved weather remains visible during refresh and after a failed update. A first
load failure offers retry without inventing placeholder readings. Storage errors
have a separate message; retry restarts observation if it previously failed.
Cancellation is propagated. Provider attribution links to Open-Meteo and identifies
the data as weather-model estimates.

### Location

Barcelona is selected initially. **Use my location** requests foreground,
approximate location permission and obtains one fix through Android's location
APIs. The request is cancellable and limited to 30 seconds. The coordinates are
saved atomically with Preferences DataStore and shared with every widget. The
screen labels this as a saved device location, rather than claiming a city name
without reverse geocoding. **Use Barcelona** restores the default.

Permission denial, disabled location services and storage failures preserve the
previous selection and display a message. Changing location cancels the previous
weather request and observation and clears its displayed readings, so one place's
weather is never shown under another place's label. The app does not request
background or precise location access. To update the saved position after moving,
use **Use my location** again in the app; widgets refresh weather at the saved
coordinates and do not track movement. There is no city search yet.

## Glance widget

Add **WeatherFlow · Glance** from the launcher's widget picker, or use **Add Glance
widget** in the app when the launcher supports pinning. The compact layout shows
location, temperature, condition, provider valid time and attribution. Expanding
to at least 260 × 270 dp shows the compact radar with a bounded square map. The
widget supports English/Spanish and light/dark resources, and provides Refresh,
Open app and provider-link actions. A selector preview is available on
Android 12 and later; it contains no sample weather reading.

Glance 1.2.0 produces classic RemoteViews. At **260 × 270 dp** or larger, its radar
layout shows the newest historical image, its generation time, coverage information
and provider credits. Smaller layouts show weather and the provider name. On the Galaxy S25 Ultra,
both providers fit at 4 × 3 cells on the same One UI page. Add each from the
app, then resize any older widget to the same size for a direct comparison.

Widgets read the same Room repository and DataStore location as the app. An active
Glance session observes cache/location changes. Foreground changes also enqueue
a render-only job, so widgets are updated after their composition sessions end.
No second HTTP request is needed for an app refresh.

WorkManager schedules one unique 15-minute task while widgets are installed; Android
may defer it because of network, battery or scheduling constraints. Its initial
delay is 15 minutes, and installing a widget requests an immediate refresh. Periodic
ticks and manual refreshes enqueue the same unique fetch job, avoiding overlapping
widget requests. Manual refreshes can report offline failure immediately; failed
jobs retry with exponential backoff, up to three attempts. Removing the final
widget cancels its periodic, fetch and render work. Updating an existing installation
changes its hourly periodic work in place, rather than leaving the old interval.
The provider disables platform
periodic updates to avoid a second timer.

Progress and refresh errors are persisted in a shared Preferences DataStore and
scoped to the requested coordinates. Failed updates retain cached readings and their original timestamp;
an empty cache shows a message and refresh action. Network requests run in a
CoroutineWorker, outside broadcast callbacks. Hilt entry points supply the shared
repositories to system-created workers and widgets.

## Experimental Remote Compose widget

On Android 17 (API 37) and later, add **WeatherFlow · Remote Compose** from the
launcher picker or **Add Remote Compose** in the app.
Its compact and expanded layouts use the same localized presentation values,
weather cache, selected location, units, valid time and Open-Meteo attribution as
Glance. Actions refresh the weather, open the app and open the provider's website.
The layout is regenerated after resizing. The radar layout shows the provider
name and a bounded square map. The receiver only schedules work.

The `remote` package captures a Remote Compose document in a CoroutineWorker and
sends it through Android's `RemoteViews.DrawInstructions`. It uses the platform
widget profile for document API level 7. This does not embed a player in the app
or convert the entire widget to a bitmap. At **260 × 270 dp** or larger, the
radar layout replays the same historical frames used by Glance. A remote integer
clock expression selects one state per second in `RemoteStateLayout`. Each state
contains its image and matching generation time/counter. The host executes the
sequence; Kotlin coroutines and WorkManager do not drive individual frames.
The basemap is recorded once and reused across frame states. Documents are capped
at 512 KiB before transport; creation/size failures use the existing explicit
experimental-widget error view. The clock expression's alpha22 restricted API
suppression is isolated in `RemoteRadarContent`.

Both providers share one unique 15-minute schedule and one fetch queue. Removing
either provider keeps the schedule while the other still has installed instances.
The final removal cancels the shared work. Foreground cache/location changes render
both providers without another weather HTTP request. A location change replaces
outdated fetch work with a radar-only refresh; the app refreshes its weather.
Resizing to radar mode replaces pending work with a full refresh so installation's
initial weather request is not lost. A one-second delay coalesces rapid changes,
and a new request does not depend on an older request succeeding.
Rendering is serialized so a slower
capture cannot overwrite a newer render. Glance observes the shared progress store
during active sessions; Remote Compose receives a new document on each update.

### Compatibility and alpha integration

Remote Compose is pinned to **1.0.0-alpha22**, an experimental release. Google's
[architecture guide](https://developer.android.com/develop/ui/compose/system/remote-compose)
describes document creation, transport and playback in the host process.
WeatherFlow enables this provider only on API 37+, through a versioned
resource and runtime checks; the app and Glance keep their API 26 minimum.

The creation-compose dependency declares API 29 as its minimum. A manifest
`tools:overrideLibrary` exception applies only to that dependency. Its document
capture and transport are isolated behind API 37 checks and `@RequiresApi(37)`;
the dependency manifest has no startup component. Compatibility tests check the
older-device paths and provider visibility. This exception must be reviewed when
updating the library.

`RemoteWeatherDocument` contains the restricted profile and action-ID calls,
with narrowly scoped lint suppressions. In alpha22, the built-in widget profile's
factory drops the `WriterEvents` callback, causing `pendingIntentAction` capture
to fail. The adapter preserves the API-7 widget profile metadata and forwards the
callback to `RemoteComposeWriterAndroid`. Document tests cover all three actions
and verify that refresh is disabled while an update is running. The workaround
must be reviewed against future [Remote Compose releases](https://developer.android.com/jetpack/androidx/releases/compose-remote).

If document creation fails, the experimental widget shows an explicit error with
refresh/open actions, and Glance can still receive updates. Enabling the provider
by Android version does not replace testing it in the device's actual launcher.

This integration was manually checked in the Android 17 emulator's Pixel launcher
and One UI on a Samsung Galaxy S25 Ultra running Android 17. Checks included
pinning and displaying the widget; the emulator also covered refresh, opening the
app and resizing to the expanded layout. The same APK was installed and launched
on an Android 9 emulator to check compatibility below the dependency's declared
API 29 minimum. API 26 paths are covered by Robolectric, rather than a physical
Android 8 device.

## Weather domain

`:domain` defines immutable models and the repository interface shared by screens
and widgets:

- `Coordinates`: latitude and longitude in decimal degrees, with inclusive ranges of `[-90, 90]` and `[-180, 180]`. Non-finite values are rejected.
- `CurrentWeather`: coordinates, temperature and feels-like temperature in Celsius, relative humidity as an integer percentage, wind speed in meters per second, condition and observation time as `java.time.Instant`.
- `WeatherCondition`: provider-independent categories, including `UNKNOWN` for unrecognized provider codes. Provider code mapping belongs to `:data`.
- `WeatherRepository`: observes available weather with `Flow<CurrentWeather?>` and requests updates with a suspending refresh method.

Observation does not trigger a network request. Each collector receives the latest
available observation (or `null` if none exists), followed by updates for the
requested coordinates. Refresh publishes a new observation after storing it;
failures propagate as exceptions and retain the previous observation. Coroutine
cancellation must propagate normally.

Models reject non-finite temperatures, humidity outside `0..100`, and negative or
non-finite wind speeds. Observation time describes when the weather was observed,
not when it was downloaded. Consumers can use it to identify stale data.

The domain uses Kotlin/JVM, `java.time` and Coroutines Core, with no Android, Hilt
or network dependencies. Coroutines Core is exposed with Gradle's `api`
configuration because `Flow` appears in the public repository contract. Repository
implementations and their behavior tests live in `:data`.

## Weather data and offline cache

`:data` implements `WeatherRepository` using Retrofit, Kotlin Serialization and
Room. Hilt binds a singleton repository and provides the HTTPS API client and
database. The data module contributes the Internet permission through manifest
merging.

Observation reads Room only and does not call the network. An explicit refresh
requests current conditions from Open-Meteo, validates the response, then stores
it. Room notifies active collectors, and later collectors can read the saved
observation after process recreation. Network, decoding, validation and database
errors propagate to callers; failed refreshes retain the previous valid data.
Coroutine cancellation is not swallowed.

The API is explicitly requested to return Celsius, meters per second and Unix
timestamp seconds. Response units are checked before mapping to domain models.
Cache keys use the requested coordinates, not the weather model's grid-cell
coordinates. A database transaction compares timestamps before writing, so late
responses cannot replace newer observations. Corrected readings at the same
timestamp can replace the previous values.

Room's version 1 schema is checked into `data/schemas` for future migration tests.
The cache has no automatic expiration; consumers inspect `observedAt`. The screen
requests updates explicitly and widget background work refreshes it while widgets
are installed. It is a snapshot cache, not a weather-history database.

### Provider and attribution

Weather data is provided by [Open-Meteo](https://open-meteo.com/), using its
[Forecast API](https://open-meteo.com/en/docs). Current conditions are estimates
based on weather models, rather than direct station measurements. `observedAt`
represents the provider's valid time, not the local download time.

Open-Meteo data is licensed under
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). WeatherFlow maps provider
codes to its own condition categories and stores a subset of the response. The
free hosted endpoint is intended for non-commercial use and is subject to
[usage limits and attribution requirements](https://open-meteo.com/en/pricing).
Weather screens and widgets must carry provider attribution when implemented.
The radar uses its own provider and cache, described below.

## Historical rain radar data

`RadarRepository` in `:domain` exposes cache-only `observeRadar(coordinates)` and
an explicit suspending `refreshRadar(coordinates)`. Its `:data` implementation is
bound as a Hilt singleton. Callers pass the coordinates from the shared location
repository, so Barcelona and a saved device position use the same contract.
Both large widgets use this repository and a cache-only `WidgetRadarReader`.
Widget installation, manual refresh and shared periodic work update radar data.
Weather and radar have independent persisted progress/errors scoped to coordinates.
Failure of one does not suppress successful data from the other. Operation
timeouts are reported as failures; cancellation of the worker propagates.

A refresh reads the RainViewer manifest, sorts and deduplicates historical
frames, and selects up to six of the most recent available frames. Conflicting
identifiers/timestamps and future frames are rejected. The implementation uses
the manifest's opaque paths instead of constructing them from timestamps.
Unknown fields, including nowcast and satellite fields, are ignored.

Each image is a 256 × 256 PNG centered on the requested coordinates, at zoom 7,
using Universal Blue and smoothing. Images cover a region rather than a street
or neighborhood. Latitude must be inside Web Mercator's ±85.05112878° range;
unsupported polar locations fail before requesting data. Domain snapshots include
the center, zoom and image size so future renderers can align geographic layers.
No basemap is included in the provider's transparent precipitation images.

`RadarFrame.generatedAt` is the composite frame's generation time; contributing
radars may have different observation times. `manifestGeneratedAt`, `fetchedAt`
and the coverage mask's `checkedAt` remain separate. Refreshing an unchanged
manifest does not make its historical frames current. The public API supplies
historical evolution, with no future precipitation prediction.

### Coverage and persistence

The same geographic area has a coverage mask: transparent pixels indicate
potential radar coverage and opaque pixels indicate unavailable areas. The
snapshot reports full, partial or absent coverage of the tile, plus whether its
center is covered. This infrequently updated mask describes potential coverage,
not the operational status of every contributing radar. An empty precipitation
image alone must never be interpreted as proof that there is no rain.

Frames and their coverage mask are downloaded and validated before publication.
PNG signature, dimensions, termination and actual Android decoding are checked;
individual network image bodies are capped at 512 KiB, and manifests at 128 KiB.
The implementation reuses unchanged frames by source identifier and generation
time. Coverage is revalidated after 24 hours; changing location obtains a separate
sequence and mask.

The single-process disk cache stores a versioned ZIP containing metadata and PNGs
in the app's cache directory. It syncs a temporary file, then atomically replaces
the committed snapshot on the same filesystem. CRC checks, bounded archive reads
and image validation protect reopening. Failed downloads, invalid data and failed
replacement writes preserve the previous complete snapshot. Cancellation
propagates; no incomplete sequence is published. Interrupted temporary files are
removed on reopening and corrupt entries become cache misses. Filesystem access
errors propagate instead of appearing as valid empty data.

Retention is capped at four locations and 8 MiB of committed snapshots, evicting
the oldest written entries. One temporary snapshot may consume up to another
4 MiB during replacement. Cache keys include the full requested coordinates;
locations do not share images accidentally. Eviction can emit `null` to active
observers. Older manifest generation times cannot overwrite a newer committed
snapshot.
This disposable cache is independent of Room; the weather database schema stays
at version 1.

Refreshes are serialized, run off the main thread and have a 60-second overall
timeout. HTTP calls have a 10-second connect timeout and a 30-second call timeout,
with cancellation attached until the response body has been read. The client
spaces requests by at least 750 ms, disables automatic retries and redirects,
and honors `Retry-After` on HTTP 429/503. During the cooldown, another request
fails without contacting the provider. Callers decide when to retry; this step
does not introduce an additional timer alongside the shared widget schedule.
Other clients sharing the same public IP may
also consume the provider's allowance.

### Radar provider and attribution

Radar data is provided by [RainViewer](https://www.rainviewer.com/). Its public
[Weather Maps API](https://www.rainviewer.com/api/weather-maps-api.html) describes
two hours of past radar frames at ten-minute intervals, including coordinate-
centered images and coverage masks. The
[transition summary](https://www.rainviewer.com/api/transition-faq.html) documents
the January 2026 restrictions: zoom 7, Universal Blue, 100 requests/IP/minute,
and withdrawal of nowcast and satellite IR. Some general FAQ statements differ;
this integration follows the specific Weather Maps and transition documentation.

The free API is intended for personal and educational use, requires no API key,
and has no availability guarantee. Review its
[terms and attribution requirements](https://www.rainviewer.com/api.html) before
redistributing or using the project commercially. The radar layouts visibly credit
**Radar: RainViewer** and link to the provider. This repository's source availability does not relicense the provider's
data. Automated tests use controlled responses and generated images, with no
live provider calls or downloaded radar imagery checked into Git.

### Geographic map and radar presentation

`RadarMapRepository` exposes a cache-only map flow and an explicit refresh.
Its OpenStreetMap implementation downloads only the tiles intersecting the current
256 × 256, zoom-7 viewport (at most four tiles). The worker requests map tiles only
while a radar-sized widget exists; resizing to that size schedules a radar/map
refresh. There is no map browsing, bulk download, adjacent-area prefetch or offline
map-download feature.

Web Mercator world-pixel coordinates position and crop these tiles around the
radar's saved center. Longitude wraps at the dateline; latitude never wraps across
the poles. Both providers share the same basemap, precipitation overlays, coverage
shading and center marker. Glance composites the latest overlay; Remote Compose
keeps the base and overlays separate for document reuse. A missing map produces
an explicit unavailable state rather than an unlocated precipitation picture.

The map uses a 16 MiB OkHttp disk cache, honors server expiry and conditional
revalidation headers, and supplies a seven-day fallback only when no cache/expiry
headers are provided. Cache-only reads never access the network. Invalid imagery
is removed from the HTTP cache so it can be retried. Requests identify WeatherFlow
with its repository URL. Both radar layouts show a visible, clickable OpenStreetMap
copyright credit, in addition to RainViewer and Open-Meteo attribution. See the
[tile usage policy](https://operations.osmfoundation.org/policies/tiles/) and
[OpenStreetMap copyright](https://www.openstreetmap.org/copyright). The public tile
service offers no availability guarantee.

Frame timestamps remain visible during replay, with a frame counter. Coverage
is described explicitly. At render time, the entire sequence is marked old when
its newest frame is more than 30 minutes old. Timestamps remain visible between
refreshes even if background work is deferred. The historical frames are not a forecast.
Android controls background scheduling and launcher rendering; the 15-minute
interval is not a guarantee of exact refresh times or uninterrupted playback.

The radar layouts were manually checked in the Android 17 Pixel launcher;
frame/image and timestamp changes were also verified in One UI on the Samsung
Galaxy S25 Ultra. Document tests verify six frame states, five actions and seven
encoded bitmaps (one shared map plus six overlays). Glance tests inflate actual
RemoteViews and check that header text remains separated after measurement.

## Dependency injection

The application uses [Hilt](https://dagger.dev/hilt/gradle-setup.html) with
[KSP](https://dagger.dev/dev-guide/ksp.html) for code generation. Versions are
pinned in `gradle/libs.versions.toml`.

`WeatherFlowApplication` is the Hilt application root and is registered in the
manifest. `MainActivity` is an Android entry point. Hilt and KSP are applied to
`:app`, `:data` and `:widget`. Room also uses KSP; Kotlin Serialization generates the JSON
serializers. `:domain` remains independent of Hilt and Android.

## Build and verification

Use the checked-in Gradle Wrapper and install Android SDK Platform 37. The wrapper
uses the Java 25 daemon toolchain configured in
`gradle/gradle-daemon-jvm.properties`; compiled application and library bytecode
targets Java 17.

```sh
./gradlew projects
./gradlew :app:assembleDebug :data:assembleDebug :widget:assembleDebug :domain:build
./gradlew :app:assembleRelease
./gradlew :app:testDebugUnitTest :data:testDebugUnitTest :widget:testDebugUnitTest :domain:test
./gradlew :app:lintDebug :data:lintDebug :widget:lintDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :data:connectedDebugAndroidTest
```

- `projects` lists the registered Gradle modules.
- `assembleDebug` builds debug artifacts; `:domain:build` builds and checks the JVM library.
- `assembleRelease` builds the optimized release APK.
- `testDebugUnitTest` and `test` run local unit tests. Domain tests cover model validation; data tests cover HTTP requests, decoding, mappings and repository behavior using controlled responses.
- `lintDebug` runs Android static analysis on the debug variant.
- `connectedDebugAndroidTest` installs the debug app and test APK on connected devices and runs instrumentation tests. Set `ANDROID_SERIAL` to select a specific device when several are connected.

`WeatherViewModelTest` checks initial refresh, cached data, retry, storage errors,
cancellation and overlapping refresh prevention. Formatter tests cover decimal
localization and time-zone offsets. `WeatherScreenTest` checks loading, content,
errors, refresh, provider attribution and scrolling with enlarged text.

`ApplicationStartupTest` launches the production `MainActivity` with a
`HiltTestApplication` and a test repository installed with `@TestInstallIn`. It
verifies ViewModel injection, refresh interactions and retention across activity
recreation. The repository binding module is public so app instrumentation can
replace the binding while the concrete data implementation remains internal.

`WeatherDatabaseTest` exercises actual Room queries, observation,
isolation between locations, concurrent writes and persistence after reopening
a database on Android. Each test uses its own temporary database and deletes it
afterward. Location preference tests verify the default, replacement and
persistence after reopening storage.

Widget tests verify cache/session observation, switching locations, progress,
refresh failures and cancellation, and unique 15-minute scheduling/removal.
Glance content tests check compact/expanded layouts, empty/error/loading states
and attribution, including actual RemoteViews inflation during refresh. They use
Robolectric 4.17 with Android APIs 26 and 37 and the test-only
JVM options recommended in its [setup guide](https://robolectric.org/getting-started/).
Screen tests also exercise location actions, permission-denial messaging and the
separate widget-pinning callbacks. Registry and scheduling tests cover Glance-only,
Remote-only and final-removal cases. Resource tests exclude Remote Compose on APIs
26, 35 and 36. Remote document tests capture compact, expanded, loading and error
content and verify immutable, explicit, per-widget action intents. Shared-status
tests run on APIs 26 and 37.

Automated tests do not call the live weather API. Network tests use MockWebServer
on the local machine; repository and ViewModel tests use in-memory fakes. UI tests
use controlled states, and the activity integration tests replace the real
repository. Instrumented database tests use Android SQLite and do not require
Internet access.

Radar tests cover immutable encoded images and ordered snapshots, manifest
validation, provider URL construction, request spacing/cooldowns and HTTP body
limits/cancellation with MockWebServer. Repository tests verify atomic publication,
reuse, location isolation and failure preservation. Real filesystem tests cover
reopening, eviction, corruption and interrupted writes. Android instrumentation
checks actual PNG decoding, coverage classification and complete sequence
persistence with generated images. No live network is required for these tests.

## Continuous integration

[Android CI](.github/workflows/android-ci.yml) runs on branch pushes and pull
requests. Its **Build, test and lint** job uses Ubuntu 24.04, Temurin Java 25,
Android SDK Platform `android-37.0` and Build Tools `36.0.0`, the effective default
of the pinned Android Gradle Plugin. Application bytecode still targets Java 17.
Gradle comes from the checked-in Wrapper; CI does not select a different version.
The SDK step reuses these components when present on the runner and installs
them with `sdkmanager` only when needed, then checks their expected paths.

The job builds the debug APK, runs local tests in all four modules, builds the
release APK, then runs debug lint for the three Android modules. Each command
runs in a separate step/process. Release source generation and debug lint stay
separate to avoid the generated-source lookup failure observed with Hilt/KSP.

The Gradle options make CI output reproducible and useful for diagnosis:

- `--no-daemon` uses a fresh build process for each invocation.
- `--stacktrace` includes exception details on failure.
- `--console=plain` keeps logs free of interactive console formatting.
- `--continue` runs independent test/lint tasks after a failure; the command still fails overall.

The official [Gradle action](https://github.com/gradle/actions/tree/main/setup-gradle)
validates the Wrapper JAR and caches Gradle User Home using its **basic** GitHub
cache provider. Pull requests restore caches without writing them; branch pushes
can update their caches. No additional cache service or secret is required.
Actions are pinned to full commit SHAs, with their major versions in comments;
review these pins deliberately when updating the workflow.

Available test results and build/lint reports are uploaded as the
`android-verification-reports` artifact for **14 days**, including when a preceding
check fails. Cancelled runs skip the upload. Missing reports produce a warning
when setup or compilation failed before they could be generated. Earlier checks
retain their failure status. New commits cancel obsolete runs for the same
branch or pull request; each job has a 45-minute limit.

The workflow uses read-only repository permissions and does not retain checkout
credentials. It compiles the release variant without publishing APKs, creating
releases or configuring production signing. Automated tests use controlled
responses and do not request live weather, radar or map data.

This first workflow covers local JVM/Robolectric tests and static/build checks.
Instrumented tests continue to use the emulator/device commands above; they are
not yet part of hosted CI. After pushing the workflow, inspect its run in the
repository's **Actions** tab and download its reports. Local validation does not
exercise the hosted SDK setup, cache or artifact-upload steps.

## Development workflow

Use GitFlow with feature branches based on `develop` and Conventional Commits.
Each commit should represent one coherent change that builds and passes its
relevant checks, including any required configuration and documentation updates.
