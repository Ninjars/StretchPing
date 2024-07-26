package jez.stretchping.features.planslist

import jez.stretchping.features.planslist.PlansListVM.State

object PlansListStateToViewState : (State) -> PlansListViewState {
    override fun invoke(state: State): PlansListViewState =
        PlansListViewState(
            plans = state.plans.map { config ->
                PlansListViewState.Plan(
                    id = config.exerciseId,
                    name = config.exerciseName,
                    isLooping = config.repeat,
                )
            }
        )
}
