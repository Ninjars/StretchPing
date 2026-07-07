package jez.stretchping.features.activetimer.logic

import androidx.core.util.Consumer
import jez.stretchping.audio.GameSoundEffect
import jez.stretchping.audio.SoundManager
import jez.stretchping.audio.TTSManager
import jez.stretchping.features.activetimer.ActiveTimerVM
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.Command
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.State.SegmentSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class EventSchedulerTest {

    /**
     * Every scheduled sound, tagged with the virtual-clock instant it fired at.
     * Populated by the mocked [SoundManager] so tests can assert absolute timing.
     */
    private data class SoundEvent(val effect: GameSoundEffect, val atMillis: Long)

    private val fired = mutableListOf<SoundEvent>()

    private fun stretch(durationSeconds: Int, isLast: Boolean = false) = SegmentSpec.Stretch(
        name = null,
        durationSeconds = durationSeconds,
        index = 0,
        isLast = isLast,
        repCount = 1,
        isStartOfSegment = true,
    )

    /** Builds a scheduler whose clock is the [TestScope] virtual clock. */
    private fun TestScope.newScheduler(): EventScheduler {
        val soundManager = mock<SoundManager> {
            on { playSilence() } doAnswer {}
        }
        // Record playSound(effect) invocations with the current virtual time.
        whenever(soundManager.playSound(org.mockito.kotlin.any())) doAnswer { invocation ->
            fired.add(SoundEvent(invocation.getArgument(0), testScheduler.currentTime))
            Unit
        }
        val ttsManager = mock<TTSManager>()
        return EventScheduler(
            soundManager = soundManager,
            ttsManager = ttsManager,
            timeProvider = { testScheduler.currentTime },
        )
    }

    private fun noopConsumer() = Consumer<ActiveTimerVM.Event> { }

    @Test
    fun `boundary and countdown pings fire at absolute offsets from start`() = runTest {
        val scheduler = newScheduler()
        val config = EventsConfiguration(activePings = 3, transitionPings = 0)

        scheduler.planFutureActions(
            coroutineScope = this,
            eventsConfiguration = config,
            executedCommand = Command.StartSegment(0L, 0, stretch(10)),
            eventConsumer = noopConsumer(),
        )
        advanceUntilIdle()

        // 2 countdown beeps (pingCount=3 -> indices 1..2) at 8s and 9s, then the
        // ActiveSection boundary at exactly 10s.
        assertEquals(
            listOf(
                SoundEvent(GameSoundEffect.CountdownBeep, 9_000L),
                SoundEvent(GameSoundEffect.CountdownBeep, 8_000L),
                SoundEvent(GameSoundEffect.ActiveSection, 10_000L),
            ).sortedBy { it.atMillis },
            fired.sortedBy { it.atMillis },
        )
    }

    @Test
    fun `boundary fires at absolute deadline even when a wake-up is late`() = runTest {
        val scheduler = newScheduler()

        scheduler.planFutureActions(
            coroutineScope = this,
            eventsConfiguration = EventsConfiguration(activePings = 2, transitionPings = 0),
            executedCommand = Command.StartSegment(0L, 0, stretch(5)),
            eventConsumer = noopConsumer(),
        )

        // Simulate a delayed wake-up: jump the clock well past the 4s ping deadline
        // in one step. delayUntil() must still fire it once and the 5s boundary
        // must land at 5s, not be pushed out by the overshoot.
        advanceTimeBy(4_500)
        runCurrent()
        advanceUntilIdle()

        val boundary = fired.single { it.effect == GameSoundEffect.ActiveSection }
        assertEquals(5_000L, boundary.atMillis)
    }

    @Test
    fun `drift does not accumulate across chained segments`() = runTest {
        // Re-schedule 5 back-to-back 3s segments the way the engine does on each
        // OnSegmentCompleted. Because every segment anchors to
        // timeProvider.now() + duration, the Nth boundary must land at exactly
        // N*3s with zero accumulated drift.
        val scheduler = newScheduler()
        val boundaries = mutableListOf<Long>()
        var segmentEnded: Boolean
        val consumer = Consumer<ActiveTimerVM.Event> {
            boundaries.add(testScheduler.currentTime)
            segmentEnded = true
        }

        repeat(5) { i ->
            segmentEnded = false
            scheduler.planFutureActions(
                coroutineScope = this,
                eventsConfiguration = EventsConfiguration(0, 0),
                executedCommand = Command.StartSegment(0L, i, stretch(3)),
                eventConsumer = consumer,
            )
            // Advance until this segment's boundary fires, mimicking the engine
            // re-scheduling the next segment only once the previous one ends.
            while (!segmentEnded) {
                advanceTimeBy(3_000)
                runCurrent()
            }
        }

        assertEquals(listOf(3_000L, 6_000L, 9_000L, 12_000L, 15_000L), boundaries)
    }

    @Test
    fun `pause clears pending jobs so no further sounds fire`() = runTest {
        val scheduler = newScheduler()
        scheduler.planFutureActions(
            coroutineScope = this,
            eventsConfiguration = EventsConfiguration(activePings = 3, transitionPings = 0),
            executedCommand = Command.StartSegment(0L, 0, stretch(10)),
            eventConsumer = noopConsumer(),
        )
        advanceTimeBy(3_000)
        runCurrent()
        fired.clear()

        val active = ActiveTimerEngine.State.ActiveSegment(
            startedAtTime = 0L,
            startedAtFraction = 0f,
            endAtTime = 10_000L,
            pausedAtFraction = 0.3f,
            pausedAtTime = 3_000L,
            spec = stretch(10),
        )
        scheduler.planFutureActions(
            coroutineScope = this,
            eventsConfiguration = EventsConfiguration(activePings = 3, transitionPings = 0),
            executedCommand = Command.PauseSegment(3_000L, active),
            eventConsumer = noopConsumer(),
        )
        // The Stop sound fires on pause; drop it so we only check for leaks.
        fired.removeAll { it.effect == GameSoundEffect.Stop }
        advanceUntilIdle()

        assertEquals(emptyList<SoundEvent>(), fired)
    }
}
