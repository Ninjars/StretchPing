package jez.stretchping.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Adapted from https://betterprogramming.pub/tristate-toggle-in-jetpack-compose-5b080e537c64
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TriStateToggle(
    states: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelectionChanged: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clip(shape = RoundedCornerShape(24.dp)),
        ) {
            states.forEachIndexed { index, text ->
                AnimatedContent(
                    targetState = index == selectedIndex,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(250, delayMillis = 45)) with
                                fadeOut(animationSpec = tween(90))
                    }
                ) { isSelected ->
                    val textColor = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val backgroundColor = if (isSelected) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        Color.Transparent
                    }

                    Text(
                        text = text,
                        color = textColor,
                        modifier = Modifier
                            .clip(shape = RoundedCornerShape(24.dp))
                            .clickable { onSelectionChanged(index) }
                            .background(backgroundColor)
                            .padding(
                                vertical = 12.dp,
                                horizontal = 16.dp,
                            ),
                    )
                }
            }
        }
    }
}
