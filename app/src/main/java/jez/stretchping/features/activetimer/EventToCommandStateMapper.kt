package jez.stretchping.features.activetimer

import jez.stretchping.features.activetimer.ActiveTimerVM.ActiveState
import jez.stretchping.features.activetimer.ActiveTimerVM.Command
import jez.stretchping.features.activetimer.ActiveTimerVM.Event
import jez.stretchping.features.activetimer.ActiveTimerVM.State

internal object EventToCommand : (State.Active, Event) -> Command? {
    override fun invoke(state: State.Active, event: Event): Command? {
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
                    activeSegment != null -> resume(activeSegment)
                    else -> start(state)
                }
            is Event.OnSectionCompleted -> if (state.isAtEnd()) {
                Command.ResetToStart
            } else {
                start(state)
            }
            is Event.Reset -> Command.ResetToStart
            is Event.SetStretchDuration,
            is Event.SetBreakDuration,
            is Event.SetRepCount,
            is Event.UpdateTheme -> null
        }
    }

    private fun start(state: State.Active): Command {
        var isNewRep = false
        val currentSegments =
            state.activeState.queuedSegments.takeIf { it.isNotEmpty() }
                ?: state.createSegments().also {
                    isNewRep = true
                }
        val nextSegment = currentSegments.first()
        val queuedSegments = currentSegments.drop(1)
        return Command.StartSegment(
            System.currentTimeMillis(),
            nextSegment,
            queuedSegments,
            isNewRep,
        )
    }


    private fun resume(activeSegment: ActiveState.ActiveSegment): Command? =
        if (activeSegment.pausedAtFraction == null) {
            null
        } else {
            Command.ResumeSegment(
                startMillis = System.currentTimeMillis(),
                startFraction = activeSegment.pausedAtFraction,
                pausedSegment = activeSegment,
            )
        }

    private fun State.Active.isAtEnd(): Boolean =
        repCount > 0 && activeState.repeatsCompleted == repCount - 1 && activeState.queuedSegments.isEmpty()

    private fun State.Active.createSegments(): List<ActiveState.SegmentSpec> =
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
