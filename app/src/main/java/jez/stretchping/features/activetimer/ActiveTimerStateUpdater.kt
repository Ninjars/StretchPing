package jez.stretchping.features.activetimer

import jez.stretchping.features.activetimer.ActiveTimerVM.ActiveState
import jez.stretchping.features.activetimer.ActiveTimerVM.Command

internal object ActiveTimerStateUpdater : (ActiveState, Command?) -> ActiveState {

    override fun invoke(state: ActiveState, command: Command?): ActiveState =
        when (command) {
            null -> state
            is Command.PauseSegment -> pauseActiveSegment(state, command)
            is Command.ResumeSegment -> resumePausedSegment(state, command)
            is Command.StartSegment -> startNextSegment(state, command)
            is Command.ResetToStart -> resetToStart(state)
            is Command.UpdateActiveSegmentLength -> state.copy(activeSegmentLength = command.seconds)
            is Command.UpdateBreakSegmentLength -> state.copy(transitionLength = command.seconds)
            is Command.UpdateTargetRepCount -> state.copy(targetRepeatCount = command.count)
        }

    private fun resetToStart(state: ActiveState): ActiveState =
        state.copy(
            activeSegment = null,
            queuedSegments = emptyList(),
            repeatsCompleted = -1,
        )

    private fun startNextSegment(state: ActiveState, command: Command.StartSegment): ActiveState =
        state.copy(
            targetRepeatCount = if (state.targetRepeatCount <= 0) -1 else state.targetRepeatCount,
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
