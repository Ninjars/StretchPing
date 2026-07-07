package jez.stretchping.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import dev.redfoxstudio.stretchping.R
import kotlin.math.max

@Composable
fun FocusingInputFieldWithPicker(
    text: String,
    pickerOptions: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    label: @Composable (() -> Unit)? = null,
    pickerTitle: String? = null,
    suffix: @Composable (() -> Unit)? = null,
) {
    var showDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    SelectOnFocusTextField(
        text = text,
        modifier = modifier,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = colors,
        useOutlinedTextField = true,
        label = label,
        suffix = suffix,
        trailingIcon = {
            IconButton(
                onClick = {
                    // Clear focus so the text field adopts the picked value:
                    // SelectOnFocusTextField treats the local text as
                    // authoritative while focused, and a Compose Dialog does not
                    // blur the field underneath it.
                    focusManager.clearFocus()
                    showDialog = true
                },
            ) {
                Icon(
                    imageVector = Icons.Default.AvTimer,
                    contentDescription = stringResource(R.string.desc_radial_picker),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        onValueChange = onValueChange,
    )
    if (showDialog) {
        RadialPicker(
            values = pickerOptions,
            initialSelectedIndex = max(0, pickerOptions.indexOfFirst { it == text }),
            onDismissRequest = { showDialog = false },
            title = pickerTitle,
        ) {
            onValueChange(pickerOptions[it])
            showDialog = false
        }
    }
}
