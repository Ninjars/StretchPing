package jez.stretchping.features.edittimer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.redfoxstudio.stretchping.R
import jez.stretchping.ui.components.SelectOnFocusTextField
import jez.stretchping.ui.components.TimerControls
import jez.stretchping.ui.components.TimerControlsEvent
import jez.stretchping.ui.components.TimerControlsViewState
import jez.stretchping.ui.theme.secondaryTextFieldColors
import jez.stretchping.utils.rememberEventConsumer

sealed class EditTimerEvent {
    data object Start : EditTimerEvent()
    data class UpdateActiveDuration(val duration: String) : EditTimerEvent()
    data class UpdateTransitionDuration(val duration: String) : EditTimerEvent()
    data class UpdateRepCount(val count: String) : EditTimerEvent()
}


@Composable
fun EditTimerScreen(
    viewModel: EditTimerVM
) {
    EditTimerScreen(
        viewModel.viewState.collectAsState(),
        rememberEventConsumer(viewModel)
    )
}

@Composable
private fun EditTimerScreen(
    viewState: State<EditTimerViewState>,
    eventHandler: (EditTimerEvent) -> Unit,
) {
    val localFocusManager = LocalFocusManager.current
    val state = viewState.value
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    localFocusManager.clearFocus()
                })
            },
    ) {
        Settings(state, eventHandler)

        TimerControls(
            eventHandler = {
                when (it) {
                    TimerControlsEvent.PlayClicked -> eventHandler(EditTimerEvent.Start)
                    TimerControlsEvent.PauseClicked,
                    TimerControlsEvent.ResetClicked,
                    TimerControlsEvent.BackClicked -> Unit
                }
            },
        ) {
            with(state) {
                TimerControlsViewState(
                    mainButtonEnabled = canStart,
                    showMainButton = true,
                    showBackWhenPaused = false,
                    showResetSegment = false,
                    isPaused = true,
                )
            }
        }
    }
}

@Composable
private fun Settings(
    state: EditTimerViewState,
    eventHandler: (EditTimerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        val focusManager = LocalFocusManager.current

        // Edit Stretch Duration
        SelectOnFocusTextField(
            modifier = Modifier.fillMaxWidth(),
            text = state.activeDuration,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 30.sp
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions {
                focusManager.moveFocus(FocusDirection.Next)
            },
            colors = secondaryTextFieldColors(),
            label = {
                Text(stringResource(id = R.string.active_duration))
            },
        ) {
            eventHandler(EditTimerEvent.UpdateActiveDuration(it))
        }

        // Edit Transition Duration
        SelectOnFocusTextField(
            modifier = Modifier.fillMaxWidth(),
            text = state.transitionDuration,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 30.sp
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions {
                focusManager.moveFocus(FocusDirection.Next)
            },
            colors = secondaryTextFieldColors(),
            label = {
                Text(stringResource(id = R.string.transition_duration))
            },
        ) {
            eventHandler(EditTimerEvent.UpdateTransitionDuration(it))
        }

        // Edit Rep Count
        SelectOnFocusTextField(
            modifier = Modifier.fillMaxWidth(),
            text = state.repCount,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 30.sp
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            colors = secondaryTextFieldColors(),
            label = {
                Text(stringResource(id = R.string.rep_count))
            },
        ) {
            eventHandler(EditTimerEvent.UpdateRepCount(it))
        }
    }
}
