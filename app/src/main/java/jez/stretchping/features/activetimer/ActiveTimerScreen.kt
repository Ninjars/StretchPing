package jez.stretchping.features.activetimer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import jez.stretchping.features.activetimer.ActiveTimerVM.Event
import jez.stretchping.utils.rememberEventConsumer

@Composable
fun ActiveTimerScreen(
    viewModel: ActiveTimerVM
) {
    ActiveTimerScreen(viewModel.viewState.collectAsState(), rememberEventConsumer(viewModel))
}

@Composable
private fun ActiveTimerScreen(
    state: State<ActiveTimerViewState>,
    eventHandler: (Event) -> Unit,
) {

}
