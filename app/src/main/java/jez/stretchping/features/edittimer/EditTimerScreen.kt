@file:OptIn(ExperimentalPermissionsApi::class)

package jez.stretchping.features.edittimer

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import jez.stretchping.R
import jez.stretchping.ui.components.IntSliderControl
import jez.stretchping.ui.components.TimerControls
import jez.stretchping.ui.components.TimerControlsEvent
import jez.stretchping.ui.components.TimerControlsViewState
import jez.stretchping.ui.components.TriStateToggle
import jez.stretchping.utils.rememberEventConsumer

sealed class EditTimerEvent {
    data object Start : EditTimerEvent()
    data class UpdateActiveDuration(val duration: String) : EditTimerEvent()
    data class UpdateTransitionDuration(val duration: String) : EditTimerEvent()
    data class UpdateActivePings(val count: Int) : EditTimerEvent()
    data class UpdateTransitionPings(val count: Int) : EditTimerEvent()
    data class UpdateRepCount(val count: String) : EditTimerEvent()
    data class UpdateTheme(val themeModeIndex: Int) : EditTimerEvent()
    data class UpdatePlayInBackground(val enabled: Boolean) : EditTimerEvent()
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
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    localFocusManager.clearFocus()
                })
            },
    ) {
        Settings(state, eventHandler, Modifier.weight(1f))

        TimerControls(
            eventHandler = {
                when (it) {
                    TimerControlsEvent.PlayClicked -> eventHandler(EditTimerEvent.Start)
                    TimerControlsEvent.PauseClicked,
                    TimerControlsEvent.BackClicked -> Unit
                }
            },
        ) {
            with(state) {
                TimerControlsViewState(
                    mainButtonEnabled = canStart,
                    showMainButton = true,
                    showBackWhenPaused = false,
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
    modifier: Modifier,
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
            label = {
                Text(stringResource(id = R.string.rep_count))
            },
        ) {
            eventHandler(EditTimerEvent.UpdateRepCount(it))
        }

        AdvancedSettings(
            state = state,
            eventHandler = eventHandler,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AdvancedSettings(
    state: EditTimerViewState,
    eventHandler: (EditTimerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSettings by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            modifier = Modifier.clipToBounds(),
            visible = showSettings,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                // Edit number of pings during active sections
                IntSliderControl(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.active_ping_title_count),
                    value = state.activePings,
                    onValueChange = { eventHandler(EditTimerEvent.UpdateActivePings(it)) },
                    maxValue = 10.coerceAtMost(state.activeDuration.toIntOrNull() ?: Int.MAX_VALUE),
                )

                // Edit number of pings during transition sections
                IntSliderControl(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.transition_ping_title_count),
                    value = state.transitionPings,
                    onValueChange = { eventHandler(EditTimerEvent.UpdateTransitionPings(it)) },
                    maxValue = 10.coerceAtMost(
                        state.transitionDuration.toIntOrNull() ?: Int.MAX_VALUE
                    ),
                )

                // Auto Pause
                val notificationPermissionState = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS) {
                    eventHandler(EditTimerEvent.UpdatePlayInBackground(true))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.background_play),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Checkbox(
                        checked = state.playInBackground,
                        onCheckedChange = { enabled ->
                            if (enabled && !notificationPermissionState.status.isGranted) {
                                notificationPermissionState.launchPermissionRequest()
                            } else {
                                eventHandler(EditTimerEvent.UpdatePlayInBackground(enabled))
                            }
                        }
                    )
                }

                // Theme Selection
                Text(
                    text = stringResource(R.string.theme_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                TriStateToggle(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    states = state.themeState.optionStringResources.map { stringResource(id = it) },
                    selectedIndex = state.themeState.selectedIndex,
                ) {
                    eventHandler(EditTimerEvent.UpdateTheme(it))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showSettings = !showSettings },
            shape = CircleShape,
            contentPadding = PaddingValues(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.settings),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun SelectOnFocusTextField(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    label: @Composable (() -> Unit)? = null,
    onValueChanged: (String) -> Unit,
) {
    val fieldState = remember { mutableStateOf(TextFieldValue(text)) }

    val newTextRange = with(fieldState.value) {
        if (text != this.text) {
            val oldPosition = this.selection.end
            val newPosition = oldPosition + text.length - this.text.length
            TextRange(newPosition)
        } else {
            fieldState.value.selection
        }
    }
    fieldState.value = fieldState.value.copy(
        text = text,
        selection = newTextRange
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(isFocused) {
        fieldState.value = fieldState.value.copy(
            selection = if (isFocused) {
                TextRange(
                    start = 0,
                    end = fieldState.value.text.length
                )
            } else {
                TextRange.Zero
            }
        )
    }

    TextField(
        modifier = modifier,
        interactionSource = interactionSource,
        value = fieldState.value,
        label = label,
        textStyle = textStyle,
        onValueChange = {
            fieldState.value = it
            onValueChanged(it.text)
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        colors = with(MaterialTheme.colorScheme) {
            TextFieldDefaults.colors(
                focusedContainerColor = secondaryContainer,
                unfocusedContainerColor = secondaryContainer,
                focusedTextColor = onSecondaryContainer,
                unfocusedTextColor = onSecondaryContainer,
            )
        },
    )
}
