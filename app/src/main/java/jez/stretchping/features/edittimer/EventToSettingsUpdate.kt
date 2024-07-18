package jez.stretchping.features.edittimer

import jez.stretchping.features.edittimer.EditTimerVM.SettingsCommand
import jez.stretchping.persistence.ThemeMode

internal object EventToSettingsUpdate : (EditTimerEvent) -> SettingsCommand? {
    override fun invoke(event: EditTimerEvent): SettingsCommand? =
        when (event) {
            is EditTimerEvent.Start -> null
            is EditTimerEvent.UpdateTheme -> SettingsCommand.SetThemeMode(ThemeMode.displayValues[event.themeModeIndex])
            is EditTimerEvent.UpdateRepCount -> event.count.toFlooredInt()?.let {
                SettingsCommand.SetRepCount(it)
            }

            is EditTimerEvent.UpdateActiveDuration -> event.duration.toFlooredInt()?.let {
                SettingsCommand.SetActivityDuration(it)
            }

            is EditTimerEvent.UpdateTransitionDuration -> event.duration.toFlooredInt()?.let {
                SettingsCommand.SetTransitionDuration(it)
            }

            is EditTimerEvent.AutoPause -> SettingsCommand.SetAutoPause(event.enabled)
            is EditTimerEvent.UpdateActivePings -> SettingsCommand.SetActivePings(event.count)
            is EditTimerEvent.UpdateTransitionPings -> SettingsCommand.SetTransitionPings(event.count)
        }

    private fun String.toFlooredInt(): Int? =
        if (this.isEmpty()) {
            Int.MIN_VALUE
        } else {
            try {
                this.toFloat().toInt()
            } catch (e: NumberFormatException) {
                null
            }
        }
}
