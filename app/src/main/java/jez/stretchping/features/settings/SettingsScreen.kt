package jez.stretchping.features.settings

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import jez.stretchping.R
import jez.stretchping.ui.components.IntSliderControl
import jez.stretchping.ui.components.TriStateToggle
import jez.stretchping.utils.rememberEventConsumer

data class SettingsViewState(
    val transitionPings: Int,
    val activePings: Int,
    val playInBackground: Boolean,
    val themeState: TriOptionsState,
    val navLabelsState: TriOptionsState,
    val versionInfo: String,
) {
    data class TriOptionsState(
        val optionStringResources: List<Int>,
        val selectedIndex: Int,
    )
}

sealed interface SettingsScreenEvent {
    data class UpdateActivePings(val count: Int) : SettingsScreenEvent
    data class UpdateTransitionPings(val count: Int) : SettingsScreenEvent
    data class UpdateTheme(val themeModeIndex: Int) : SettingsScreenEvent
    data class UpdatePlayInBackground(val enabled: Boolean) : SettingsScreenEvent
    data class UpdateShowLabels(val optionIndex: Int) : SettingsScreenEvent
}

@Composable
fun SettingsScreen(viewModel: SettingsVM) {
    Screen(
        viewModel.viewState.collectAsState(),
        rememberEventConsumer(viewModel)
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun Screen(
    stateFlow: State<SettingsViewState>,
    eventHandler: (SettingsScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = stateFlow.value
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        // Edit number of pings during active sections
        IntSliderControl(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.active_ping_title_count),
            value = state.activePings,
            onValueChange = { eventHandler(SettingsScreenEvent.UpdateActivePings(it)) },
            maxValue = 10,
        )

        // Edit number of pings during transition sections
        IntSliderControl(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.transition_ping_title_count),
            value = state.transitionPings,
            onValueChange = { eventHandler(SettingsScreenEvent.UpdateTransitionPings(it)) },
            maxValue = 10,
        )

        // Auto Pause
        val notificationPermissionState =
            rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS) {
                eventHandler(SettingsScreenEvent.UpdatePlayInBackground(true))
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
                        eventHandler(SettingsScreenEvent.UpdatePlayInBackground(enabled))
                    }
                }
            )
        }

        // Nav labels
        Text(
            text = stringResource(R.string.nav_label_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )
        TriStateToggle(
            states = state.navLabelsState.optionStringResources.map { stringResource(id = it) },
            selectedIndex = state.navLabelsState.selectedIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            eventHandler(SettingsScreenEvent.UpdateShowLabels(it))
        }

        // Theme Selection
        Text(
            text = stringResource(R.string.theme_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )
        TriStateToggle(
            states = state.themeState.optionStringResources.map { stringResource(id = it) },
            selectedIndex = state.themeState.selectedIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            eventHandler(SettingsScreenEvent.UpdateTheme(it))
        }

        // Version
        Text(
            text = state.versionInfo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
