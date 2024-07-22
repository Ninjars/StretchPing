package jez.stretchping.features.edittimer

import androidx.core.util.Consumer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jez.stretchping.NavigationDispatcher
import jez.stretchping.Route
import jez.stretchping.features.activetimer.ExerciseConfig
import jez.stretchping.persistence.SettingsRepository
import jez.stretchping.persistence.ThemeMode
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
    private val settingsState = combine(
        settingsRepository.repCount,
        settingsRepository.activityDuration,
        settingsRepository.transitionDuration,
        settingsRepository.activePingsCount,
        settingsRepository.transitionPingsCount,
    ) { repCount, activityDuration, transitionDuration, activePingsCount, transitionPingsCount ->
        CombinedSettings(
            repCount = repCount,
            activityDuration = activityDuration,
            transitionDuration = transitionDuration,
            activePingsCount = activePingsCount,
            transitionPingsCount = transitionPingsCount,
        )
    }
    private val state = combine(
        settingsState,
        settingsRepository.themeMode,
        settingsRepository.playInBackground,
    ) { settingsState, themeMode, playInBackground ->
        State(
            repCount = settingsState.repCount,
            activeSegmentLength = settingsState.activityDuration,
            transitionLength = settingsState.transitionDuration,
            transitionPings = settingsState.transitionPingsCount,
            activePings = settingsState.activePingsCount,
            playInBackground = playInBackground,
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
                with (state.value) {
                    Route.ActiveTimer(
                        config = ExerciseConfig(
                            repCount = repCount,
                            activityDuration = activeSegmentLength,
                            transitionDuration = transitionLength,
                            activePingsCount = activePings,
                            transitionPingsCount = transitionPings,
                            playInBackground = playInBackground,
                        )
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
        val transitionLength: Int,
        val activePings: Int,
        val transitionPings: Int,
        val playInBackground: Boolean,
        val themeMode: ThemeMode,
    ) {
        companion object {
            val Default = State(
                repCount = 0,
                activeSegmentLength = 0,
                transitionLength = 0,
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
