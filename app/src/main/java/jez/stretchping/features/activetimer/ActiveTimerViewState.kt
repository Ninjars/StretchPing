package jez.stretchping.features.activetimer

import jez.stretchping.features.activetimer.ActiveTimerVM.ActiveState.SegmentSpec
import jez.stretchping.features.activetimer.ActiveTimerVM.State
import jez.stretchping.persistence.ThemeMode

data class ActiveTimerViewState(
    val editTimerState: EditTimerState?,
    val activeTimer: ActiveTimerState?,
    val segmentDescription: SegmentDescription?,
) {
    val isLoading = editTimerState == null && activeTimer == null && segmentDescription == null
}

data class SegmentDescription(
    val mode: ActiveTimerState.Mode,
    val duration: Duration,
    val repsRemaining: String,
)

data class EditTimerState(
    val activeDuration: String,
    val breakDuration: String,
    val repCount: String,
    val themeState: ThemeState,
) {
    data class ThemeState(
        val options: List<String>,
        val selectedIndex: Int,
    )
}

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

internal object StateToViewState :
        (State) -> ActiveTimerViewState {
    override fun invoke(
        state: State,
    ): ActiveTimerViewState =
        when (state) {
            is State.Loading ->
                ActiveTimerViewState(null, null, null)
            is State.Active ->
                ActiveTimerViewState(
                    activeTimer = state.activeState.activeSegment?.toState(),
                    segmentDescription = state.toSegmentDescription(),
                    editTimerState = state.toEditTimerState(state.themeMode),
                )
        }

    private fun State.Active.toEditTimerState(themeMode: ThemeMode): EditTimerState? =
        if (activeState.activeSegment == null) {
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
                    repCount == Int.MIN_VALUE -> ""
                    repCount < 1 -> "∞"
                    else -> repCount.toString()
                },
                themeState = createThemeState(themeMode),
            )
        } else {
            null
        }

    private fun createThemeState(themeMode: ThemeMode): EditTimerState.ThemeState =
        EditTimerState.ThemeState(
            options = ThemeMode.values().map { it.toString() },
            selectedIndex = themeMode.ordinal,
        )

    private fun ActiveTimerVM.ActiveState.ActiveSegment.toState(): ActiveTimerState =
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

    private fun State.Active.toSegmentDescription(): SegmentDescription {
        val segmentLength =
            activeState.activeSegment?.spec?.durationSeconds
                ?: activeSegmentLength
        val segmentMode =
            activeState.activeSegment?.mode?.map() ?: ActiveTimerState.Mode.Stretch

        return SegmentDescription(
            mode = segmentMode,
            duration = segmentLength.toDuration(),
            repsRemaining = (repCount - activeState.repeatsCompleted).toRepCountString()
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
