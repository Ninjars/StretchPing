package jez.stretchping.features.activetimer.logic

import androidx.core.util.Consumer
import jez.stretchping.NavigationDispatcher
import jez.stretchping.Route
import jez.stretchping.features.activetimer.ActiveTimerVM
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.State.ActiveSegment
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.State.SegmentSpec
import jez.stretchping.features.activetimer.view.ActiveTimerStateToViewState
import jez.stretchping.features.activetimer.view.ActiveTimerViewState
import jez.stretchping.persistence.EngineSettings
import jez.stretchping.persistence.ExerciseConfig
import jez.stretchping.utils.getIf
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
    private val engineSettings: EngineSettings,
    exerciseConfig: ExerciseConfig,
) : Consumer<ActiveTimerVM.Event> {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val eventToCommand = EventToCommand(exerciseConfig.repeat)

    private val mutableState = MutableStateFlow(
        with(exerciseConfig.parse()) {
            State(
                hasStarted = false,
                segments = this,
                index = 0,
                activeSegment = null,
                completedReps = 0,
            )
        }
    )

    val viewState: StateFlow<ActiveTimerViewState> =
        mutableState.toViewState(
            scope = coroutineScope,
        ) { state -> ActiveTimerStateToViewState(state) }

    private fun ExerciseConfig.parse() =
        sections.flatMapIndexed { index, section ->
            val hasIntro = section.introDuration > 0 || section.name.isNotBlank()
            listOfNotNull(
                getIf(hasIntro) {
                    SegmentSpec.Announcement(
                        name = section.name,
                        durationSeconds = section.introDuration,
                    )
                },
            ) + (0 until section.repCount).flatMap {
                val hasTransition = (!hasIntro || it > 0) && section.transitionDuration > 0
                listOfNotNull(
                    getIf(hasTransition) {
                        SegmentSpec.Transition(
                            name = section.name,
                            durationSeconds = section.transitionDuration,
                            index = it,
                            repCount = section.repCount,
                            isStartOfSegment = it == 0 && !hasIntro,
                        )
                    },
                    getIf(section.activityDuration > 0) {
                        SegmentSpec.Stretch(
                            name = section.name,
                            durationSeconds = section.activityDuration,
                            index = it,
                            isLast = !repeat && index == sections.size - 1 && it == section.repCount - 1,
                            repCount = section.repCount,
                            isStartOfSegment = it == 0 && !hasIntro && !hasTransition,
                        )
                    },
                )
            }
        }

    override fun accept(value: ActiveTimerVM.Event) {
        coroutineScope.launch {
            val currentState = mutableState.value
            val command = eventToCommand(currentState, value)

            mutableState.compareAndSet(
                mutableState.value,
                ActiveTimerStateUpdater(currentState, command),
            )

            command?.let {
                eventScheduler.planFutureActions(
                    coroutineScope = this,
                    eventsConfiguration = EventsConfiguration(
                        engineSettings.activePingsCount,
                        engineSettings.transitionPingsCount,
                    ),
                    executedCommand = it,
                    eventConsumer = this@ActiveTimerEngine
                )

                when (it) {
                    is Command.GoBack,
                    is Command.SequenceCompleted -> {
                        withContext(Dispatchers.Main) {
                            navigationDispatcher.navigateTo(Route.Back)
                            onEndCallback()
                        }
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
            val index: Int,
            val segmentSpec: SegmentSpec,
        ) : Command()

        data class RepeatExercise(
            val startMillis: Long,
            val segmentSpec: SegmentSpec,
        ) : Command()

        data class ResumeSegment(
            val startMillis: Long,
            val startFraction: Float,
            val pausedSegment: ActiveSegment,
        ) : Command()

        data class PauseSegment(
            val pauseMillis: Long,
            val runningSegment: ActiveSegment,
        ) : Command()

        data object GoBack : Command()
        data object SequenceCompleted : Command()
    }

    data class State(
        val hasStarted: Boolean,
        val segments: List<SegmentSpec>,
        val index: Int,
        val activeSegment: ActiveSegment?,
        val completedReps: Int,
    ) {
        data class ActiveSegment(
            val startedAtTime: Long,
            val startedAtFraction: Float,
            val endAtTime: Long,
            val pausedAtFraction: Float?,
            val pausedAtTime: Long?,
            val spec: SegmentSpec,
        ) {
            val remainingDurationMillis = endAtTime - (pausedAtTime ?: startedAtTime)
        }

        sealed interface SegmentSpec {
            val name: String?
            val durationSeconds: Int
            val isLast: Boolean
            val position: String
            val isStartOfSegment: Boolean

            data class Announcement(
                override val name: String?,
                override val durationSeconds: Int,
            ) : SegmentSpec {
                override val isLast = false
                override val isStartOfSegment = true
                override val position: String = ""
            }

            data class Transition(
                override val name: String?,
                override val durationSeconds: Int,
                override val isStartOfSegment: Boolean,
                val index: Int,
                val repCount: Int,
            ) : SegmentSpec {
                override val isLast = false
                override val position: String = if (repCount > 1) "${index + 1} / $repCount" else ""
            }

            data class Stretch(
                override val name: String?,
                override val durationSeconds: Int,
                override val isLast: Boolean,
                override val isStartOfSegment: Boolean,
                val index: Int,
                val repCount: Int,
            ) : SegmentSpec {
                override val position: String = if (repCount > 1) "${index + 1} / $repCount" else ""
            }
        }
    }
}
