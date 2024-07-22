package jez.stretchping.features.edittimer

import jez.stretchping.R
import jez.stretchping.persistence.ThemeMode

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
                activePings = activePings,
                transitionPings = transitionPings,
                themeState = createThemeState(themeMode),
                canStart = activeSegmentLength > 0 && transitionDuration > 0 && repCount > Int.MIN_VALUE,
                playInBackground = playInBackground,
            )
        }

    private fun createThemeState(themeMode: ThemeMode): EditTimerViewState.ThemeState =
        with(ThemeMode.displayValues) {
            EditTimerViewState.ThemeState(
                optionStringResources = this.map { it.toStringResId() },
                selectedIndex = this.indexOf(themeMode),
            )
        }

    private fun ThemeMode.toStringResId() =
        when (this) {
            ThemeMode.Unset -> -1
            ThemeMode.System -> R.string.theme_system
            ThemeMode.Light -> R.string.theme_light
            ThemeMode.Dark -> R.string.theme_dark
        }
}
