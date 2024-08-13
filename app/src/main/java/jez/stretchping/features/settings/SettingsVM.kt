package jez.stretchping.features.settings

import androidx.core.util.Consumer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jez.stretchping.persistence.NavLabelDisplayMode
import jez.stretchping.persistence.SettingsRepository
import jez.stretchping.persistence.ThemeMode
import jez.stretchping.utils.SystemConstants
import jez.stretchping.utils.toViewState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsVM @Inject constructor(
    private val settingsRepository: SettingsRepository,
    systemConstants: SystemConstants,
) : Consumer<SettingsScreenEvent>, ViewModel() {
    private val state = combine(
        settingsRepository.engineSettings,
        settingsRepository.themeMode,
        settingsRepository.showNavLabels,
    ) { engineSettings, themeMode, showNavLabels ->
        State(
            transitionPings = engineSettings.transitionPingsCount,
            activePings = engineSettings.activePingsCount,
            playInBackground = engineSettings.playInBackground,
            themeMode = themeMode,
            showNavLabels = showNavLabels,
            dynamicThemeEnabled = systemConstants.isDynamicThemeEnabled,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        State.Default
    )

    val viewState: StateFlow<SettingsViewState> =
        state.toViewState(
            viewModelScope,
            State.Default
        ) { state -> SettingsStateToViewState(state) }

    override fun accept(value: SettingsScreenEvent) {
        viewModelScope.launch {
            updateSettings(value)
        }
    }

    private suspend fun updateSettings(event: SettingsScreenEvent) {
        when (event) {
            is SettingsScreenEvent.UpdatePlayInBackground ->
                settingsRepository.setPlayInBackground(event.enabled)

            is SettingsScreenEvent.UpdateShowLabels ->
                settingsRepository.setShowNavLabels(NavLabelDisplayMode.displayValues[event.optionIndex])

            is SettingsScreenEvent.UpdateTheme ->
                settingsRepository.setThemeMode(ThemeMode.displayValues[event.themeModeIndex])

            is SettingsScreenEvent.UpdateActivePings ->
                settingsRepository.setActivePingsCount(event.count)

            is SettingsScreenEvent.UpdateTransitionPings ->
                settingsRepository.setTransitionPingsCount(event.count)
        }
    }

    data class State(
        val transitionPings: Int,
        val activePings: Int,
        val playInBackground: Boolean,
        val themeMode: ThemeMode,
        val showNavLabels: NavLabelDisplayMode,
        val dynamicThemeEnabled: Boolean,
    ) {
        companion object {
            val Default = State(
                transitionPings = 0,
                activePings = 0,
                playInBackground = false,
                themeMode = ThemeMode.Unset,
                showNavLabels = NavLabelDisplayMode.Unset,
                dynamicThemeEnabled = false,
            )
        }
    }
}
