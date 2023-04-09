package jez.stretchping.features.activetimer

import jez.stretchping.features.activetimer.ActiveTimerVM.State.SegmentSpec

data class ActiveTimerViewState(
    val editTimerState: EditTimerState?,
    val activeTimer: ActiveTimerState?,
    val segmentDescription: SegmentDescription?,
)

data class SegmentDescription(
    val mode: ActiveTimerState.Mode,
    val duration: Duration,
    val repsRemaining: String,
)

data class EditTimerState(
    val activeDuration: String,
    val breakDuration: String,
    val repCount: String,
)

data class Duration(
    val minutes: Int,
    val seconds: Int,
) {
    fun asSeconds() = minutes * 60 + seconds
}

data class ActiveTimerState(
    val startAtFraction: Float,
    val durationMillis: Long,
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
            editTimerState = state.toEditTimerState(),
        )

    private fun ActiveTimerVM.State.toEditTimerState(): EditTimerState? =
        if (activeSegment == null) {
            EditTimerState(
                activeDuration = when (activeSegmentLength) {
                    Int.MIN_VALUE -> ""
                    else -> activeSegmentLength.toString()
                },
                breakDuration = when (transitionLength) {
                    Int.MIN_VALUE -> ""
                    else -> transitionLength.toString()
                },
                repCount = when {
                    targetRepeatCount == Int.MIN_VALUE -> ""
                    targetRepeatCount < 1 -> "∞"
                    else -> targetRepeatCount.toString()
                }
            )
        } else {
            null
        }

    private fun ActiveTimerVM.State.ActiveSegment.toState(): ActiveTimerState =
        ActiveTimerState(
            startAtFraction = startedAtFraction,
            durationMillis = remainingDurationMillis,
            pausedAtFraction = pausedAtFraction,
            mode = mode.map()
        )

    private fun SegmentSpec.Mode.map() =
        when (this) {
            SegmentSpec.Mode.Stretch -> ActiveTimerState.Mode.Stretch
            SegmentSpec.Mode.Transition -> ActiveTimerState.Mode.Transition
        }

    private fun ActiveTimerVM.State.toSegmentDescription(): SegmentDescription {
        val segmentLength =
            activeSegment?.spec?.durationSeconds
                ?: activeSegmentLength
        val segmentMode =
            activeSegment?.mode?.map() ?: ActiveTimerState.Mode.Stretch

        return SegmentDescription(
            mode = segmentMode,
            duration = segmentLength.toDuration(),
            repsRemaining = (targetRepeatCount - repeatsCompleted).toRepCountString()
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
