package jez.stretchping.features.home

import androidx.core.util.Consumer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jez.stretchping.features.home.HomeScreenVM.State.LaunchMode
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
) : Consumer<HomeScreenEvent>, ViewModel() {
    private val initialState = State(LaunchMode.Unknown, NavLabelDisplayMode.Unset)
    private val mutableState: MutableStateFlow<State> = MutableStateFlow(initialState)
    val state: StateFlow<State> = mutableState.toViewState(
        scope = viewModelScope,
        initial = initialState,
    ) { it }

    init {
        viewModelScope.launch {
            settingsRepository.hasLaunched.collect { hasLaunched ->
                with(mutableState.value) {
                    mutableState.value = copy(
                        launchMode = when (launchMode) {
                            LaunchMode.Unknown -> if (hasLaunched) LaunchMode.NormalLaunch else LaunchMode.FirstLaunch
                            LaunchMode.FirstLaunch,
                            LaunchMode.NormalLaunch -> launchMode
                        }
                    )
                }
            }
            settingsRepository.showNavLabels.collect { displayMode ->
                with(mutableState.value) {
                    mutableState.value = copy(
                        navLabelDisplayMode = displayMode
                    )
                }
            }
        }
    }

    override fun accept(value: HomeScreenEvent) {
        viewModelScope.launch {
            settingsRepository.setHasLaunched()
        }
    }

    data class State(
        val launchMode: LaunchMode,
        val navLabelDisplayMode: NavLabelDisplayMode,
    ) {
        enum class LaunchMode {
            Unknown, FirstLaunch, NormalLaunch,
        }
    }
}
