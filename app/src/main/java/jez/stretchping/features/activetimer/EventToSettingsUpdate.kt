package jez.stretchping.features.activetimer

import jez.stretchping.features.activetimer.ActiveTimerVM.Event
import jez.stretchping.features.activetimer.ActiveTimerVM.SettingsCommand
import jez.stretchping.persistence.ThemeMode
import java.lang.Integer.max

internal object EventToSettingsUpdate : (Event) -> SettingsCommand? {
    override fun invoke(event: Event): SettingsCommand? =
        when (event) {
            is Event.UpdateTheme -> SettingsCommand.SetThemeMode(ThemeMode.displayValues[event.themeModeIndex])
            is Event.SetRepCount -> event.count.toFlooredInt()?.let {
                SettingsCommand.SetRepCount(max(-1, it))
            }
            is Event.SetStretchDuration -> event.duration.toFlooredInt()?.let {
                SettingsCommand.SetActivityDuration(max(0, it))
            }
            is Event.SetBreakDuration -> event.duration.toFlooredInt()?.let {
                SettingsCommand.SetTransitionDuration(max(0, it))
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
