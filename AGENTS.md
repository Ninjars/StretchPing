# StretchPing — Contributor & Agent Guide

StretchPing is a no-fuss Android stretching-timer app (Kotlin, Jetpack Compose,
single `app` module). Users either run a simple repeating timer or build named
"plans" of sections (reps × stretch/break durations), which an audio-guided
foreground-service timer then executes with pings and TTS announcements.

App id: `dev.redfoxstudio.stretchping` (code lives under `jez.stretchping`).
minSdk 26, target/compileSdk 36.

## Build, verify, run

```bash
./gradlew :app:compileDebugKotlin   # fastest compile check — run after every change
./gradlew assembleDebug             # full debug APK
./gradlew lint                      # android lint
./gradlew testDebugUnitTest         # unit tests (currently only placeholders)
./gradlew installDebug              # deploy to connected device/emulator
```

Gotchas:
- A device with the Play-Store (release-signed) build installed will reject
  `installDebug` with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Don't uninstall the
  user's app to work around it (their plans live in DataStore); use an emulator.
- Release versioning is manual: bump `versionCode`/`versionName` in
  `app/build.gradle` (history convention: a dedicated
  "updated version code for release" commit).
- Kotlin is 1.9.x with kapt (Hilt) and a pinned `composeOptions`
  `kotlinCompilerExtensionVersion` — Kotlin upgrades require the Compose
  compiler plugin migration first (see docs/MAINTENANCE.md).

## Layout

```
app/src/main/java/jez/stretchping/
  features/<feature>/   one package per screen: activetimer, edittimer, home,
                        planner, planslist, settings
  service/              ActiveTimerService (foreground) + controller/dispatcher
  persistence/          SettingsRepository (DataStore), ExerciseConfig models
  audio/                SoundManager (SoundPool pings), TTSManager
  notification/         NotificationsHelper
  ui/components/        shared composables (RadialPicker, TimerControls, …)
  ui/theme/             Material3 theme; dynamic color on Android 12+
  utils/                small helpers (toViewState, toFlooredInt, IdProvider)
docs/                   IDEAS.md (brainstorm capture), MAINTENANCE.md (audit & plan)
assets/                 Play Store artwork sources (not bundled in the app)
```

## Architecture: unidirectional feature pattern

Every screen follows the same shape (planner is the reference example):

1. `<Feature>UIEvent` — sealed interface of user intents.
2. `<Feature>VM` — `@HiltViewModel`, implements `Consumer<<Feature>UIEvent>`.
   Holds a private `MutableStateFlow<State>` (internal model, never exposed).
3. `<Feature>EventToState` — reducer: `(State, UIEvent) -> State`, pure.
4. `<Feature>StateToViewState` — pure mapper to a UI-facing `ViewState`.
5. `<Feature>ViewState` — immutable data class rendered by the screen.
6. `<Feature>Screen` — composable taking the VM; collects
   `viewModel.viewState.collectAsState()` and sends events back via
   `rememberEventConsumer(viewModel)`.

The VM wires 2→5 with the `toViewState(scope, initial) { mapper }` extension
(`utils/MutableStateFlowtoViewState.kt`). Side effects (navigation,
persistence) happen in the VM's `accept()`, not in reducers or mappers.

When adding a screen: create the package with those files, add a `Route`
subclass (sealed class in `NavigationDispatcher.kt` — params are serialized
into the nav route, complex configs as kotlinx-serialization JSON), and
register a composable destination in `MainActivity`'s NavHost. Navigate by
injecting `NavigationDispatcher` and calling `navigateTo(Route.X)`;
`Route.Back` pops.

`home` and `settings` predate the full pattern (no separate ViewState/reducer
files); prefer the full shape for new screens.

## Active timer subsystem

`ActiveTimerService` is a `mediaPlayback` foreground service owning an
`ActiveTimerEngine` — the same event→command→state pattern as VMs
(`EventToCommand`, `ActiveTimerStateUpdater`), plus an
`EventScheduler` that schedules pings/TTS for upcoming segment boundaries.
The screen binds to the service via `ActiveTimerServiceDispatcher` and feeds
events to the engine. Timer logic under `features/activetimer/logic/` is pure
JVM and the best target for unit tests.

## Persistence

`SettingsRepository` (@Singleton) wraps a Preferences DataStore: theme, timer
defaults, ping settings, and the list of exercise plans (`ExerciseConfig`,
@Serializable, stored as a JSON string, cached in a `MutableStateFlow`).
Writes happen on edit; there is no separate "save" step in the UI.

## Conventions

- Naming: `<Feature>VM`, `<Feature>UIEvent`, `<Feature>ViewState`,
  `<Feature>EventToState`, `<Feature>StateToViewState`.
- All user-visible text goes through `res/values/strings.xml` — no hardcoded
  literals in composables (currently at 100%; keep it that way).
- Interactive elements need `contentDescription`s (or explicit `null` for
  decorative icons).
- Use scheme colors from `MaterialTheme.colorScheme` only — dynamic color must
  keep working; never hardcode hex values in composables.
- Add a `@Preview` composable per screen/major component, wrapped in
  `StretchPingTheme`.
- `.idea/` is untracked; don't commit IDE config.
- Commit messages: short, lowercase, descriptive (see `git log`).

## Where to look next

- `docs/IDEAS.md` — captured future directions; add ideas there, prune freely.
- `docs/MAINTENANCE.md` — known issues, audit findings, and upgrade plan.
