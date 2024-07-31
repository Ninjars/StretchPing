package jez.stretchping.features.edittimer

import jez.stretchping.features.edittimer.EditTimerVM.SettingsCommand
import jez.stretchping.utils.toFlooredInt

internal object EventToSettingsUpdate : (EditTimerEvent) -> SettingsCommand? {
    override fun invoke(event: EditTimerEvent): SettingsCommand? =
        when (event) {
            is EditTimerEvent.Start -> null
            is EditTimerEvent.UpdateRepCount -> event.count.toFlooredInt()?.let {
                SettingsCommand.SetRepCount(it)
            }

            is EditTimerEvent.UpdateActiveDuration -> event.duration.toFlooredInt()?.let {
                SettingsCommand.SetActivityDuration(it)
            }

            is EditTimerEvent.UpdateTransitionDuration -> event.duration.toFlooredInt()?.let {
                SettingsCommand.SetTransitionDuration(it)
            }
        }
}
