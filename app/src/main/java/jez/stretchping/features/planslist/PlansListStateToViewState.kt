package jez.stretchping.features.planslist

import jez.stretchping.features.planslist.PlansListVM.State
import jez.stretchping.persistence.ExerciseConfig

object PlansListStateToViewState : (State) -> PlansListViewState {
    override fun invoke(state: State): PlansListViewState =
        PlansListViewState(
            plans = state.plans.map { config ->
                PlansListViewState.Plan(
                    id = config.exerciseId,
                    name = config.exerciseName,
                    isLooping = config.repeat,
                    canStart = config.isValid(),
                )
            }
        )
}

private fun ExerciseConfig.isValid(): Boolean =
    exerciseId.isNotEmpty()
            && exerciseName.isNotBlank()
            && sections.isNotEmpty()
            && sections.none {
        it.repCount < 1
                || it.activityDuration < 1
                || it.transitionDuration < 0
                || it.introDuration < 0
    }
