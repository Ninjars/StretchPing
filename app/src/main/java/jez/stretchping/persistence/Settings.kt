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
    System, Light, Dark,
}

@Singleton
class Settings @Inject constructor(@ApplicationContext private val context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map {
            (it[ThemePref] ?: 0).toThemeMode()
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit {
            it[ThemePref] = mode.toInt()
        }
    }

    private fun ThemeMode.toInt() = when (this) {
        ThemeMode.System -> 0
        ThemeMode.Light -> 1
        ThemeMode.Dark -> 2
    }

    private fun Int.toThemeMode() = when (this) {
        1 -> ThemeMode.Light
        2 -> ThemeMode.Dark
        else -> ThemeMode.System
    }

    private companion object {
        val ThemePref = intPreferencesKey("ThemePref")
    }
}
