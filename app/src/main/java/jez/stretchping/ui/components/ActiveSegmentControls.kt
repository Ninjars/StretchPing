package jez.stretchping.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Undo
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
import jez.stretchping.features.activetimer.ActiveTimerState
import jez.stretchping.features.activetimer.ActiveTimerVM
import jez.stretchping.features.activetimer.ActiveTimerViewState

private const val SecondaryButtonSize = 60
private const val PrimaryButtonSize = 84

@Composable
fun ActiveSegmentControls(
    eventHandler: (ActiveTimerVM.Event) -> Unit,
    modifier: Modifier = Modifier,
    stateProvider: () -> ActiveTimerViewState,
) {
    val state = stateProvider()
    val timerState = state.activeTimer

    Row(
        modifier = modifier
            .padding(horizontal = 48.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Crossfade(targetState = timerState?.isPaused == true) {
            if (it) {
                CircleButton(
                    onClick = { eventHandler(ActiveTimerVM.Event.Reset) },
                    imageVector = Icons.Rounded.Undo,
                    contentDescription = stringResource(id = R.string.timer_resume),
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
            state = timerState,
            enabled = state.editTimerState?.canStart ?: true,
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
    eventHandler: (ActiveTimerVM.Event) -> Unit,
    state: ActiveTimerState?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val isPlay = state == null || state.isPaused
    CircleButton(
        onClick = if (isPlay) {
            { eventHandler(ActiveTimerVM.Event.Start) }
        } else {
            { eventHandler(ActiveTimerVM.Event.Pause) }
        },
        imageVector = when {
            isPlay -> Icons.Rounded.PlayArrow
            else -> Icons.Rounded.Pause
        },
        contentDescription = stringResource(id = R.string.timer_start),
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