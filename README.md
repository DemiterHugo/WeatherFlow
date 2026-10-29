# WeatherFlow

Android project for comparing Jetpack Glance and Remote Compose through weather
widgets, with an animated rain radar as the intended demonstration.

## Project status

The project currently contains a working Compose starter application, a modular
build structure, Hilt dependency injection configured in `:app`, and tested domain
models with a weather repository contract. Weather data retrieval, widgets and
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
implementations and their behavior tests will be added in `:data`.

## Dependency injection

The application uses [Hilt](https://dagger.dev/hilt/gradle-setup.html) with
[KSP](https://dagger.dev/dev-guide/ksp.html) for code generation. Versions are
pinned in `gradle/libs.versions.toml`.

`WeatherFlowApplication` is the Hilt application root and is registered in the
manifest. `MainActivity` is an Android entry point. Hilt and KSP are applied only
to `:app` at this stage; library modules will add their processing dependencies
when they contain injectable implementations. `:domain` remains independent of
Hilt and Android.

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
```

- `projects` lists the registered Gradle modules.
- `assembleDebug` builds debug artifacts; `:domain:build` builds and checks the JVM library.
- `testDebugUnitTest` and `test` run local unit tests. Domain tests cover model validation and boundary cases; the app also has the generated starter unit test.
- `lintDebug` runs Android static analysis on the debug variant.
- `connectedDebugAndroidTest` installs the debug app and test APK on connected devices and runs instrumentation tests. Set `ANDROID_SERIAL` to select a specific device when several are connected.

`ApplicationStartupTest` launches the production `MainActivity` and checks that it
uses `WeatherFlowApplication`, exercising the Hilt application and activity
startup path. `:data` and `:widget` do not have functional tests yet.

## Development workflow

Use GitFlow with feature branches based on `develop` and Conventional Commits.
Each commit should represent one coherent change that builds and passes its
relevant checks, including any required configuration and documentation updates.
