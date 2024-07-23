package jez.stretchping.features.activetimer.view

data class ActiveTimerViewState(
    val activeTimer: ActiveTimerState?,
    val segmentDescription: SegmentDescription?,
    val repCompleteHeading: String?,
)

data class SegmentDescription(
    val name: String,
    val mode: ActiveTimerState.Mode,
    val duration: Duration,
    val position: String,
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
        Announce, Stretch, Transition,
    }
}
