package jez.stretchping.features.activetimer.logic

import androidx.core.util.Consumer
import jez.stretchping.audio.GameSoundEffect
import jez.stretchping.audio.SoundManager
import jez.stretchping.features.activetimer.ActiveTimerVM
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.ActiveState.SegmentSpec.Mode
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.Command
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
        executedCommand: Command,
        eventConsumer: Consumer<ActiveTimerVM.Event>,
    ) {
        soundManager.playSilence()
        when (executedCommand) {
            is Command.GoBack ->
                clearAllJobs()

            is Command.SequenceCompleted -> {
                clearAllJobs()
            }

            is Command.PauseSegment -> {
                clearAllJobs()
                soundManager.playSound(GameSoundEffect.Stop)
            }

            is Command.ResumeSegment ->
                scheduleNextSegmentEvents(
                    coroutineScope,
                    eventsConfiguration,
                    executedCommand.pausedSegment.remainingDurationMillis,
                    executedCommand.pausedSegment.mode,
                    executedCommand.isLastSegment,
                    eventConsumer,
                )

            is Command.StartSegment ->
                scheduleNextSegmentEvents(
                    coroutineScope,
                    eventsConfiguration,
                    executedCommand.segmentSpec.durationSeconds * 1000L,
                    executedCommand.segmentSpec.mode,
                    executedCommand.isLastSegment,
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
        isLastSegment: Boolean,
        eventConsumer: Consumer<ActiveTimerVM.Event>,
    ) {
        jobs.add(
            coroutineScope.launch {
                delay(durationMillis.milliseconds)
                eventConsumer.accept(ActiveTimerVM.Event.OnSectionCompleted)

                if (isLastSegment) {
                    soundManager.playSound(GameSoundEffect.Completed)
                } else {
                    soundManager.playSound(
                        when (segmentMode) {
                            Mode.Stretch -> GameSoundEffect.ActiveSection
                            Mode.Transition -> GameSoundEffect.TransitionSection
                        }
                    )
                }
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