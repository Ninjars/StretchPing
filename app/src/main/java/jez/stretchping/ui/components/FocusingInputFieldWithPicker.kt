package jez.stretchping.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.redfoxstudio.stretchping.R
import kotlin.math.max

@Composable
fun FocusingInputFieldWithPicker(
    text: String,
    pickerOptions: List<String>,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    label: @Composable (() -> Unit)? = null,
    onValueChanged: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        SelectOnFocusTextField(
            text = text,
            modifier = Modifier.weight(1f),
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = colors,
            useOutlinedTextField = true,
            label = label,
            onValueChanged = onValueChanged,
        )
        Button(
            onClick = {
                // Clear focus so the text field adopts the picked value:
                // SelectOnFocusTextField treats the local text as
                // authoritative while focused, and a Compose Dialog does not
                // blur the field underneath it.
                focusManager.clearFocus()
                showDialog = true
            },
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AvTimer,
                contentDescription = stringResource(R.string.desc_radial_picker),
                modifier = Modifier.size(32.dp)
            )
        }
    }
    if (showDialog) {
        RadialPicker(
            values = pickerOptions,
            initialSelectedIndex = max(0, pickerOptions.indexOfFirst { it == text }),
            onDismissRequest = { showDialog = false }
        ) {
            onValueChanged(pickerOptions[it])
            showDialog = false
        }
    }
}
