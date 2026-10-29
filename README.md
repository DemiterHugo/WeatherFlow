# WeatherFlow

Android project for comparing Jetpack Glance and Remote Compose through weather
widgets, with an animated rain radar as the intended demonstration.

## Project status

The project currently contains a working Compose starter application, a modular
build structure and Hilt dependency injection configured in `:app`. Weather data,
widgets and radar rendering will be implemented incrementally.

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
- `testDebugUnitTest` and `test` run local unit tests. The new modules have no tests yet; the app currently has only the generated starter unit test.
- `lintDebug` runs Android static analysis on the debug variant.
- `connectedDebugAndroidTest` installs the debug app and test APK on connected devices and runs instrumentation tests. Set `ANDROID_SERIAL` to select a specific device when several are connected.

`ApplicationStartupTest` launches the production `MainActivity` and checks that it
uses `WeatherFlowApplication`, exercising the Hilt application and activity
startup path. Library modules have no functional tests yet.

## Development workflow

Use GitFlow with feature branches based on `develop` and Conventional Commits.
Each commit should represent one coherent change that builds and passes its
relevant checks, including any required configuration and documentation updates.
