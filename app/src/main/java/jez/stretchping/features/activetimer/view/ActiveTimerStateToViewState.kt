package jez.stretchping.features.activetimer.view

import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.State
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.State.ActiveSegment
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.State.SegmentSpec

internal object ActiveTimerStateToViewState : (State) -> ActiveTimerViewState {
    override fun invoke(
        state: State,
    ): ActiveTimerViewState =
        ActiveTimerViewState(
            activeTimer = state.activeSegment?.toState(),
            segmentDescription = state.toSegmentDescription(),
            repCompleteHeading = state.completedReps.toHeadingString(),
        )

    private fun ActiveSegment.toState(): ActiveTimerState =
        ActiveTimerState(
            startAtFraction = startedAtFraction,
            endTimeMillis = endAtTime,
            pausedAtFraction = pausedAtFraction,
            mode = spec.toMode()
        )

    private fun SegmentSpec.toMode() =
        when (this) {
            is SegmentSpec.Announcement -> ActiveTimerState.Mode.Announce
            is SegmentSpec.Stretch -> ActiveTimerState.Mode.Stretch
            is SegmentSpec.Transition -> ActiveTimerState.Mode.Transition
        }

    private fun State.toSegmentDescription(): SegmentDescription {
        val segment = activeSegment
        return if (segment == null) {
            SegmentDescription(
                name = "",
                mode = ActiveTimerState.Mode.Announce,
                duration = 0.toDuration(),
                position = "",
            )
        } else {
            SegmentDescription(
                name = segment.spec.name ?: "",
                mode = segment.spec.toMode(),
                duration = segment.spec.durationSeconds.toDuration(),
                position = segment.spec.position
            )
        }
    }

    private fun Int.toDuration(): Duration {
        val value = Integer.max(this, 0)
        val minutes = value / 60
        val seconds = value % 60
        return Duration(minutes, seconds)
    }

    private fun Int.toHeadingString(): String? =
        if (this > 0) {
            this.toString()
        } else {
            null
        }
}
