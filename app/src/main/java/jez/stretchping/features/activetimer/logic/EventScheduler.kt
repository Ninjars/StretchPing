package jez.stretchping.features.activetimer.logic

import androidx.core.util.Consumer
import jez.stretchping.audio.GameSoundEffect
import jez.stretchping.audio.SoundManager
import jez.stretchping.audio.TTSManager
import jez.stretchping.features.activetimer.ActiveTimerVM
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.Command
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.State.SegmentSpec
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
    private val ttsManager: TTSManager,
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
                    executedCommand.pausedSegment.spec,
                    eventConsumer,
                )

            is Command.StartSegment ->
                scheduleNextSegmentEvents(
                    coroutineScope,
                    eventsConfiguration,
                    executedCommand.segmentSpec.durationSeconds * 1000L,
                    executedCommand.segmentSpec,
                    eventConsumer,
                )

            is Command.RepeatExercise ->
                scheduleNextSegmentEvents(
                    coroutineScope,
                    eventsConfiguration,
                    executedCommand.segmentSpec.durationSeconds * 1000L,
                    executedCommand.segmentSpec,
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
        segmentSpec: SegmentSpec,
        eventConsumer: Consumer<ActiveTimerVM.Event>,
    ) {
        clearAllJobs()
        jobs.add(
            coroutineScope.launch {
                if (segmentSpec is SegmentSpec.Announcement) {
                    segmentSpec.name?.let {
                        ttsManager.announce(it)
                    }
                }
                delay(durationMillis.milliseconds)
                eventConsumer.accept(ActiveTimerVM.Event.OnSegmentCompleted)

                if (segmentSpec.isLast) {
                    soundManager.playSound(GameSoundEffect.Completed)
                } else {
                    when (segmentSpec) {
                        is SegmentSpec.Stretch -> soundManager.playSound(GameSoundEffect.ActiveSection)
                        is SegmentSpec.Transition -> soundManager.playSound(GameSoundEffect.TransitionSection)
                        is SegmentSpec.Announcement ->
                            if (segmentSpec.durationSeconds > 0) {
                                soundManager.playSound(GameSoundEffect.TransitionSection)
                            }
                    }
                }
            }
        )
        enqueueCountdownPings(
            coroutineScope = coroutineScope,
            durationMillis = durationMillis,
            pingCount = when (segmentSpec) {
                is SegmentSpec.Stretch -> eventsConfiguration.activePings
                is SegmentSpec.Transition,
                is SegmentSpec.Announcement -> eventsConfiguration.transitionPings
            },
            pingIntervalMillis = 1000L,
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
        jobs.forEach { it.cancel() }
    }
}
