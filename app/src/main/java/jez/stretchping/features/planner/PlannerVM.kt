package jez.stretchping.features.planner

import androidx.core.util.Consumer
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.map
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
    savedStateHandle: SavedStateHandle,
) : Consumer<PlannerUIEvent>, ViewModel() {
    private val planId = savedStateHandle.get<String>(Route.Planner.routePlanId)!!
    private val initialState: State = State()
    private val mutableState = MutableStateFlow(initialState)
    private val plannerEventToState = PlannerEventToState(idProvider)

    val viewState: StateFlow<PlannerViewState> =
        mutableState.toViewState(
            scope = viewModelScope,
            initial = State()
        ) { state -> PlannerStateToViewState(state) }

    init {
        viewModelScope.launch {
            settingsRepository.exerciseConfigs
                .map {
                    it.exercises
                        .first { exercise -> exercise.exerciseId == planId }
                        .toState()
                }.collect {
                    mutableState.value = it
                }
        }
    }

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

    private fun ExerciseConfig.toState() =
        State(
            id = exerciseId,
            planName = exerciseName,
            repeat = repeat,
            sections = sections.toState(),
        )

    private fun List<ExerciseConfig.SectionConfig>.toState() =
        map {
            Section(
                id = it.sectionId,
                name = it.name,
                repCount = it.repCount,
                entryTransitionDuration = it.introDuration,
                repDuration = it.activityDuration,
                repTransitionDuration = it.transitionDuration,
            )
        }

    data class State(
        val id: String = "",
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
}
