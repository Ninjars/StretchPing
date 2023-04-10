package jez.stretchping.features.activetimer

import jez.stretchping.features.activetimer.ActiveTimerVM.Event
import jez.stretchping.features.activetimer.ActiveTimerVM.SettingsCommand
import jez.stretchping.persistence.ThemeMode

internal object EventToSettingsUpdate : (Event) -> SettingsCommand? {
    override fun invoke(event: Event): SettingsCommand? =
        when (event) {
            is Event.UpdateTheme -> SettingsCommand.SetThemeMode(ThemeMode.displayValues[event.themeModeIndex])
            is Event.UpdateRepCount -> event.count.toFlooredInt()?.let {
                SettingsCommand.SetRepCount(it)
            }
            is Event.UpdateStretchDuration -> event.duration.toFlooredInt()?.let {
                SettingsCommand.SetActivityDuration(it)
            }
            is Event.UpdateBreakDuration -> event.duration.toFlooredInt()?.let {
                SettingsCommand.SetTransitionDuration(it)
            }
            is Event.OnSectionCompleted,
            is Event.Pause,
            is Event.Reset,
            is Event.Start -> null
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
