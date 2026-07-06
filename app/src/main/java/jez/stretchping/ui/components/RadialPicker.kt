package jez.stretchping.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.redfoxstudio.stretchping.R
import jez.stretchping.ui.theme.StretchPingTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadialPicker(
    values: List<String>,
    initialSelectedIndex: Int,
    onDismissRequest: () -> Unit,
    title: String? = null,
    onSelectionConfirmed: (Int) -> Unit,
) {
    var currentIndex by remember { mutableIntStateOf(initialSelectedIndex) }
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    )
                }
                Dial(
                    values = values,
                    currentIndex = { currentIndex },
                    onSelectedIndexChange = { index -> currentIndex = index },
                    onSelectionMade = { onSelectionConfirmed(currentIndex) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(id = R.string.radial_picker_cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun Dial(
    values: List<String>,
    currentIndex: () -> Int,
    onSelectedIndexChange: (Int) -> Unit,
    onSelectionMade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val numberOfValues = values.size
    val radiansPerValue = (2 * PI / numberOfValues).toFloat()
    val baseAngle = -(PI * 0.5).toFloat()
    val segmentOffset = -radiansPerValue * 0.5f
    val twoPi = (2 * PI).toFloat()

    // Track the hand angle continuously so animating between adjacent values
    // never spins the long way around the dial.
    val targetAngle = baseAngle + radiansPerValue * currentIndex()
    var unwrappedAngle by remember { mutableFloatStateOf(targetAngle) }
    val angleDelta = (targetAngle - unwrappedAngle).mod(twoPi)
    unwrappedAngle += if (angleDelta > PI) angleDelta - twoPi else angleDelta
    val handAngle by animateFloatAsState(
        targetValue = unwrappedAngle,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "dialHandAngle",
    )

    var dragPosition by remember { mutableStateOf(Offset.Zero) }

    val textMeasurer = rememberTextMeasurer(cacheSize = numberOfValues)
    val textStyle = MaterialTheme.typography.bodyLarge
    val dialColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val valueColor = MaterialTheme.colorScheme.onSurface
    val selectedValueColor = MaterialTheme.colorScheme.onPrimary
    val selectorColor = MaterialTheme.colorScheme.primary

    fun selectSegmentAt(position: Offset, center: Offset): Int =
        (((position - center).toRadians() - baseAngle - segmentOffset).mod(2 * PI) / radiansPerValue).toInt()

    Canvas(
        modifier = modifier
            .size(280.dp)
            .clip(CircleShape)
            .background(dialColor)
            .pointerInput(Unit) {
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                detectDragGestures(
                    onDragStart = {
                        dragPosition = it
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragPosition += dragAmount
                        val dragSegment = selectSegmentAt(dragPosition, center)
                        if (dragSegment != currentIndex()) {
                            onSelectedIndexChange(dragSegment)
                        }
                    },
                    onDragEnd = {
                        onSelectionMade()
                    },
                )
            }
            .pointerInput(Unit) {
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                detectTapGestures {
                    onSelectedIndexChange(selectSegmentAt(it, center))
                    onSelectionMade()
                }
            }
    ) {
        val valueRadius = size.width * 0.5f - 28.dp.toPx()
        val selectorRadius = 24.dp.toPx()
        val selectorCenter = center + handAngle.toOffset() * valueRadius

        drawCircle(
            color = selectorColor,
            radius = 4.dp.toPx(),
            center = center,
        )
        drawLine(
            color = selectorColor,
            start = center,
            end = selectorCenter,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = selectorColor,
            radius = selectorRadius,
            center = selectorCenter,
        )

        values.forEachIndexed { index, text ->
            val angle = baseAngle + radiansPerValue * index
            val result = textMeasurer.measure(text, textStyle)
            val textCenter = center + angle.toOffset() * valueRadius
            drawText(
                textLayoutResult = result,
                color = if (index == currentIndex()) selectedValueColor else valueColor,
                topLeft = textCenter + Offset(
                    -result.size.width * 0.5f,
                    -result.size.height * 0.5f
                ),
            )
        }
    }
}

private fun Float.toOffset() =
    Offset(
        x = cos(this),
        y = sin(this),
    )

private fun Offset.toRadians() =
    atan2(y = y, x = x)

@Preview
@Composable
private fun RadialNumberPickerPreview() {
    StretchPingTheme(isDarkTheme = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            RadialPicker(
                values = listOf("5", "10", "15", "20", "30", "45", "60", "90", "120"),
                initialSelectedIndex = 4,
                onDismissRequest = {},
                title = "Stretch",
                onSelectionConfirmed = {},
            )
        }
    }
}
