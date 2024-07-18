package jez.stretchping.features.activetimer.logic

import jez.stretchping.features.activetimer.ActiveTimerVM.Event
import jez.stretchping.features.activetimer.ExerciseConfig
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.ActiveState
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.Command
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.State

internal object EventToCommand : (ExerciseConfig, State, Event) -> Command? {
    override fun invoke(exerciseConfig: ExerciseConfig, state: State, event: Event): Command? {
        val activeSegment = state.activeState.activeSegment
        return when (event) {
            is Event.Pause ->
                if (activeSegment == null || activeSegment.pausedAtFraction != null) {
                    null
                } else {
                    Command.PauseSegment(
                        pauseMillis = System.currentTimeMillis(),
                        runningSegment = activeSegment,
                    )
                }

            is Event.Start ->
                when {
                    activeSegment != null -> resume(exerciseConfig, state, activeSegment)
                    else -> start(exerciseConfig, state)
                }

            is Event.OnSectionCompleted -> if (
                isAtEnd(
                    exerciseConfig.repCount,
                    state.activeState.repeatsCompleted,
                    state.activeState.queuedSegments
                )
            ) {
                Command.SequenceCompleted
            } else {
                start(exerciseConfig, state)
            }

            is Event.BackPressed -> Command.GoBack
        }
    }

    private fun start(exerciseConfig: ExerciseConfig, state: State): Command {
        var isNewRep = false
        val currentSegments =
            state.activeState.queuedSegments.takeIf { it.isNotEmpty() }
                ?: exerciseConfig.createSegments().also {
                    isNewRep = true
                }
        val nextSegment = currentSegments.first()
        val queuedSegments = currentSegments.drop(1)
        val isLast =
            isAtEnd(exerciseConfig.repCount, state.activeState.repeatsCompleted, queuedSegments)
        return Command.StartSegment(
            System.currentTimeMillis(),
            nextSegment,
            queuedSegments,
            isNewRep,
            isLast,
        )
    }

    private fun resume(
        exerciseConfig: ExerciseConfig,
        state: State,
        activeSegment: ActiveState.ActiveSegment
    ): Command? =
        if (activeSegment.pausedAtFraction == null) {
            null
        } else {
            val isLast = isAtEnd(
                exerciseConfig.repCount,
                state.activeState.repeatsCompleted,
                state.activeState.queuedSegments
            )
            Command.ResumeSegment(
                startMillis = System.currentTimeMillis(),
                startFraction = activeSegment.pausedAtFraction,
                pausedSegment = activeSegment,
                isLastSegment = isLast,
            )
        }

    private fun isAtEnd(
        repCount: Int,
        completedReps: Int,
        remainingSegments: List<ActiveState.SegmentSpec>,
    ) = repCount > 0 && completedReps == repCount - 1 && remainingSegments.isEmpty()

    private fun ExerciseConfig.createSegments(): List<ActiveState.SegmentSpec> =
        listOf(
            ActiveState.SegmentSpec(
                durationSeconds = transitionDuration,
                mode = ActiveState.SegmentSpec.Mode.Transition,
            ),
            ActiveState.SegmentSpec(
                durationSeconds = activityDuration,
                mode = ActiveState.SegmentSpec.Mode.Stretch,
            ),
        )
}
