package jez.stretchping.features.activetimer.logic

import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.ActiveState
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.Command

internal object ActiveTimerStateUpdater : (ActiveState, Command?) -> ActiveState {

    override fun invoke(state: ActiveState, command: Command?): ActiveState =
        when (command) {
            null -> state
            is Command.PauseSegment -> pauseActiveSegment(state, command)
            is Command.ResumeSegment -> resumePausedSegment(state, command)
            is Command.StartSegment -> startNextSegment(state, command)
            is Command.SequenceCompleted,
            is Command.GoBack -> state
        }

    private fun startNextSegment(state: ActiveState, command: Command.StartSegment): ActiveState =
        state.copy(
            activeSegment = command.toActiveSegment(),
            queuedSegments = command.queuedSegments,
            repeatsCompleted = if (command.isNewRep) {
                state.repeatsCompleted + 1
            } else {
                state.repeatsCompleted
            },
        )

    private fun Command.StartSegment.toActiveSegment(): ActiveState.ActiveSegment =
        ActiveState.ActiveSegment(
            startedAtTime = startMillis,
            startedAtFraction = 0f,
            endAtTime = startMillis + segmentSpec.durationSeconds.toMillis(),
            pausedAtFraction = null,
            pausedAtTime = null,
            spec = segmentSpec,
        )

    private fun Int.toMillis() = this * 1000

    private fun resumePausedSegment(
        state: ActiveState,
        command: Command.ResumeSegment
    ): ActiveState =
        state.copy(
            activeSegment = command.pausedSegment.toResumed(
                command.startMillis,
                command.startFraction,
            )
        )

    private fun ActiveState.ActiveSegment.toResumed(
        currentTimeMillis: Long,
        currentFraction: Float,
    ): ActiveState.ActiveSegment {
        return copy(
            endAtTime = currentTimeMillis + this.remainingDurationMillis,
            startedAtTime = currentTimeMillis,
            startedAtFraction = currentFraction,
            pausedAtFraction = null,
            pausedAtTime = null,
        )
    }

    private fun pauseActiveSegment(state: ActiveState, command: Command.PauseSegment): ActiveState =
        state.copy(activeSegment = command.runningSegment.toPaused(command.pauseMillis))

    private fun ActiveState.ActiveSegment.toPaused(currentTimeMillis: Long): ActiveState.ActiveSegment {
        val scalingFactor = 1f - startedAtFraction
        val currentFraction =
            (currentTimeMillis - startedAtTime).toDouble() / (endAtTime - startedAtTime).toDouble()
        return copy(
            pausedAtTime = currentTimeMillis,
            pausedAtFraction = (startedAtFraction + scalingFactor * currentFraction).toFloat(),
        )
    }
}
