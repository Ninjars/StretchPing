package jez.stretchping.features.activetimer.logic

import androidx.core.util.Consumer
import jez.stretchping.NavigationDispatcher
import jez.stretchping.Route
import jez.stretchping.features.activetimer.ActiveTimerVM
import jez.stretchping.features.activetimer.ExerciseConfig
import jez.stretchping.features.activetimer.view.ActiveTimerStateToViewState
import jez.stretchping.features.activetimer.view.ActiveTimerViewState
import jez.stretchping.utils.toViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActiveTimerEngine(
    private val onEndCallback: () -> Unit,
    private val eventScheduler: EventScheduler,
    private val navigationDispatcher: NavigationDispatcher,
    private val exerciseConfig: ExerciseConfig,
) : Consumer<ActiveTimerVM.Event> {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val mutableState = MutableStateFlow(
        State(
            hasStarted = false,
            activeState = ActiveState(),
        )
    )

    val viewState: StateFlow<ActiveTimerViewState> =
        mutableState.toViewState(
            scope = coroutineScope,
        ) { state -> ActiveTimerStateToViewState(exerciseConfig, state) }

    override fun accept(event: ActiveTimerVM.Event) {
        coroutineScope.launch {
            val currentState = mutableState.value
            val command = EventToCommand(exerciseConfig, currentState, event)
            val newActiveState = ActiveTimerStateUpdater(currentState.activeState, command)

            mutableState.compareAndSet(
                mutableState.value,
                currentState.copy(
                    hasStarted = currentState.hasStarted || event is ActiveTimerVM.Event.Start,
                    activeState = newActiveState
                )
            )

            command?.let {
                eventScheduler.planFutureActions(
                    coroutineScope = this,
                    eventsConfiguration = EventsConfiguration(
                        exerciseConfig.activePingsCount,
                        exerciseConfig.transitionPingsCount
                    ),
                    executedCommand = it,
                    eventConsumer = this@ActiveTimerEngine
                )

                when (it) {
                    is Command.GoBack,
                    is Command.SequenceCompleted ->
                        withContext(Dispatchers.Main) {
                            navigationDispatcher.navigateTo(Route.Back)
                            onEndCallback()
                        }

                    else -> Unit
                }
            }
        }
    }

    fun dispose() {
        eventScheduler.dispose()
        coroutineScope.cancel()
    }

    fun hasStarted() = mutableState.value.hasStarted

    sealed class Command {
        data class StartSegment(
            val startMillis: Long,
            val segmentSpec: ActiveState.SegmentSpec,
            val queuedSegments: List<ActiveState.SegmentSpec>,
            val isNewRep: Boolean,
            val isLastSegment: Boolean,
        ) : Command()

        data class ResumeSegment(
            val startMillis: Long,
            val startFraction: Float,
            val pausedSegment: ActiveState.ActiveSegment,
            val isLastSegment: Boolean,
        ) : Command()

        data class PauseSegment(
            val pauseMillis: Long,
            val runningSegment: ActiveState.ActiveSegment,
        ) : Command()

        data object GoBack : Command()
        data object SequenceCompleted : Command()
    }

    data class State(
        val hasStarted: Boolean,
        val activeState: ActiveState = ActiveState(),
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
