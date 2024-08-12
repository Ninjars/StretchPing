package jez.stretchping.features.activetimer.logic

import jez.stretchping.features.activetimer.ActiveTimerVM.Event
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.Command
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.State

internal class EventToCommand(
    private val canRepeat: Boolean,
) : (State, Event) -> Command? {
    override fun invoke(
        state: State,
        event: Event,
    ): Command? {
        val activeSegment = state.activeSegment
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
                    else ->
                        Command.StartSegment(
                            System.currentTimeMillis(),
                            index = 0,
                            segmentSpec = state.segments[0],
                        )
                }

            is Event.RestartSegmentPressed ->
                findSegmentStartIndex(state).let { index ->
                    Command.StartSegment(
                        System.currentTimeMillis(),
                        index = index,
                        segmentSpec = state.segments[index],
                    )
                }

            is Event.OnSegmentCompleted -> nextSegment(state)

            is Event.BackPressed -> Command.GoBack
        }
    }

    private fun findSegmentStartIndex(state: State): Int {
        for (i in (0 until state.index).reversed()) {
            if (state.segments[i].isStartOfSegment) return i
        }
        return 0
    }

    private fun nextSegment(state: State): Command {
        val nextIndex = state.index + 1
        return if (nextIndex >= state.segments.size) {
            if (canRepeat) {
                Command.RepeatExercise(
                    System.currentTimeMillis(),
                    segmentSpec = state.segments[0],
                )
            } else {
                Command.SequenceCompleted
            }
        } else {
            Command.StartSegment(
                System.currentTimeMillis(),
                index = nextIndex,
                segmentSpec = state.segments[nextIndex],
            )
        }
    }


    private fun resume(
        activeSegment: State.ActiveSegment
    ): Command? =
        if (activeSegment.pausedAtFraction == null) {
            null
        } else {
            Command.ResumeSegment(
                startMillis = System.currentTimeMillis(),
                startFraction = activeSegment.pausedAtFraction,
                pausedSegment = activeSegment,
            )
        }
}
