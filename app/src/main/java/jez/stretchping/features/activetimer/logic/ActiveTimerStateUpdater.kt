package jez.stretchping.features.activetimer.logic

import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.Command
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine.State

internal object ActiveTimerStateUpdater : (State, Command?) -> State {

    override fun invoke(state: State, command: Command?): State =
        when (command) {
            null -> state
            is Command.PauseSegment -> pauseActiveSegment(state, command)
            is Command.ResumeSegment -> resumePausedSegment(state, command)
            is Command.StartSegment -> startSegment(state, command)
            is Command.RepeatExercise -> restart(state, command)
            is Command.SequenceCompleted,
            is Command.GoBack -> state
        }

    private fun startSegment(state: State, command: Command.StartSegment): State =
        state.copy(
            hasStarted = true,
            activeSegment = state.segments[command.index].toActiveSegment(command.startMillis),
            index = command.index,
        )

    private fun restart(state: State, command: Command.RepeatExercise): State =
        state.copy(
            hasStarted = true,
            activeSegment = state.segments[0].toActiveSegment(command.startMillis),
            completedReps = state.completedReps + 1,
            index = 0,
        )

    private fun State.SegmentSpec.toActiveSegment(startMillis: Long): State.ActiveSegment =
        State.ActiveSegment(
            startedAtTime = startMillis,
            startedAtFraction = 0f,
            endAtTime = startMillis + durationSeconds.toMillis(),
            pausedAtFraction = null,
            pausedAtTime = null,
            spec = this,
        )

    private fun Int.toMillis() = this * 1000

    private fun resumePausedSegment(
        state: State,
        command: Command.ResumeSegment
    ): State =
        state.copy(
            hasStarted = true,
            activeSegment = command.pausedSegment.toResumed(
                command.startMillis,
                command.startFraction,
            )
        )

    private fun State.ActiveSegment.toResumed(
        currentTimeMillis: Long,
        currentFraction: Float,
    ): State.ActiveSegment {
        return copy(
            endAtTime = currentTimeMillis + this.remainingDurationMillis,
            startedAtTime = currentTimeMillis,
            startedAtFraction = currentFraction,
            pausedAtFraction = null,
            pausedAtTime = null,
        )
    }

    private fun pauseActiveSegment(state: State, command: Command.PauseSegment): State =
        state.copy(
            hasStarted = true,
            activeSegment = command.runningSegment.toPaused(command.pauseMillis)
        )

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
