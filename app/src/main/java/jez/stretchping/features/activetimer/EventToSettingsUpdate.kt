package jez.stretchping.features.activetimer

import jez.stretchping.features.activetimer.ActiveTimerVM.Event
import jez.stretchping.features.activetimer.ActiveTimerVM.SettingsCommand
import jez.stretchping.persistence.ThemeMode

internal object EventToSettingsUpdate : (Event) -> SettingsCommand? {
    override fun invoke(event: Event): SettingsCommand? =
        when (event) {
            is Event.UpdateTheme -> SettingsCommand.SetThemeMode(ThemeMode.values()[event.themeModeIndex])
            is Event.OnSectionCompleted,
            is Event.Pause,
            is Event.Reset,
            is Event.SetBreakDuration,
            is Event.SetRepCount,
            is Event.SetStretchDuration,
            is Event.Start -> null
        }
}
