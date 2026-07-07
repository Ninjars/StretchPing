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
    private val timeProvider: TimeProvider,
) {
    private val jobs: MutableList<Job> = mutableListOf()

    // Monotonic deadline of the segment currently scheduled. A segment that
    // chains straight off the previous one (i.e. StartSegment/RepeatExercise
    // fired by OnSegmentCompleted) anchors to this exact instant instead of
    // `now()`, so per-boundary wake-up latency can't compound across a long
    // sequence. Reset (null) whenever the chain breaks — pause, resume, stop,
    // back — or when a fresh start begins far from the last deadline.
    private var lastDeadlineMillis: Long? = null

    suspend fun planFutureActions(
        coroutineScope: CoroutineScope,
        eventsConfiguration: EventsConfiguration,
        executedCommand: Command,
        eventConsumer: Consumer<ActiveTimerVM.Event>,
    ) {
        soundManager.playSilence()
        when (executedCommand) {
            is Command.GoBack -> {
                lastDeadlineMillis = null
                clearAllJobs()
            }

            is Command.SequenceCompleted -> {
                lastDeadlineMillis = null
                clearAllJobs()
            }

            is Command.PauseSegment -> {
                lastDeadlineMillis = null
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

    private fun scheduleNextSegmentEvents(
        coroutineScope: CoroutineScope,
        eventsConfiguration: EventsConfiguration,
        durationMillis: Long,
        segmentSpec: SegmentSpec,
        eventConsumer: Consumer<ActiveTimerVM.Event>,
    ) {
        clearAllJobs()

        // Anchor everything to a single absolute deadline. Firing times are
        // computed as absolute instants (see [delayUntil]) rather than chained
        // relative delays, so a late wake-up on one ping never shifts the
        // segment boundary or the pings around it.
        val now = timeProvider.nowMillis()
        // When this segment chains straight off the previous boundary, anchor
        // its start to that boundary's deadline so drift doesn't accumulate
        // across the whole sequence. `now` is used for a fresh/resumed start.
        val previousDeadline = lastDeadlineMillis
        val startMillis =
            if (previousDeadline != null && now - previousDeadline in 0..CHAIN_TOLERANCE_MILLIS) {
                previousDeadline
            } else {
                now
            }
        val deadlineMillis = startMillis + durationMillis
        lastDeadlineMillis = deadlineMillis

        jobs.add(
            coroutineScope.launch {
                if (segmentSpec is SegmentSpec.Announcement) {
                    segmentSpec.name?.let {
                        ttsManager.announce(it)
                    }
                }
                delayUntil(deadlineMillis)

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

                eventConsumer.accept(ActiveTimerVM.Event.OnSegmentCompleted)
            },
        )
        enqueueCountdownPings(
            coroutineScope = coroutineScope,
            deadlineMillis = deadlineMillis,
            durationMillis = durationMillis,
            pingCount = when (segmentSpec) {
                is SegmentSpec.Stretch -> eventsConfiguration.activePings

                is SegmentSpec.Transition,
                is SegmentSpec.Announcement,
                -> eventsConfiguration.transitionPings
            },
            pingIntervalMillis = 1000L,
        )
    }

    private fun enqueueCountdownPings(
        coroutineScope: CoroutineScope,
        deadlineMillis: Long,
        durationMillis: Long,
        pingCount: Int,
        pingIntervalMillis: Long,
    ) {
        (1 until (min(pingCount, (durationMillis / pingIntervalMillis).toInt()))).forEach {
            val fireAtMillis = deadlineMillis - it * pingIntervalMillis
            jobs.add(
                coroutineScope.launch {
                    delayUntil(fireAtMillis)
                    soundManager.playSound(GameSoundEffect.CountdownBeep)
                },
            )
        }
    }

    /**
     * Suspends until the monotonic clock reaches [targetMillis]. Recomputes the
     * remaining time after each wake so an over- or under-shoot (e.g. the CPU
     * being throttled in doze) is corrected against the absolute target instead
     * of accumulating into later events.
     */
    private suspend fun delayUntil(targetMillis: Long) {
        while (true) {
            val remaining = targetMillis - timeProvider.nowMillis()
            if (remaining <= 0) return
            delay(remaining.milliseconds)
        }
    }

    fun dispose() {
        lastDeadlineMillis = null
        jobs.forEach { it.cancel() }
    }

    private companion object {
        // A chained segment starts within roughly a wake-up-latency window of the
        // previous deadline; beyond this we treat the start as fresh and anchor to now.
        const val CHAIN_TOLERANCE_MILLIS = 750L
    }
}
