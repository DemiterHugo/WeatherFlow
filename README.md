# WeatherFlow

Android project for comparing Jetpack Glance and Remote Compose through weather
widgets, with an animated rain radar as the intended demonstration.

## Project status

The project currently contains a Compose weather screen with MVVM, a modular
build structure, Hilt dependency injection, tested domain models, and an
Open-Meteo weather repository backed by a persistent Room cache, and a resizable
Glance weather widget. Barcelona is the default location; the app can save an
approximate device location for both screen and widgets. Remote Compose and rain
radar rendering will be implemented incrementally.

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
to at least 300 × 300 dp also shows humidity, wind in km/h and the model-estimate
label. The widget supports English/Spanish and light/dark resources, and provides
Refresh, Open app and provider-link actions. A selector preview is available on
Android 12 and later; it contains no sample weather reading.

Glance 1.2.0 produces classic RemoteViews, providing the baseline for the future
Remote Compose comparison. It does not implement the animated radar yet.

Widgets read the same Room repository and DataStore location as the app. An active
Glance session observes cache/location changes. Foreground changes also enqueue
a render-only job, so widgets are updated after their composition sessions end.
No second HTTP request is needed for an app refresh.

WorkManager schedules one unique hourly task while widgets are installed; Android
may defer it because of network, battery or scheduling constraints. Its initial
delay is one hour, and installing a widget requests an immediate refresh. Periodic
ticks and manual refreshes enqueue the same unique fetch job, avoiding overlapping
widget requests. Manual refreshes can report offline failure immediately; failed
jobs retry with exponential backoff, up to three attempts. Removing the final
widget cancels its periodic, fetch and render work. The provider disables platform
periodic updates to avoid a second timer.

Progress and refresh errors are saved per widget and scoped to the requested
coordinates. Failed updates retain cached readings and their original timestamp;
an empty cache shows a message and refresh action. Network requests run in a
CoroutineWorker, outside broadcast callbacks. Hilt entry points supply the shared
repositories to system-created workers and widgets.

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
The rain radar will be integrated separately.

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
./gradlew :app:testDebugUnitTest :data:testDebugUnitTest :widget:testDebugUnitTest :domain:test
./gradlew :app:lintDebug :data:lintDebug :widget:lintDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :data:connectedDebugAndroidTest
```

- `projects` lists the registered Gradle modules.
- `assembleDebug` builds debug artifacts; `:domain:build` builds and checks the JVM library.
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
refresh failures and cancellation, and unique hourly scheduling/removal.
Glance content tests check compact/expanded layouts, empty/error/loading states
and attribution, including actual RemoteViews inflation during refresh. They use
Robolectric 4.17 with Android APIs 26 and 37 and the test-only
JVM options recommended in its [setup guide](https://robolectric.org/getting-started/).
Screen tests also exercise location actions, permission-denial messaging and the
widget-pinning callback.

Automated tests do not call the live weather API. Network tests use MockWebServer
on the local machine; repository and ViewModel tests use in-memory fakes. UI tests
use controlled states, and the activity integration tests replace the real
repository. Instrumented database tests use Android SQLite and do not require
Internet access.

## Development workflow

Use GitFlow with feature branches based on `develop` and Conventional Commits.
Each commit should represent one coherent change that builds and passes its
relevant checks, including any required configuration and documentation updates.
