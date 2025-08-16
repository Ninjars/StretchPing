package jez.stretchping.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jez.stretchping.persistence.NavLabelDisplayMode
import jez.stretchping.persistence.SettingsRepository
import jez.stretchping.utils.toViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenVM @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val initialState = State(NavLabelDisplayMode.Unset)
    private val mutableState: MutableStateFlow<State> = MutableStateFlow(initialState)
    val state: StateFlow<State> = mutableState.toViewState(
        scope = viewModelScope,
        initial = initialState,
    ) { it }

    init {
        viewModelScope.launch {
            settingsRepository.showNavLabels.collect { displayMode ->
                with(mutableState.value) {
                    mutableState.value = copy(
                        navLabelDisplayMode = displayMode
                    )
                }
            }
        }
    }

    data class State(
        val navLabelDisplayMode: NavLabelDisplayMode,
    )
}
