package jez.stretchping.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    Unset, System, Light, Dark;

    companion object {
        val displayValues = listOf(System, Light, Dark)
    }
}

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    private var cachedExercises = MutableStateFlow(ExerciseConfigs(emptyList()))

    private val activityDuration: Flow<Int> = context.dataStore.data
        .map {
            it[ActivityDurationPref] ?: 30
        }

    init {
        // pre-cache exercise configs
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                context.dataStore.data.map {
                    it[ExercisesPref]?.let { json ->
                        Json.decodeFromString(
                            json
                        )
                    } ?: ExerciseConfigs(emptyList())
                }
                    .collect {
                        cachedExercises.compareAndSet(ExerciseConfigs(emptyList()), it)
                    }
            }
        }
    }

    suspend fun setActivityDuration(durationSeconds: Int) =
        editDataStore {
            it[ActivityDurationPref] = durationSeconds
        }

    private val transitionDuration: Flow<Int> = context.dataStore.data
        .map {
            it[TransitionDurationPref] ?: 5
        }

    suspend fun setTransitionDuration(durationSeconds: Int) =
        editDataStore {
            it[TransitionDurationPref] = durationSeconds
        }

    private val repCount: Flow<Int> = context.dataStore.data
        .map {
            it[RepCountPref] ?: -1
        }

    suspend fun setRepCount(count: Int) =
        editDataStore {
            it[RepCountPref] = count
        }

    private val transitionPingsCount: Flow<Int> = context.dataStore.data
        .map {
            it[TransitionPingsPref] ?: 3
        }

    suspend fun setTransitionPingsCount(count: Int) =
        editDataStore {
            it[TransitionPingsPref] = count
        }

    private val activePingsCount: Flow<Int> = context.dataStore.data
        .map {
            it[ActivePingsPref] ?: 5
        }

    suspend fun setActivePingsCount(count: Int) =
        editDataStore {
            it[ActivePingsPref] = count
        }

    private val playInBackground: Flow<Boolean> = context.dataStore.data
        .map {
            it[PlayInBackgroundPref] ?: false
        }

    suspend fun setPlayInBackground(shouldPause: Boolean) =
        editDataStore {
            it[PlayInBackgroundPref] = shouldPause
        }

    val engineSettings: Flow<EngineSettings> =
        combine(
            activePingsCount,
            transitionPingsCount,
            playInBackground,
        ) { activePings, transitionPings, playInBackground ->
            EngineSettings(activePings, transitionPings, playInBackground)
        }

    val simpleTimerConfig: Flow<TimerConfig> =
        combine(
            repCount,
            activityDuration,
            transitionDuration,
        ) { repCount, activityDuration, transitionDuration ->
            TimerConfig(
                repCount,
                activityDuration,
                transitionDuration,
            )
        }

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map {
            it[ThemePref]?.toThemeMode() ?: ThemeMode.System
        }

    suspend fun setThemeMode(mode: ThemeMode) =
        editDataStore {
            it[ThemePref] = mode.toInt()
        }

    val exerciseConfigs: Flow<ExerciseConfigs> = cachedExercises

    suspend fun saveExercise(exerciseConfig: ExerciseConfig) {
        editDataStore { prefs ->
            val current = cachedExercises.value
            val exercises = current.exercises

            val existingIndex =
                exercises.indexOfFirst { ex -> ex.exerciseId == exerciseConfig.exerciseId }
            val newExercises = if (existingIndex < 0) {
                exercises + exerciseConfig
            } else {
                exercises.toMutableList().apply {
                    this[existingIndex] = exerciseConfig
                }
            }

            val newConfig = current.copy(exercises = newExercises)
            prefs[ExercisesPref] = Json.encodeToString(newConfig)
            cachedExercises.value = newConfig
        }
    }

    private suspend fun editDataStore(func: (MutablePreferences) -> Unit) =
        withContext(Dispatchers.IO) {
            context.dataStore.edit {
                func(it)
            }
        }


    private fun ThemeMode.toInt() = when (this) {
        ThemeMode.Unset -> 0
        ThemeMode.System -> 1
        ThemeMode.Light -> 2
        ThemeMode.Dark -> 3
    }

    private fun Int.toThemeMode() = when (this) {
        1 -> ThemeMode.System
        2 -> ThemeMode.Light
        3 -> ThemeMode.Dark
        else -> ThemeMode.Unset
    }

    private companion object {
        val ThemePref = intPreferencesKey("ThemePref")
        val ActivityDurationPref = intPreferencesKey("ActivityDuration")
        val TransitionDurationPref = intPreferencesKey("TransitionDuration")
        val RepCountPref = intPreferencesKey("RepCountDuration")
        val TransitionPingsPref = intPreferencesKey("TransitionPings")
        val ActivePingsPref = intPreferencesKey("ActivePings")
        val PlayInBackgroundPref = booleanPreferencesKey("PlayInBackground")
        val ExercisesPref = stringPreferencesKey("Plans")
    }
}
