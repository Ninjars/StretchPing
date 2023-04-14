package jez.stretchping.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    Unset, System, Light, Dark;

    companion object {
        val displayValues = listOf(System, Light, Dark)
    }
}

@Singleton
class Settings @Inject constructor(@ApplicationContext private val context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map {
            it[ThemePref]?.toThemeMode() ?: ThemeMode.System
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit {
            it[ThemePref] = mode.toInt()
        }
    }

    val activityDuration: Flow<Int> = context.dataStore.data
        .map {
            it[ActivityDurationPref] ?: 30
        }

    suspend fun setActivityDuration(durationSeconds: Int) {
        context.dataStore.edit {
            it[ActivityDurationPref] = durationSeconds
        }
    }

    val transitionDuration: Flow<Int> = context.dataStore.data
        .map {
            it[TransitionDurationPref] ?: 5
        }

    suspend fun setTransitionDuration(durationSeconds: Int) {
        context.dataStore.edit {
            it[TransitionDurationPref] = durationSeconds
        }
    }

    val repCount: Flow<Int> = context.dataStore.data
        .map {
            it[RepCountPref] ?: -1
        }

    suspend fun setRepCount(count: Int) {
        context.dataStore.edit {
            it[RepCountPref] = count
        }
    }

    val transitionPingsCount: Flow<Int> = context.dataStore.data
        .map {
            it[TransitionPingsPref] ?: 3
        }

    suspend fun setTransitionPingsCount(count: Int) {
        context.dataStore.edit {
            it[TransitionPingsPref] = count
        }
    }

    val activePingsCount: Flow<Int> = context.dataStore.data
        .map {
            it[ActivePingsPref] ?: 5
        }

    suspend fun setActivePingsCount(count: Int) {
        context.dataStore.edit {
            it[ActivePingsPref] = count
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
    }
}
