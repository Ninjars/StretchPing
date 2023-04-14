package jez.stretchping.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jez.stretchping.R
import jez.stretchping.features.activetimer.ActiveTimerVM
import jez.stretchping.features.activetimer.EditTimerState


@Composable
fun PlanningActivityControls(
    state: EditTimerState,
    eventHandler: (ActiveTimerVM.Event) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth(0.9f),
    ) {
        val focusManager = LocalFocusManager.current

        // Edit Stretch Duration
        SelectOnFocusTextField(
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
                Text(stringResource(id = R.string.stretch_duration))
            },
        ) {
            eventHandler(ActiveTimerVM.Event.UpdateStretchDuration(it))
        }

        // Edit Transition Duration
        SelectOnFocusTextField(
            text = state.breakDuration,
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
            eventHandler(ActiveTimerVM.Event.UpdateBreakDuration(it))
        }

        // Edit Rep Count
        val repCount = state.repCount
        SelectOnFocusTextField(
            text = repCount,
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
            eventHandler(ActiveTimerVM.Event.UpdateRepCount(it))
        }

        // Theme Selection
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(id = R.string.theme_title))
            TriStateToggle(
                states = state.themeState.optionStringResources.map { stringResource(id = it) },
                selectedIndex = state.themeState.selectedIndex,
            ) {
                eventHandler(ActiveTimerVM.Event.UpdateTheme(it))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectOnFocusTextField(
    text: String,
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
            TextFieldDefaults.textFieldColors(
                containerColor = secondaryContainer,
                textColor = onSecondaryContainer,
            )
        },
    )
}
