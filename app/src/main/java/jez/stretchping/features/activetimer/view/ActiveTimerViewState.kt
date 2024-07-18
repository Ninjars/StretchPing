package jez.stretchping.features.activetimer.view

data class ActiveTimerViewState(
    val activeTimer: ActiveTimerState?,
    val segmentDescription: SegmentDescription?,
)

data class SegmentDescription(
    val mode: ActiveTimerState.Mode,
    val duration: Duration,
    val repsRemaining: String,
)


data class Duration(
    val minutes: Int,
    val seconds: Int,
) {
    fun asSeconds() = minutes * 60 + seconds
}

data class ActiveTimerState(
    val startAtFraction: Float,
    val endTimeMillis: Long,
    val pausedAtFraction: Float?,
    val mode: Mode,
) {
    val isPaused = pausedAtFraction != null

    enum class Mode {
        Stretch, Transition,
    }
}
