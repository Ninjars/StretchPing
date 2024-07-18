package jez.stretchping.features.activetimer.logic

import jez.stretchping.features.activetimer.ActiveTimerVM.Event
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.ActiveState
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.Command
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.State

internal object EventToCommand : (State, Event) -> Command? {
    override fun invoke(state: State, event: Event): Command? {
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
                    activeSegment != null -> resume(state, activeSegment)
                    else -> start(state)
                }

            is Event.OnSectionCompleted -> if (state.isAtEnd()) {
                Command.SequenceCompleted
            } else {
                start(state)
            }

            is Event.BackPressed -> Command.GoBack
        }
    }

    private fun start(state: State): Command {
        var isNewRep = false
        val currentSegments =
            state.activeState.queuedSegments.takeIf { it.isNotEmpty() }
                ?: state.createSegments().also {
                    isNewRep = true
                }
        val nextSegment = currentSegments.first()
        val queuedSegments = currentSegments.drop(1)
        val isLast = isAtEnd(state.repCount, state.activeState.repeatsCompleted, queuedSegments)
        return Command.StartSegment(
            System.currentTimeMillis(),
            nextSegment,
            queuedSegments,
            isNewRep,
            isLast,
        )
    }

    private fun resume(state: State, activeSegment: ActiveState.ActiveSegment): Command? =
        if (activeSegment.pausedAtFraction == null) {
            null
        } else {
            val isLast = isAtEnd(
                state.repCount,
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

    private fun State.isAtEnd(): Boolean =
        isAtEnd(repCount, activeState.repeatsCompleted, activeState.queuedSegments)

    private fun isAtEnd(
        repCount: Int,
        completedReps: Int,
        remainingSegments: List<ActiveState.SegmentSpec>,
    ) = repCount > 0 && completedReps == repCount - 1 && remainingSegments.isEmpty()

    private fun State.createSegments(): List<ActiveState.SegmentSpec> =
        listOf(
            ActiveState.SegmentSpec(
                durationSeconds = transitionLength,
                mode = ActiveState.SegmentSpec.Mode.Transition,
            ),
            ActiveState.SegmentSpec(
                durationSeconds = activeSegmentLength,
                mode = ActiveState.SegmentSpec.Mode.Stretch,
            ),
        )
}
