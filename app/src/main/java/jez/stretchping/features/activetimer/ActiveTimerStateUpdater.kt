package jez.stretchping.features.activetimer

import jez.stretchping.features.activetimer.ActiveTimerVM.Command
import jez.stretchping.features.activetimer.ActiveTimerVM.State

internal object ActiveTimerStateUpdater : (State, Command?) -> State {

    override fun invoke(state: State, command: Command?): State =
        when (command) {
            null -> state
            is Command.PauseSegment -> pauseActiveSegment(state, command)
            is Command.ResumeSegment -> resumePausedSegment(state, command)
            is Command.StartSegment -> startNextSegment(state, command)
            is Command.ResetToStart -> resetToStart(state, command.segments)
        }

    private fun resetToStart(state: State, segments: List<State.SegmentSpec>): State =
        state.copy(
            activeSegment = null,
            queuedSegments = segments,
            repeatsCompleted = state.targetRepeatCount,
        )

    private fun startNextSegment(state: State, command: Command.StartSegment): State =
        state.copy(
            activeSegment = command.toActiveSegment(),
            queuedSegments = command.queuedSegments,
            repeatsCompleted = if (command.queuedSegments.isEmpty()) {
                state.repeatsCompleted + 1
            } else {
                state.repeatsCompleted
            },
        )

    private fun Command.StartSegment.toActiveSegment(): State.ActiveSegment =
        State.ActiveSegment(
            startedAtTime = startMillis,
            startedAtFraction = 0f,
            endAtTime = startMillis + segmentSpec.durationSeconds.toMillis(),
            pausedAtFraction = null,
            pausedAtTime = null,
            spec = segmentSpec,
        )

    private fun Int.toMillis() = this * 1000

    private fun resumePausedSegment(state: State, command: Command.ResumeSegment): State =
        state.copy(
            activeSegment = command.pausedSegment.toResumed(
                command.startMillis,
                command.startFraction,
                command.remainingDurationMillis,
            )
        )

    private fun State.ActiveSegment.toResumed(
        currentTimeMillis: Long,
        currentFraction: Float,
        remainingDurationMillis: Long
    ): State.ActiveSegment {
        return copy(
            endAtTime = currentTimeMillis + remainingDurationMillis,
            startedAtTime = currentTimeMillis,
            startedAtFraction = currentFraction,
            pausedAtFraction = null,
            pausedAtTime = null,
        )
    }

    private fun pauseActiveSegment(state: State, command: Command.PauseSegment): State =
        state.copy(activeSegment = command.runningSegment.toPaused(command.pauseMillis))

    private fun State.ActiveSegment.toPaused(currentTimeMillis: Long): State.ActiveSegment {
        val scalingFactor = 1f - startedAtFraction
        val currentFraction =
            (currentTimeMillis - startedAtTime).toDouble() / (endAtTime - startedAtTime).toDouble()
        return copy(
            pausedAtTime = currentTimeMillis,
            pausedAtFraction = (startedAtFraction + scalingFactor * currentFraction).toFloat(),
        )
    }
}
