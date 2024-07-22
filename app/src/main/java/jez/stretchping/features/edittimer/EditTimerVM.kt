package jez.stretchping.features.edittimer

import androidx.core.util.Consumer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jez.stretchping.NavigationDispatcher
import jez.stretchping.Route
import jez.stretchping.features.activetimer.ExerciseConfig
import jez.stretchping.persistence.EngineSettings
import jez.stretchping.persistence.SettingsRepository
import jez.stretchping.persistence.ThemeMode
import jez.stretchping.persistence.TimerConfig
import jez.stretchping.utils.toViewState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTimerVM @Inject constructor(
    private val navigationDispatcher: NavigationDispatcher,
    private val settingsRepository: SettingsRepository,
) : Consumer<EditTimerEvent>, ViewModel() {
    private val state = combine(
        settingsRepository.engineSettings,
        settingsRepository.simpleTimerConfig,
        settingsRepository.themeMode,
    ) { engineSettings, timerConfig, themeMode ->
        State(
            repCount = timerConfig.repCount,
            activeSegmentLength = timerConfig.activityDuration,
            transitionDuration = timerConfig.transitionDuration,
            transitionPings = engineSettings.transitionPingsCount,
            activePings = engineSettings.activePingsCount,
            playInBackground = engineSettings.playInBackground,
            themeMode = themeMode,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        State.Default
    )

    val viewState: StateFlow<EditTimerViewState> =
        state.toViewState(
            scope = viewModelScope,
            initial = State.Default
        ) { state -> EditTimerStateToViewState(state) }

    override fun accept(event: EditTimerEvent) {
        when (event) {
            EditTimerEvent.Start -> navigationDispatcher.navigateTo(
                with(state.value) {
                    Route.ActiveTimer(
                        config = ExerciseConfig(
                            engineSettings = EngineSettings(
                                activePingsCount = activePings,
                                transitionPingsCount = transitionPings,
                                playInBackground = playInBackground,
                            ),
                            timerConfig = TimerConfig(
                                repCount = repCount,
                                activityDuration = activeSegmentLength,
                                transitionDuration = transitionDuration,
                            ),
                        ),
                    )
                }
            )

            else ->
                EventToSettingsUpdate(event)?.let {
                    viewModelScope.launch {
                        updateSettings(it)
                    }
                }
        }
    }

    private suspend fun updateSettings(command: SettingsCommand) {
        when (command) {
            is SettingsCommand.SetThemeMode ->
                settingsRepository.setThemeMode(command.mode)

            is SettingsCommand.SetActivityDuration ->
                settingsRepository.setActivityDuration(command.duration)

            is SettingsCommand.SetTransitionDuration ->
                settingsRepository.setTransitionDuration(command.duration)

            is SettingsCommand.SetRepCount ->
                settingsRepository.setRepCount(command.count)

            is SettingsCommand.SetActivePings ->
                settingsRepository.setActivePingsCount(command.count)

            is SettingsCommand.SetTransitionPings ->
                settingsRepository.setTransitionPingsCount(command.count)

            is SettingsCommand.SetPlayInBackground ->
                settingsRepository.setPlayInBackground(command.enabled)
        }
    }

    data class State(
        val repCount: Int,
        val activeSegmentLength: Int,
        val transitionDuration: Int,
        val activePings: Int,
        val transitionPings: Int,
        val playInBackground: Boolean,
        val themeMode: ThemeMode,
    ) {
        companion object {
            val Default = State(
                repCount = 0,
                activeSegmentLength = 0,
                transitionDuration = 0,
                activePings = 0,
                transitionPings = 0,
                playInBackground = false,
                themeMode = ThemeMode.Unset,
            )
        }
    }

    private data class CombinedSettings(
        val repCount: Int,
        val activityDuration: Int,
        val transitionDuration: Int,
        val activePingsCount: Int,
        val transitionPingsCount: Int,
    )

    sealed class SettingsCommand {
        data class SetThemeMode(val mode: ThemeMode) : SettingsCommand()
        data class SetActivityDuration(val duration: Int) : SettingsCommand()
        data class SetTransitionDuration(val duration: Int) : SettingsCommand()
        data class SetRepCount(val count: Int) : SettingsCommand()
        data class SetActivePings(val count: Int) : SettingsCommand()
        data class SetTransitionPings(val count: Int) : SettingsCommand()
        data class SetPlayInBackground(val enabled: Boolean) : SettingsCommand()
    }
}
