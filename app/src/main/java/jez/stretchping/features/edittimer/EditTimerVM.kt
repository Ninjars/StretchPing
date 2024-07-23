package jez.stretchping.features.edittimer

import androidx.core.util.Consumer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jez.stretchping.NavigationDispatcher
import jez.stretchping.Route
import jez.stretchping.features.activetimer.ActiveTimerConfig
import jez.stretchping.persistence.EngineSettings
import jez.stretchping.persistence.ExerciseConfig
import jez.stretchping.persistence.SettingsRepository
import jez.stretchping.persistence.ThemeMode
import jez.stretchping.utils.toViewState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

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
                        config = ActiveTimerConfig(
                            engineSettings = EngineSettings(
                                activePingsCount = activePings,
                                transitionPingsCount = transitionPings,
                                playInBackground = playInBackground,
                            ),
                            exerciseConfig = ExerciseConfig(
                                exerciseId = "Quick Exercise",
                                exerciseName = "",
                                sections = listOf(
                                    ExerciseConfig.Section(
                                        name = "",
                                        repCount = max(1, repCount),
                                        introDuration = 0,
                                        activityDuration = activeSegmentLength,
                                        transitionDuration = transitionDuration,
                                    )
                                ),
                                repeat = repCount < 1,
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
