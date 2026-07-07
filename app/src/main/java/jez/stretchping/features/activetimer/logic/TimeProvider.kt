package jez.stretchping.features.activetimer.logic

/**
 * Monotonic time source used to anchor timer scheduling to absolute deadlines.
 *
 * Kept as an interface so [EventScheduler] stays pure-JVM and unit testable: the
 * production implementation reads `SystemClock.elapsedRealtime()` (see
 * [jez.stretchping.di.SchedulingModule]) while tests can inject a fake clock.
 */
fun interface TimeProvider {
    /** Milliseconds from a monotonic clock that keeps counting during deep sleep. */
    fun nowMillis(): Long
}
