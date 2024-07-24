package jez.stretchping.features.planner

import androidx.core.util.Consumer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jez.stretchping.NavigationDispatcher
import jez.stretchping.Route
import jez.stretchping.persistence.ExerciseConfig
import jez.stretchping.persistence.SettingsRepository
import jez.stretchping.utils.IdProvider
import jez.stretchping.utils.toViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PlannerUIEvent {
    data object NewSectionClicked : PlannerUIEvent
    data object Start : PlannerUIEvent
    data class UpdatePlanName(val value: String) : PlannerUIEvent
    data class UpdateIsRepeated(val value: Boolean) : PlannerUIEvent
    data class UpdateSectionName(val id: String, val value: String) : PlannerUIEvent
    data class UpdateSectionRepCount(val id: String, val value: Int) : PlannerUIEvent
    data class UpdateSectionEntryTransitionDuration(val id: String, val value: Int) : PlannerUIEvent
    data class UpdateSectionRepDuration(val id: String, val value: Int) : PlannerUIEvent
    data class UpdateSectionRepTransitionDuration(val id: String, val value: Int) : PlannerUIEvent
}

@HiltViewModel
class PlannerVM @Inject constructor(
    private val navigationDispatcher: NavigationDispatcher,
    private val settingsRepository: SettingsRepository,
    idProvider: IdProvider,
) : Consumer<PlannerUIEvent>, ViewModel() {

    // TODO: launch with a plan id and initialise from there
    private val initialState: State = State(id = idProvider.getId())
    private val mutableState = MutableStateFlow(initialState)
    private val plannerEventToState = PlannerEventToState(idProvider)

    val viewState: StateFlow<PlannerViewState> =
        mutableState.toViewState(
            scope = viewModelScope,
            initial = initialState
        ) { state -> PlannerStateToViewState(state) }

    override fun accept(event: PlannerUIEvent) {
        viewModelScope.launch {
            with(mutableState.value) {
                mutableState.compareAndSet(this, plannerEventToState(this, event))
            }
        }

        viewModelScope.launch {
            mutableState.collect {
                settingsRepository.saveExercise(it.toExerciseConfig())
            }
        }

        if (event is PlannerUIEvent.Start) {
            navigationDispatcher.navigateTo(
                Route.ActiveTimer(
                    config = mutableState.value.toExerciseConfig(),
                )
            )
        }
    }

    private fun State.toExerciseConfig() =
        ExerciseConfig(
            exerciseId = id,
            exerciseName = planName,
            repeat = repeat,
            sections = sections.toSectionConfig(),
        )

    private fun List<Section>.toSectionConfig() =
        map {
            ExerciseConfig.SectionConfig(
                sectionId = it.id,
                name = it.name,
                repCount = it.repCount,
                introDuration = it.entryTransitionDuration,
                activityDuration = it.repDuration,
                transitionDuration = it.repTransitionDuration,
            )
        }

    data class State(
        val id: String,
        val planName: String = "",
        val repeat: Boolean = false,
        val sections: List<Section> = emptyList(),
    ) {
        val canStart = sections.isNotEmpty()
                && sections.firstOrNull { it.repCount == 0 || it.repDuration == 0 } == null
    }

    data class Section(
        val id: String,
        val name: String,
        val repCount: Int,
        val entryTransitionDuration: Int,
        val repDuration: Int,
        val repTransitionDuration: Int,
    ) {
        companion object {
            fun create(id: String) = Section(
                id = id,
                name = "",
                repCount = 1,
                entryTransitionDuration = 5,
                repDuration = 30,
                repTransitionDuration = 3,
            )
        }
    }

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

                PlannerUIEvent.Start -> state
            }

        private fun List<Section>.update(id: String, func: (Section) -> Section) =
            map {
                if (it.id == id) {
                    func(it)
                } else {
                    it
                }
            }
    }
}
