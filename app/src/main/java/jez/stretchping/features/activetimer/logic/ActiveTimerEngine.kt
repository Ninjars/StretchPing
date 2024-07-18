package jez.stretchping.features.activetimer.logic

import androidx.core.util.Consumer
import jez.stretchping.NavigationDispatcher
import jez.stretchping.Route
import jez.stretchping.features.activetimer.ActiveTimerVM
import jez.stretchping.features.activetimer.ExerciseConfig
import jez.stretchping.features.activetimer.view.ActiveTimerStateToViewState
import jez.stretchping.features.activetimer.view.ActiveTimerViewState
import jez.stretchping.service.ActiveTimerServiceProvider
import jez.stretchping.utils.toViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActiveTimerEngine(
    private val eventScheduler: EventScheduler,
    private val navigationDispatcher: NavigationDispatcher,
    private val serviceProvider: ActiveTimerServiceProvider,
    exerciseConfig: ExerciseConfig,
) : Consumer<ActiveTimerVM.Event> {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

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
            scope = coroutineScope,
        ) { state -> ActiveTimerStateToViewState(state) }

    override fun accept(event: ActiveTimerVM.Event) {
        coroutineScope.launch {
            val currentState = mutableState.value
            val command = EventToCommand(currentState, event)
            val newActiveState = ActiveTimerStateUpdater(currentState.activeState, command)

            mutableState.compareAndSet(
                mutableState.value,
                currentState.copy(activeState = newActiveState)
            )

            command?.let {
                eventScheduler.planFutureActions(
                    coroutineScope = this,
                    eventsConfiguration = EventsConfiguration(
                        currentState.activePings,
                        currentState.transitionPings
                    ),
                    executedCommand = it,
                    eventConsumer = this@ActiveTimerEngine
                )

                when (it) {
                    is Command.GoBack,
                    is Command.SequenceCompleted ->
                        withContext(Dispatchers.Main) {
                            navigationDispatcher.navigateTo(Route.Back)
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
