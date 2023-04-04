package jez.stretchping.features.activetimer

import androidx.core.util.Consumer
import jez.stretchping.audio.GameSoundEffect
import jez.stretchping.audio.SoundManager
import jez.stretchping.features.activetimer.ActiveTimerVM.State.SegmentSpec.Mode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Integer.min
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class EventScheduler @Inject constructor(
    private val soundManager: SoundManager,
) {
    private val jobs: MutableList<Job> = mutableListOf()

    suspend fun planFutureActions(
        coroutineScope: CoroutineScope,
        executedCommand: ActiveTimerVM.Command,
        eventConsumer: Consumer<ActiveTimerVM.Event>,
    ) {
        when (executedCommand) {
            is ActiveTimerVM.Command.EnqueueSegments -> Unit
            is ActiveTimerVM.Command.PauseSegment,
            is ActiveTimerVM.Command.ResetToStart -> {
                clearAllJobs()
                soundManager.playSound(GameSoundEffect.Stop)
            }
            is ActiveTimerVM.Command.ResumeSegment ->
                scheduleNextSegmentEvents(
                    coroutineScope,
                    (executedCommand.remainingDurationMillis / 1000).toInt(),
                    executedCommand.pausedSegment.mode,
                    eventConsumer,
                )
            is ActiveTimerVM.Command.StartSegment ->
                scheduleNextSegmentEvents(
                    coroutineScope,
                    executedCommand.segmentSpec.durationSeconds,
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
        seconds: Int,
        segmentMode: Mode,
        eventConsumer: Consumer<ActiveTimerVM.Event>,
    ) {
        jobs.add(
            coroutineScope.launch {
                delay(seconds.seconds)
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
            durationSeconds = seconds,
            pingCount = 5,
            pingInterval = when (segmentMode) {
                Mode.Stretch -> 2
                Mode.Transition -> 1
            },
        )
    }

    private suspend fun enqueueCountdownPings(
        coroutineScope: CoroutineScope,
        durationSeconds: Int,
        pingCount: Int,
        pingInterval: Int,
    ) {
        (1..(min(pingCount, durationSeconds / pingInterval))).forEach {
            jobs.add(
                coroutineScope.launch {
                    delay((durationSeconds - it * pingInterval).seconds)
                    soundManager.playSound(GameSoundEffect.CountdownBeep)
                }
            )
        }
    }

    fun dispose() {
        soundManager.dispose()
    }
}
