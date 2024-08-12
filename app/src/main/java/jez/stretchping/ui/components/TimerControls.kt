package jez.stretchping.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jez.stretchping.R
import jez.stretchping.ui.components.TimerControlsEvent.BackClicked
import jez.stretchping.ui.components.TimerControlsEvent.PauseClicked
import jez.stretchping.ui.components.TimerControlsEvent.PlayClicked

private const val SecondaryButtonSize = 60
private const val PrimaryButtonSize = 84

sealed class TimerControlsEvent {
    object PlayClicked : TimerControlsEvent()
    object PauseClicked : TimerControlsEvent()
    object BackClicked : TimerControlsEvent()
}

data class TimerControlsViewState(
    val showMainButton: Boolean,
    val mainButtonEnabled: Boolean,
    val showBackWhenPaused: Boolean,
    val isPaused: Boolean,
)

@Composable
fun TimerControls(
    eventHandler: (TimerControlsEvent) -> Unit,
    modifier: Modifier = Modifier,
    stateProvider: () -> TimerControlsViewState,
) {
    val state = stateProvider()

    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = 48.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Crossfade(targetState = state.isPaused && state.showBackWhenPaused) { showReset ->
            if (showReset) {
                CircleButton(
                    onClick = { eventHandler(BackClicked) },
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = stringResource(id = R.string.timer_reset),
                    modifier = Modifier
                        .size(SecondaryButtonSize.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    )
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .size(SecondaryButtonSize.dp),
                )
            }
        }

        MainButton(
            eventHandler = eventHandler,
            state = state,
            enabled = state.mainButtonEnabled,
            modifier = Modifier
                .size(PrimaryButtonSize.dp)
        )

        Spacer(
            modifier = Modifier
                .size(SecondaryButtonSize.dp),
        )
    }
}

@Composable
private fun MainButton(
    eventHandler: (TimerControlsEvent) -> Unit,
    state: TimerControlsViewState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val isPaused = state.isPaused
    CircleButton(
        onClick = {
            when (isPaused) {
                true -> eventHandler(PlayClicked)
                false -> eventHandler(PauseClicked)
            }
        },
        imageVector = when (isPaused) {
            true -> Icons.Rounded.PlayArrow
            false -> Icons.Rounded.Pause
        },
        contentDescription = when (isPaused) {
            true -> stringResource(id = R.string.timer_start)
            false -> stringResource(id = R.string.timer_pause)
        },
        enabled = enabled,
        modifier = modifier,
    )
}

@Composable
private fun CircleButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = colors,
        contentPadding = PaddingValues(8.dp),
        enabled = enabled,
        modifier = modifier
    ) {
        Crossfade(targetState = imageVector) {
            Icon(
                imageVector = it,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
