package jez.stretchping.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.math.max

@Composable
fun SelectOnFocusTextField(
    text: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    useOutlinedTextField: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
) {
    // The local TextFieldValue is the single source of truth while the user is
    // typing. Upstream `text` updates arrive asynchronously (onValueChange ->
    // VM state flow -> recomposition), so a recomposition can carry a stale
    // echo of a value from *before* the keystroke currently in flight. Blindly
    // resyncing local state from `text` on every composition (as the previous
    // implementation did) would revert freshly-typed characters or jump the
    // cursor whenever typing outran the round-trip.
    //
    // While the field is focused the local value therefore always wins: the
    // user owns the text and upstream is only ever catching up to it. Upstream
    // is authoritative again once the field loses focus (see the focus
    // LaunchedEffect below) and while unfocused (external changes such as a
    // radial-picker selection, VM normalisation, or the initial value), which
    // is handled here.
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val fieldState = remember { mutableStateOf(TextFieldValue(text)) }

    if (!isFocused && text != fieldState.value.text) {
        // Unfocused: upstream is authoritative. Adopt the external value and
        // place the cursor sensibly relative to the previous content.
        val current = fieldState.value
        val oldPosition = current.selection.end
        val newPosition = max(0, oldPosition + text.length - current.text.length)
        fieldState.value = current.copy(
            text = text,
            selection = TextRange(newPosition),
        )
    }

    LaunchedEffect(isFocused) {
        fieldState.value = if (isFocused) {
            // Select-all on focus so a new value replaces the old one.
            fieldState.value.copy(
                selection = TextRange(
                    start = 0,
                    end = fieldState.value.text.length,
                ),
            )
        } else {
            // On blur, re-sync from the authoritative upstream value in case
            // local and upstream diverged (e.g. the VM normalised the input),
            // and collapse the selection.
            TextFieldValue(text = text, selection = TextRange.Zero)
        }
    }

    val onFieldValueChange: (TextFieldValue) -> Unit = {
        fieldState.value = it
        onValueChange(it.text)
    }

    if (useOutlinedTextField) {
        OutlinedTextField(
            modifier = modifier,
            interactionSource = interactionSource,
            value = fieldState.value,
            label = label,
            placeholder = placeholder,
            trailingIcon = trailingIcon,
            suffix = suffix,
            textStyle = textStyle,
            onValueChange = onFieldValueChange,
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
            placeholder = placeholder,
            trailingIcon = trailingIcon,
            suffix = suffix,
            textStyle = textStyle,
            onValueChange = onFieldValueChange,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            colors = colors,
        )
    }
}
