package jez.stretchping.features.planner

import jez.stretchping.features.planner.PlannerVM.Section
import jez.stretchping.features.planner.PlannerVM.State
import jez.stretchping.utils.IdProvider

class PlannerEventToState(private val idProvider: IdProvider) :
        (State, PlannerUIEvent) -> State {
    override fun invoke(state: State, event: PlannerUIEvent): State =
        when (event) {
            is PlannerUIEvent.NewSectionClicked ->
                state.copy(sections = state.sections + Section.create(idProvider.getId()))

            is PlannerUIEvent.UpdatePlanName ->
                state.copy(planName = event.value)

            is PlannerUIEvent.UpdateIsRepeated ->
                state.copy(repeat = event.value)

            is PlannerUIEvent.UpdateSectionEntryTransitionDuration ->
                state.copy(sections = state.sections.update(event.id) {
                    it.copy(entryTransitionDuration = event.value)
                })

            is PlannerUIEvent.UpdateSectionName ->
                state.copy(sections = state.sections.update(event.id) {
                    it.copy(name = event.value)
                })

            is PlannerUIEvent.UpdateSectionRepCount ->
                state.copy(sections = state.sections.update(event.id) {
                    it.copy(repCount = event.value)
                })

            is PlannerUIEvent.UpdateSectionRepDuration ->
                state.copy(sections = state.sections.update(event.id) {
                    it.copy(repDuration = event.value)
                })

            is PlannerUIEvent.UpdateSectionRepTransitionDuration ->
                state.copy(sections = state.sections.update(event.id) {
                    it.copy(repTransitionDuration = event.value)
                })

            is PlannerUIEvent.RepositionSection ->
                state.copy(sections = state.sections.reorder(event.fromIndex, event.toIndex))

            PlannerUIEvent.StartClicked,
            PlannerUIEvent.DeletePlanClicked -> state
        }

    private fun List<Section>.update(id: String, func: (Section) -> Section) =
        map {
            if (it.id == id) {
                func(it)
            } else {
                it
            }
        }

    private fun List<Section>.reorder(fromIndex: Int, toIndex: Int): List<Section> =
        toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
}
