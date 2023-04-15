package jez.stretchping.features.activetimer

import androidx.core.util.Consumer
import jez.stretchping.audio.GameSoundEffect
import jez.stretchping.audio.SoundManager
import jez.stretchping.features.activetimer.ActiveTimerVM.ActiveState.SegmentSpec.Mode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Integer.min
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class EventsConfiguration(
    val activePings: Int,
    val transitionPings: Int,
)

class EventScheduler @Inject constructor(
    private val soundManager: SoundManager,
) {
    private val jobs: MutableList<Job> = mutableListOf()

    suspend fun planFutureActions(
        coroutineScope: CoroutineScope,
        eventsConfiguration: EventsConfiguration,
        executedCommand: ActiveTimerVM.Command,
        eventConsumer: Consumer<ActiveTimerVM.Event>,
    ) {
        when (executedCommand) {
            is ActiveTimerVM.Command.PauseSegment,
            is ActiveTimerVM.Command.ResetToStart -> {
                clearAllJobs()
                soundManager.playSound(GameSoundEffect.Stop)
            }
            is ActiveTimerVM.Command.ResumeSegment ->
                scheduleNextSegmentEvents(
                    coroutineScope,
                    eventsConfiguration,
                    executedCommand.pausedSegment.remainingDurationMillis,
                    executedCommand.pausedSegment.mode,
                    eventConsumer,
                )
            is ActiveTimerVM.Command.StartSegment ->
                scheduleNextSegmentEvents(
                    coroutineScope,
                    eventsConfiguration,
                    executedCommand.segmentSpec.durationSeconds * 1000L,
                    executedCommand.segmentSpec.mode,
                    eventConsumer,
                )
        }
    }

    private fun clearAllJobs() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    private suspend fun scheduleNextSegmentEvents(
        coroutineScope: CoroutineScope,
        eventsConfiguration: EventsConfiguration,
        durationMillis: Long,
        segmentMode: Mode,
        eventConsumer: Consumer<ActiveTimerVM.Event>,
    ) {
        jobs.add(
            coroutineScope.launch {
                delay(durationMillis.milliseconds)
                eventConsumer.accept(ActiveTimerVM.Event.OnSectionCompleted)
                soundManager.playSound(
                    when (segmentMode) {
                        Mode.Stretch -> GameSoundEffect.ActiveSection
                        Mode.Transition -> GameSoundEffect.TransitionSection
                    }
                )
            }
        )
        enqueueCountdownPings(
            coroutineScope = coroutineScope,
            durationMillis = durationMillis,
            pingCount = when (segmentMode) {
                Mode.Stretch -> eventsConfiguration.activePings
                Mode.Transition -> eventsConfiguration.transitionPings
            },
            pingIntervalMillis = when (segmentMode) {
                Mode.Stretch -> 1
                Mode.Transition -> 1
            } * 1000L,
        )
    }

    private suspend fun enqueueCountdownPings(
        coroutineScope: CoroutineScope,
        durationMillis: Long,
        pingCount: Int,
        pingIntervalMillis: Long,
    ) {
        (1 until (min(pingCount, (durationMillis / pingIntervalMillis).toInt()))).forEach {
            jobs.add(
                coroutineScope.launch {
                    delay((durationMillis - it * pingIntervalMillis).milliseconds)
                    soundManager.playSound(GameSoundEffect.CountdownBeep)
                }
            )
        }
    }

    fun dispose() {
        soundManager.dispose()
    }
}
