package jez.stretchping.features.activetimer

import jez.stretchping.features.activetimer.ActiveTimerVM.ActiveState
import jez.stretchping.features.activetimer.ActiveTimerVM.Command
import jez.stretchping.features.activetimer.ActiveTimerVM.Event

internal object EventToCommand : (ActiveState, Event) -> Command? {
    override fun invoke(state: ActiveState, event: Event): Command? =
        when (event) {
            is Event.Pause ->
                if (state.activeSegment == null || state.activeSegment.pausedAtFraction != null) {
                    null
                } else {
                    Command.PauseSegment(
                        pauseMillis = System.currentTimeMillis(),
                        runningSegment = state.activeSegment,
                    )
                }
            is Event.Start ->
                when {
                    state.activeSegmentLength <= 0 -> null
                    state.activeSegment != null -> resume(state.activeSegment)
                    else -> start(state)
                }
            is Event.OnSectionCompleted -> if (state.isAtEnd()) {
                Command.ResetToStart
            } else {
                start(state)
            }
            is Event.Reset -> Command.ResetToStart
            is Event.SetStretchDuration -> event.duration.toFlooredInt()?.let {
                Command.UpdateActiveSegmentLength(it)
            }
            is Event.SetBreakDuration -> event.duration.toFlooredInt()?.let {
                Command.UpdateBreakSegmentLength(it)
            }
            is Event.SetRepCount -> event.count.toFlooredInt()?.let {
                Command.UpdateTargetRepCount(it)
            }
            is Event.UpdateTheme -> null
        }

    private fun String.toFlooredInt(): Int? =
        if (this.isEmpty()) {
            Int.MIN_VALUE
        } else {
            try {
                this.toFloat().toInt()
            } catch (e: NumberFormatException) {
                null
            }
        }

    private fun start(state: ActiveState): Command {
        var isNewRep = false
        val currentSegments =
            state.queuedSegments.takeIf { it.isNotEmpty() }
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

    private fun ActiveState.isAtEnd(): Boolean =
        targetRepeatCount > 0 && repeatsCompleted == targetRepeatCount - 1 && queuedSegments.isEmpty()

    private fun ActiveState.createSegments(): List<ActiveState.SegmentSpec> =
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
