package jez.stretchping.features.activetimer.view

import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.ActiveState.ActiveSegment
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.ActiveState.SegmentSpec
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.State

internal object ActiveTimerStateToViewState :
        (State) -> ActiveTimerViewState {
    override fun invoke(
        state: State,
    ): ActiveTimerViewState =
        ActiveTimerViewState(
            activeTimer = state.activeState.activeSegment?.toState(),
            segmentDescription = state.toSegmentDescription(),
        )

    private fun ActiveSegment.toState(): ActiveTimerState =
        ActiveTimerState(
            startAtFraction = startedAtFraction,
            endTimeMillis = endAtTime,
            pausedAtFraction = pausedAtFraction,
            mode = mode.map()
        )

    private fun SegmentSpec.Mode.map() =
        when (this) {
            SegmentSpec.Mode.Stretch -> ActiveTimerState.Mode.Stretch
            SegmentSpec.Mode.Transition -> ActiveTimerState.Mode.Transition
        }

    private fun State.toSegmentDescription(): SegmentDescription {
        val segmentLength =
            activeState.activeSegment?.spec?.durationSeconds
                ?: activeSegmentLength
        val segmentMode =
            activeState.activeSegment?.mode?.map() ?: ActiveTimerState.Mode.Stretch

        return SegmentDescription(
            mode = segmentMode,
            duration = segmentLength.toDuration(),
            repsRemaining = if (repCount < 1) "∞" else (repCount - activeState.repeatsCompleted).toRepCountString()
        )
    }

    private fun Int.toRepCountString() = if (this < 0) "∞" else this.toString()

    private fun Int.toDuration(): Duration {
        val value = Integer.max(this, 0)
        val minutes = value / 60
        val seconds = value % 60
        return Duration(minutes, seconds)
    }
}
