package jez.stretchping.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jez.stretchping.persistence.NavLabelDisplayMode
import jez.stretchping.persistence.SettingsRepository
import jez.stretchping.utils.toViewState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * This is a super-simple VM as most of the feature functionality
 * is covered by the HorizontalPager in the Screen.
 *
 * If any additional logic and state is required then the full
 * state data class and view mapping should be added, but for now
 * we can keep it simple and bare-bones.
 */
@HiltViewModel
class HomeScreenVM @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<NavLabelDisplayMode> = settingsRepository.showNavLabels.toViewState(
        scope = viewModelScope,
        initial = NavLabelDisplayMode.Unset,
    ) { it }
}
