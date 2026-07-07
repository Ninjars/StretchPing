package jez.stretchping.features.activetimer.logic

/**
 * Lets the engine tell its host (the foreground service) whether the timer is
 * actively counting down, so the host can hold a partial wake lock only while
 * running and release it on pause/completion. Kept as an interface so the pure
 * scheduling logic doesn't depend on Android's PowerManager.
 */
interface RunningStateController {
    fun onRunningStateChanged(isRunning: Boolean)
}
