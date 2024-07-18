package jez.stretchping.features.activetimer

import androidx.core.util.Consumer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jez.stretchping.NavigationDispatcher
import jez.stretchping.Route
import jez.stretchping.features.activetimer.logic.ActiveTimerStateUpdater
import jez.stretchping.features.activetimer.logic.EventScheduler
import jez.stretchping.features.activetimer.logic.EventToCommand
import jez.stretchping.features.activetimer.logic.EventsConfiguration
import jez.stretchping.features.activetimer.view.ActiveTimerStateToViewState
import jez.stretchping.features.activetimer.view.ActiveTimerViewState
import jez.stretchping.service.ActiveTimerServiceProvider
import jez.stretchping.utils.toViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class ExerciseConfig(
    val repCount: Int,
    val activityDuration: Int,
    val transitionDuration: Int,
    val activePingsCount: Int,
    val transitionPingsCount: Int,
    val playInBackground: Boolean,
)

@HiltViewModel
class ActiveTimerVM @Inject constructor(
    private val eventScheduler: EventScheduler,
    private val navigationDispatcher: NavigationDispatcher,
    private val serviceProvider: ActiveTimerServiceProvider,
    savedStateHandle: SavedStateHandle,
) : Consumer<ActiveTimerVM.Event>, ViewModel(), DefaultLifecycleObserver {
    private val exerciseConfig = savedStateHandle.get<String>(Route.ActiveTimer.routeConfig)!!
        .let { Json.decodeFromString<ExerciseConfig>(it) }
    private val mutableState = MutableStateFlow(
        State(
            activeState = ActiveState(),
            repCount = exerciseConfig.repCount,
            activeSegmentLength = exerciseConfig.activityDuration,
            transitionLength = exerciseConfig.transitionDuration,
            transitionPings = exerciseConfig.transitionPingsCount,
            activePings = exerciseConfig.activePingsCount,
            playInBackground = exerciseConfig.playInBackground,
        )
    )

    val viewState: StateFlow<ActiveTimerViewState> =
        mutableState.toViewState(
            scope = viewModelScope,
        ) { state -> ActiveTimerStateToViewState(state) }

    init {
        accept(Event.Start)
    }

    override fun accept(event: Event) {
        val currentState = mutableState.value
        viewModelScope.launch {
            val command = EventToCommand(currentState, event)
            val newActiveState = ActiveTimerStateUpdater(currentState.activeState, command)

            mutableState.compareAndSet(mutableState.value, currentState.copy(activeState = newActiveState))

            command?.let {
                eventScheduler.planFutureActions(
                    coroutineScope = this,
                    eventsConfiguration = EventsConfiguration(
                        currentState.activePings,
                        currentState.transitionPings
                    ),
                    executedCommand = it,
                    eventConsumer = this@ActiveTimerVM
                )

                when (it) {
                    is Command.GoBack,
                    is Command.SequenceCompleted -> navigationDispatcher.navigateTo(Route.Back)
                    else -> Unit
                }
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        with(mutableState.value) {
            if (!playInBackground) {
                accept(Event.Pause)
            }
        }
        super.onPause(owner)
    }

    override fun onCleared() {
        eventScheduler.dispose()
        super.onCleared()
    }

    sealed class Event {
        data object Start : Event()
        data object Pause : Event()
        data object BackPressed : Event()
        data object OnSectionCompleted : Event()
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

        data object GoBack : Command()
        data object SequenceCompleted : Command()
    }

    data class State(
        val activeState: ActiveState = ActiveState(),
        val repCount: Int,
        val activeSegmentLength: Int,
        val transitionLength: Int,
        val activePings: Int,
        val transitionPings: Int,
        val playInBackground: Boolean,
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
