package jez.stretchping.features.activetimer

import jez.stretchping.features.activetimer.ActiveTimerVM.Command
import jez.stretchping.features.activetimer.ActiveTimerVM.Event
import jez.stretchping.features.activetimer.ActiveTimerVM.State

internal object EventToCommand : (State, Event) -> Command? {
    override fun invoke(state: State, event: Event): Command? =
        when (event) {
            is Event.Pause ->
                if (state.activeSegment == null) {
                    null
                } else {
                    Command.PauseSegment(
                        pauseMillis = System.currentTimeMillis(),
                        runningSegment = state.activeSegment,
                    )
                }
            is Event.Start ->
                if (state.activeSegment != null) {
                    resume(state.activeSegment)
                } else {
                    start(state.queuedSegments)
                }
            is Event.OnSectionCompleted -> start(state.queuedSegments)
            is Event.Reset -> Command.ResetToStart
        }

    private fun start(queuedSegments: List<State.SegmentSpec>): Command? =
        if (queuedSegments.isEmpty()) {
            null
        } else {
            Command.StartSegment(System.currentTimeMillis(), queuedSegments.first())
        }


    private fun resume(activeSegment: State.ActiveSegment): Command? =
        if (activeSegment.pausedAtFraction == null) {
            null
        } else {
            Command.ResumeSegment(
                startMillis = System.currentTimeMillis(),
                startFraction = activeSegment.pausedAtFraction,
                remainingDurationMillis = activeSegment.endAtTime - (activeSegment.pausedAtTime
                    ?: activeSegment.startedAtTime),
                pausedSegment = activeSegment,
            )
        }
}
