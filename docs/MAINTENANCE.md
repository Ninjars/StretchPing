# Maintenance Plan

Findings from a full codebase audit (2026-07-06): correctness review of all 62
source files plus a build/architecture/setup review. Ranked by priority.
Strike items through or delete them as they're resolved; re-audit after the
P0s land. Paths are relative to `app/src/main/java/jez/stretchping/`.

## P0 — data loss & broken core flows

1. **Plan wipe race in `persistence/SettingsRepository.kt` (~64, 168-186).**
   `cachedExercises.compareAndSet(ExerciseConfigs(emptyList()), it)` drops disk
   emissions once the cache is non-empty. If `saveExercise` runs before the
   initial DataStore read completes (cold start → quick plan edit), the write
   is built from the empty cache and **all previously saved plans are wiped**.
   Fix: read current prefs inside the `edit {}` block rather than trusting the
   in-memory cache. (S)

2. **Leaked collectors in `features/planner/PlannerVM.kt` (~80-86).** Every
   `accept()` launches a new, never-cancelled `mutableState.collect {}` that
   calls `saveExercise`. N events → N live collectors → O(N²) DataStore writes
   per session, and a leaked collector can re-save a plan *after*
   `DeletePlanClicked`, resurrecting it. Fix: single collector in `init`, or
   save explicitly per event. (S)

3. **Rotation restarts the workout — `MainActivity.kt` (~75-80).** `onDestroy()`
   calls `stopService()` unconditionally; rotation destroys the service and
   engine, then the surviving VM rebinds, creates a fresh engine, and
   auto-starts from segment 0 (`ActiveTimerVM.kt` ~114-116). Fix: only stop
   the service when `isFinishing`. (S)

## P1 — correctness

4. **Ineffective CAS event handling** — `ActiveTimerEngine.kt` (~87-94) and
   `PlannerVM.kt` (~74-78): `compareAndSet(mutableState.value, …)` re-reads at
   call time so it "succeeds" on stale snapshots; concurrent events on
   `Dispatchers.Default` can overwrite each other (dropped keystrokes in the
   planner), and side effects run regardless. Fix: `MutableStateFlow.update {}`
   or a single-threaded event channel. (M)
5. **Timer drift / doze stalls** — `features/activetimer/logic/EventScheduler.kt`
   (~98): chained relative `delay()`s accumulate latency and aren't
   doze-exempt; pings drift or stall with the screen off despite the FGS.
   Fix: anchor scheduling to absolute `endAtTime` + hold a partial wake lock
   while running. (M)
6. **Service connection leak** — `service/ActiveTimerServiceController.kt`
   (~50-57): `unbind()` skips `unbindService` when binding hasn't completed
   yet → leaked ServiceConnection and double-bind on next resume. Track a
   `bindRequested` flag. (S)
7. **Unencoded JSON in nav routes** — `NavigationDispatcher.kt` (~21-24) +
   `MainActivity.kt` (~91): `Route.ActiveTimer` embeds raw JSON (including
   user-entered plan/section names) in the route; `/`, `?`, `#` in a name
   breaks matching. URL-encode the argument. (S)
8. **Silent unsaved edits** — `PlannerVM` gates persistence on `canStart`;
   after deleting all sections or clearing a rep count, further edits (even
   the plan name) silently don't save. Decide: save always, or surface the
   invalid state in the UI. (S/M)
9. **Text field desync** — `ui/components/SelectOnFocusTextField.kt` (~39-53)
   resyncs its local `TextFieldValue` from upstream during composition; with
   async event handling, fast typing can transiently revert (permanent drops
   when combined with item 4). Revisit after item 4; consider state-hoisting
   or `TextFieldState`. (M)

## P2 — platform & toolchain

10. **Edge-to-edge is broken under targetSdk 36** — `ui/theme/Theme.kt`
    (117-118) uses deprecated `window.statusBarColor`/`navigationBarColor`
    (no-ops with enforced edge-to-edge). Adopt `enableEdgeToEdge()` +
    inset-aware layouts. (S, high value)
11. **Kotlin 2 migration** — Kotlin 1.9.24 + kapt + pinned
    `composeOptions.kotlinCompilerExtensionVersion`. Move to Kotlin 2.x,
    `org.jetbrains.kotlin.plugin.compose`, and Hilt via KSP (Hilt 2.51 is also
    stale). Caps AGP/library upgrades until done. (M)
12. **Dependency refresh** — Compose BOM 2024.06.00 (~2 years old),
    navigation-compose 2.7.7, core-ktx 1.13.1, datastore 1.1.1. Accompanist
    permissions 0.34.0 is deprecated but has no stable AndroidX replacement —
    keep, note it. Do after 11. (S/M)
13. **Version catalog + kts** — versions are split between `buildscript.ext`
    and inline strings (serialization plugin version declared twice). Migrate
    to `gradle/libs.versions.toml`; `.kts` conversion optional at the same
    time. (M)
14. **Minor build hygiene** — `android.nonFinalResIds=false` in
    gradle.properties (legacy workaround; try removing), stale
    `tools:targetApi="31"` in the manifest. (S)

## Testing (currently only placeholder tests)

All prime targets are pure JVM — plain JUnit + kotlinx-coroutines-test, no
Robolectric needed. In order of value:

1. `features/activetimer/logic/ActiveTimerStateUpdater` — core timer state
   machine (pause/resume math is subtle and currently correct; lock it in).
2. `features/activetimer/logic/EventToCommandStateMapper`.
3. `features/planner/PlannerEventToState` + `PlannerStateToViewState`.
4. `features/edittimer/EventToSettingsUpdate` and
   `utils/KotlinUtils.toFlooredInt` (good first test).

P0 fixes 1-2 should land with regression tests (repository save/read race,
single-collector behavior).

## CI

None exists. Minimal GitHub Actions workflow:
`./gradlew lint testDebugUnitTest assembleDebug` on PR + main. Land alongside
the first real tests so it's guarding something. (S)

## Consistency & structure (opportunistic)

- `features/home` and `features/settings` predate the standard
  VM/EventToState/StateToViewState pattern — align when next touched.
- `PlannerScreen.kt` (~600 lines) contains a private dialog + picker field
  that overlap `ui/components/` equivalents; extract/unify.
- `MainActivity.kt` mixes nav graph, animated title, theming, and a
  `GlobalScope.launch` (title animation — unowned; move to a lifecycle scope).
- `features/planslist/PlansListVM.kt` (~62) throws on a missing plan id;
  degrade gracefully.
- `audio/TTSManager` — `initialise()` leaks the prior engine on re-init;
  `destroy()` before init throws; early `announce()` no-ops silently. (Low)
- Localization: strings are fully resource-based (keep it that way); adding
  locales is straightforward whenever desired.
- `Readme.md` is threadbare — a screenshot + feature list + link to AGENTS.md
  would do.

## Verified fine — don't "fix"

- The `from.index - 1` reorder offset in `PlannerScreen` is correct for
  reorderable 2.3.0 (moves are only reported between registered items).
- Pause/resume fraction rescaling in `ActiveTimerStateUpdater` and
  `CountdownTimer`'s keyed `Animatable` resync.
- `toFlooredInt`'s `Int.MIN_VALUE` empty-input sentinel is consistently
  clamped/mapped at every call site.
- Signing/secrets hygiene: nothing sensitive is committed; no signing config
  in the repo.

## Suggested sequence

1. P0 items 1-3 with regression tests (one small PR each).
2. CI + the first logic tests (timer state machine).
3. Edge-to-edge fix (10), then the Kotlin 2 / catalog / dependency train
   (11 → 13 → 12).
4. P1 items 4-9, letting 4 land before 9.
5. Consistency items as drive-bys when touching those files.
