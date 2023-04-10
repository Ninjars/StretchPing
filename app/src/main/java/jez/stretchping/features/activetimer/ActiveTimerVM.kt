package jez.stretchping.features.activetimer

import androidx.core.util.Consumer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jez.stretchping.persistence.Settings
import jez.stretchping.persistence.ThemeMode
import jez.stretchping.utils.toViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveTimerVM @Inject constructor(
    private val eventScheduler: EventScheduler,
    private val settings: Settings,
) : Consumer<ActiveTimerVM.Event>, ViewModel(), DefaultLifecycleObserver {
    private val activeStateFlow = MutableStateFlow(ActiveState())
    private val combinedState = combine(
        activeStateFlow,
        settings.repCount,
        settings.activityDuration,
        settings.transitionDuration,
        settings.themeMode
    ) { activeState, repCount, activityDuration, transitionDuration, themeMode ->
        State(
            activeState = activeState,
            repCount = repCount,
            activeSegmentLength = activityDuration,
            transitionLength = transitionDuration,
            themeMode = themeMode
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        State()
    )

    val viewState: StateFlow<ActiveTimerViewState> =
        combinedState.toViewState(
            scope = viewModelScope,
            initial = State(
                activeState = activeStateFlow.value,
                repCount = -1,
                activeSegmentLength = 0,
                transitionLength = 0,
                themeMode = ThemeMode.System
            )
        ) { state -> StateToViewState(state) }

    override fun accept(event: Event) {
        val currentState = combinedState.value
        viewModelScope.launch {
            val command = EventToCommand(currentState, event)
            val newActiveState = ActiveTimerStateUpdater(currentState.activeState, command)
            activeStateFlow.compareAndSet(activeStateFlow.value, newActiveState)

            command?.let {
                eventScheduler.planFutureActions(this, it, this@ActiveTimerVM)
            }

            EventToSettingsUpdate(event)?.let {
                updateSettings(it)
            }
        }
    }

    private suspend fun updateSettings(command: SettingsCommand) {
        when (command) {
            is SettingsCommand.SetThemeMode ->
                settings.setThemeMode(command.mode)
            is SettingsCommand.SetActivityDuration ->
                settings.setActivityDuration(command.duration)
            is SettingsCommand.SetTransitionDuration ->
                settings.setTransitionDuration(command.duration)
            is SettingsCommand.SetRepCount ->
                settings.setRepCount(command.count)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        accept(Event.Pause)
        super.onPause(owner)
    }

    override fun onCleared() {
        eventScheduler.dispose()
        super.onCleared()
    }

    sealed class Event {
        object Start : Event()
        object Pause : Event()
        object Reset : Event()
        object OnSectionCompleted : Event()
        data class SetStretchDuration(val duration: String) : Event()
        data class SetBreakDuration(val duration: String) : Event()
        data class SetRepCount(val count: String) : Event()
        data class UpdateTheme(val themeModeIndex: Int) : Event()
    }

    sealed class SettingsCommand {
        data class SetThemeMode(val mode: ThemeMode) : SettingsCommand()
        data class SetActivityDuration(val duration: Int) : SettingsCommand()
        data class SetTransitionDuration(val duration: Int) : SettingsCommand()
        data class SetRepCount(val count: Int) : SettingsCommand()
    }

    sealed class Command {
        data class StartSegment(
            val startMillis: Long,
            val segmentSpec: ActiveState.SegmentSpec,
            val queuedSegments: List<ActiveState.SegmentSpec>,
            val isNewRep: Boolean,
        ) : Command()

        data class ResumeSegment(
            val startMillis: Long,
            val startFraction: Float,
            val pausedSegment: ActiveState.ActiveSegment,
        ) : Command()

        data class PauseSegment(
            val pauseMillis: Long,
            val runningSegment: ActiveState.ActiveSegment,
        ) : Command()

        object ResetToStart : Command()
    }

    data class State(
        val activeState: ActiveState = ActiveState(),
        val repCount: Int = -1,
        val activeSegmentLength: Int = 30,
        val transitionLength: Int = 5,
        val themeMode: ThemeMode = ThemeMode.System,
    )

    data class ActiveState(
        val queuedSegments: List<SegmentSpec> = emptyList(),
        val activeSegment: ActiveSegment? = null,
        val repeatsCompleted: Int = -1,
    ) {
        data class ActiveSegment(
            val startedAtTime: Long,
            val startedAtFraction: Float,
            val endAtTime: Long,
            val pausedAtFraction: Float?,
            val pausedAtTime: Long?,
            val spec: SegmentSpec,
        ) {
            val mode = spec.mode
            val remainingDurationMillis = endAtTime - (pausedAtTime ?: startedAtTime)
        }

        data class SegmentSpec(
            val durationSeconds: Int,
            val mode: Mode,
        ) {
            enum class Mode {
                Stretch,
                Transition,
            }
        }
    }
}
