package jez.stretchping.ui.components


import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun SelectOnFocusTextField(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    colors: TextFieldColors = TextFieldDefaults.colors(),
    useOutlinedTextField: Boolean = false,
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

    if (useOutlinedTextField) {
        OutlinedTextField(
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
            colors = colors,
        )
    } else {
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
            colors = colors,
        )
    }
}