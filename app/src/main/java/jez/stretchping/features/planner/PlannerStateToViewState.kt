package jez.stretchping.features.planner

import jez.stretchping.features.planner.PlannerVM.State

object PlannerStateToViewState : (State) -> PlannerViewState {
    override fun invoke(state: State): PlannerViewState =
        with(state) {
            PlannerViewState(
                isInitialised = state.id.isNotBlank(),
                planName = planName,
                repeat = repeat,
                canStart = canStart,
                sections = sections.map {
                    PlannerViewState.Section(
                        it.id,
                        it.name,
                        it.repCount.toString(),
                        it.entryTransitionDuration.toString(),
                        it.repDuration.toString(),
                        it.repTransitionDuration.toString(),
                    )
                }
            )
        }
}
