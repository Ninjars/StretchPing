package jez.stretchping.features.edittimer

internal object EditTimerStateToViewState :
        (EditTimerVM.State) -> EditTimerViewState {
    override fun invoke(
        state: EditTimerVM.State,
    ): EditTimerViewState =
        with(state) {
            EditTimerViewState(
                activeDuration = when (activeSegmentLength) {
                    Int.MIN_VALUE -> ""
                    else -> activeSegmentLength.toString()
                },
                transitionDuration = when (transitionDuration) {
                    Int.MIN_VALUE -> ""
                    else -> transitionDuration.toString()
                },
                repCount = when {
                    repCount == Int.MIN_VALUE -> ""
                    repCount < 1 -> "∞"
                    else -> repCount.toString()
                },
                canStart = activeSegmentLength > 0 && repCount > Int.MIN_VALUE,
            )
        }
}
