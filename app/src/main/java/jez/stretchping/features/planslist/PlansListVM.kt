package jez.stretchping.features.planslist

import androidx.core.util.Consumer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jez.stretchping.NavigationDispatcher
import jez.stretchping.Route
import jez.stretchping.persistence.ExerciseConfig
import jez.stretchping.persistence.ExerciseConfigs
import jez.stretchping.persistence.SettingsRepository
import jez.stretchping.utils.IdProvider
import jez.stretchping.utils.toViewState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PlansListUIEvent {
    data object NewPlanClicked : PlansListUIEvent
    data class OpenPlanClicked(val id: String) : PlansListUIEvent
    data class StartPlanClicked(val id: String) : PlansListUIEvent
    data class DeletePlanClicked(val id: String) : PlansListUIEvent
}

@HiltViewModel
class PlansListVM @Inject constructor(
    private val navigationDispatcher: NavigationDispatcher,
    private val settingsRepository: SettingsRepository,
    private val idProvider: IdProvider,
) : Consumer<PlansListUIEvent>, ViewModel() {

    private val state = settingsRepository.exerciseConfigs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = ExerciseConfigs(emptyList())
        ).map { State(it.exercises) }

    val viewState: StateFlow<PlansListViewState> =
        state.toViewState(
            scope = viewModelScope,
            initial = State(emptyList())
        ) { state -> PlansListStateToViewState(state) }

    override fun accept(event: PlansListUIEvent) {
        when (event) {
            is PlansListUIEvent.DeletePlanClicked ->
                viewModelScope.launch {
                    settingsRepository.deleteExercise(event.id)
                }

            is PlansListUIEvent.StartPlanClicked ->
                viewModelScope.launch {
                    Route.ActiveTimer(
                        config = state.first().plans.firstOrNull { it.exerciseId == event.id }
                            ?: throw IllegalStateException("plan with id ${event.id} not found")
                    )
                }

            is PlansListUIEvent.NewPlanClicked ->
                navigationDispatcher.navigateTo(
                    Route.Planner(idProvider.getId())
                )

            is PlansListUIEvent.OpenPlanClicked ->
                navigationDispatcher.navigateTo(
                    Route.Planner(event.id)
                )
        }
    }

    data class State(
        val plans: List<ExerciseConfig> = emptyList(),
    )
}
