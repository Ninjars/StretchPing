package jez.stretchping.features.activetimer

import jez.stretchping.features.activetimer.ActiveTimerVM.State.SegmentSpec

data class ActiveTimerViewState(
    val activeTimer: ActiveTimerState?,
    val segmentDescription: SegmentDescription?,
)

data class SegmentDescription(
    val mode: ActiveTimerState.Mode,
    val duration: String,
    val repsRemaining: String,
)

data class ActiveTimerState(
    val start: Long,
    val startAtFraction: Float,
    val end: Long,
    val pausedAtFraction: Float?,
    val mode: Mode,
) {
    val isPaused = pausedAtFraction != null

    enum class Mode {
        Stretch,
        Transition,
    }
}

internal object ActiveTimerStateToViewState : (ActiveTimerVM.State) -> ActiveTimerViewState {
    override fun invoke(state: ActiveTimerVM.State): ActiveTimerViewState =
        ActiveTimerViewState(
            activeTimer = state.activeSegment?.toState(),
            segmentDescription = state.toSegmentDescription(),
        )

    private fun ActiveTimerVM.State.ActiveSegment.toState(): ActiveTimerState =
        ActiveTimerState(
            start = startedAtTime,
            startAtFraction = startedAtFraction,
            end = endAtTime,
            pausedAtFraction = pausedAtFraction,
            mode = mode.map()
        )

    private fun SegmentSpec.Mode.map() =
        when (this) {
            SegmentSpec.Mode.Stretch -> ActiveTimerState.Mode.Stretch
            SegmentSpec.Mode.Transition -> ActiveTimerState.Mode.Transition
        }

    private fun ActiveTimerVM.State.toSegmentDescription(): SegmentDescription? {
        val segment =
            activeSegment?.spec
                ?: fullSequence.firstOrNull { it.mode != SegmentSpec.Mode.Transition }

        return segment?.let {
            SegmentDescription(
                mode = it.mode.map(),
                duration = it.durationSeconds.toString(),
                repsRemaining = if (repeatsRemaining < 0) "∞" else repeatsRemaining.toString(),
            )
        }
    }
}
