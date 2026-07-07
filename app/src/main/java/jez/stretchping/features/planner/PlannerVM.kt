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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max

sealed interface PlannerUIEvent {
    data object NewSectionClicked : PlannerUIEvent
    data object StartClicked : PlannerUIEvent
    data object DeletePlanClicked : PlannerUIEvent
    data class DeleteSectionClicked(val id: String) : PlannerUIEvent
    data class UpdatePlanName(val value: String) : PlannerUIEvent
    data class UpdateIsRepeated(val value: Boolean) : PlannerUIEvent
    data class UpdateSectionName(val id: String, val value: String) : PlannerUIEvent
    data class UpdateSectionRepCount(val id: String, val value: Int) : PlannerUIEvent
    data class UpdateSectionEntryTransitionDuration(val id: String, val value: Int) : PlannerUIEvent
    data class UpdateSectionRepDuration(val id: String, val value: Int) : PlannerUIEvent
    data class UpdateSectionRepTransitionDuration(val id: String, val value: Int) : PlannerUIEvent
    data class RepositionSection(val fromIndex: Int, val toIndex: Int) : PlannerUIEvent
    data class StartFromSectionClicked(val sectionId: String) : PlannerUIEvent
}

@HiltViewModel
class PlannerVM @Inject constructor(
    private val navigationDispatcher: NavigationDispatcher,
    private val settingsRepository: SettingsRepository,
    idProvider: IdProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel(),
    Consumer<PlannerUIEvent> {
    private val planId = savedStateHandle.get<String>(Route.Planner.ROUTE_PLAN_ID)!!
    private val initialState: State = State()
    private val mutableState = MutableStateFlow(initialState)
    private val plannerEventToState = PlannerEventToState(idProvider)

    val viewState: StateFlow<PlannerViewState> =
        mutableState.toViewState(
            scope = viewModelScope,
            initial = State(),
        ) { state -> PlannerStateToViewState(state) }

    init {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                settingsRepository.exerciseConfigs
                    .map {
                        it.exercises
                            .firstOrNull { exercise -> exercise.exerciseId == planId }
                            ?.toState()
                            ?: State(id = planId)
                    }.collect {
                        mutableState.value = it
                    }
            }
        }

        viewModelScope.launch {
            mutableState.collect {
                // Persist as soon as the plan has any content, not only when it's
                // startable. Otherwise edits made while a plan is temporarily
                // invalid (e.g. a cleared rep count) are silently dropped. A
                // brand-new plan with no sections is still not written, so we
                // don't create empty-plan clutter.
                if (it.sections.isNotEmpty()) {
                    settingsRepository.saveExercise(it.toExerciseConfig())
                }
            }
        }
    }

    override fun accept(event: PlannerUIEvent) {
        // update {} applies the reducer atomically, retrying on contention, so
        // concurrent events can't clobber each other's edits (dropped keystrokes).
        mutableState.update { plannerEventToState(it, event) }

        when (event) {
            is PlannerUIEvent.DeletePlanClicked -> {
                viewModelScope.launch {
                    settingsRepository.deleteExercise(planId)
                    navigationDispatcher.navigateTo(Route.Back)
                }
            }

            is PlannerUIEvent.StartClicked -> {
                navigationDispatcher.navigateTo(
                    Route.ActiveTimer(
                        config = mutableState.value.toExerciseConfig(),
                    ),
                )
            }

            is PlannerUIEvent.StartFromSectionClicked -> {
                navigationDispatcher.navigateTo(
                    Route.ActiveTimer(
                        config = mutableState.value.toExerciseConfig(event.sectionId),
                    ),
                )
            }

            else -> Unit
        }
    }

    private fun State.toExerciseConfig(initialSectionId: String? = null) = ExerciseConfig(
        exerciseId = id,
        exerciseName = planName,
        repeat = repeat,
        sections = sections.toSectionConfig(initialSectionId),
    )

    private fun List<Section>.toSectionConfig(initialSectionId: String? = null): List<ExerciseConfig.SectionConfig> {
        val initialIndex =
            max(0, initialSectionId?.let { targetId -> indexOfFirst { it.id == targetId } } ?: 0)
        val list = if (initialIndex > 0) {
            subList(initialIndex, size)
        } else {
            this
        }
        return list.map {
            ExerciseConfig.SectionConfig(
                sectionId = it.id,
                name = it.name,
                repCount = it.repCount,
                introDuration = it.entryTransitionDuration,
                activityDuration = it.repDuration,
                transitionDuration = it.repTransitionDuration,
            )
        }
    }

    private fun ExerciseConfig.toState() = State(
        id = exerciseId,
        planName = exerciseName,
        repeat = repeat,
        sections = sections.toState(),
    )

    private fun List<ExerciseConfig.SectionConfig>.toState() = map {
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
        val canStart = sections.isNotEmpty() &&
            sections.none {
                it.repCount < 1 ||
                    it.repDuration < 1 ||
                    it.entryTransitionDuration < 0 ||
                    it.repTransitionDuration < 0
            }
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
