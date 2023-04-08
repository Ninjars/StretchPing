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
                    start(state)
                }
            is Event.OnSectionCompleted -> if (state.isAtEnd()) {
                Command.ResetToStart(state.createSegments())
            } else {
                start(state)
            }
            is Event.Reset -> Command.ResetToStart(state.createSegments())
        }

    private fun start(state: State): Command {
        val currentSegments =
            state.queuedSegments.takeIf { it.isNotEmpty() } ?: state.createSegments()
        val nextSegment = currentSegments.first()
        val queuedSegments = currentSegments.drop(1)
        return Command.StartSegment(
            System.currentTimeMillis(),
            nextSegment,
            queuedSegments,
        )
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

    private fun State.isAtEnd(): Boolean =
        targetRepeatCount > 0 && repeatsCompleted == targetRepeatCount && queuedSegments.isEmpty()

    private fun State.createSegments(): List<State.SegmentSpec> =
        listOf(
            State.SegmentSpec(
                durationSeconds = transitionLength,
                mode = State.SegmentSpec.Mode.Transition,
            ),
            State.SegmentSpec(
                durationSeconds = activeSegmentLength,
                mode = State.SegmentSpec.Mode.Stretch,
            ),
        )
}
