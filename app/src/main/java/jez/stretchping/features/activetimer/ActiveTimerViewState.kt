package jez.stretchping.features.activetimer

import jez.stretchping.features.activetimer.ActiveTimerVM.State.SegmentSpec

data class ActiveTimerViewState(
    val activeTimer: ActiveTimerState?,
)

data class ActiveTimerState(
    val start: Long,
    val startAtFraction: Float,
    val end: Long,
    val pausedAtFraction: Float?,
    val mode: Mode,
) {
    enum class Mode {
        Stretch,
        Transition,
    }
}

internal object ActiveTimerStateToViewState : (ActiveTimerVM.State) -> ActiveTimerViewState {
    override fun invoke(state: ActiveTimerVM.State): ActiveTimerViewState =
        ActiveTimerViewState(
            activeTimer = state.activeSegment?.toState(),
        )

    private fun ActiveTimerVM.State.ActiveSegment.toState(): ActiveTimerState =
        ActiveTimerState(
            start = startedAtTime,
            startAtFraction = startedAtFraction,
            end = endAtTime,
            pausedAtFraction = pausedAtFraction,
            mode = when (mode) {
                SegmentSpec.Mode.Stretch -> ActiveTimerState.Mode.Stretch
                SegmentSpec.Mode.Transition -> ActiveTimerState.Mode.Transition
            }
        )
}
