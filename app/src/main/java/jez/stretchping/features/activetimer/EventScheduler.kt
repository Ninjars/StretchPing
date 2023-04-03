package jez.stretchping.features.activetimer

import androidx.core.util.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class EventScheduler @Inject constructor() {
    private val jobs: MutableList<Job> = mutableListOf()

    suspend fun planFutureActions(
        coroutineScope: CoroutineScope,
        executedCommand: ActiveTimerVM.Command,
        eventConsumer: Consumer<ActiveTimerVM.Event>,
    ) {
        when (executedCommand) {
            is ActiveTimerVM.Command.EnqueueSegments -> Unit
            is ActiveTimerVM.Command.PauseSegment,
            is ActiveTimerVM.Command.ResetToStart -> clearAllJobs()
            is ActiveTimerVM.Command.ResumeSegment ->
                scheduleNextSegmentEvent(
                    coroutineScope,
                    (executedCommand.remainingDurationMillis / 1000).toInt(),
                    eventConsumer,
                )
            is ActiveTimerVM.Command.StartSegment ->
                scheduleNextSegmentEvent(
                    coroutineScope,
                    executedCommand.segmentSpec.durationSeconds,
                    eventConsumer,
                )
        }
    }

    private fun clearAllJobs() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    private suspend fun scheduleNextSegmentEvent(
        coroutineScope: CoroutineScope,
        seconds: Int,
        eventConsumer: Consumer<ActiveTimerVM.Event>,
    ) {
        jobs.add(
            coroutineScope.launch {
                delay(seconds.seconds)
                eventConsumer.accept(ActiveTimerVM.Event.OnSectionCompleted)
            }
        )
    }
}
