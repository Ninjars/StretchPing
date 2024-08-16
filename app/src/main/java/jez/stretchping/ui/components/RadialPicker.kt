package jez.stretchping.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import dev.redfoxstudio.stretchping.R
import jez.stretchping.ui.theme.StretchPingTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun RadialPicker(
    values: List<String>,
    initialSelectedIndex: Int,
    onDismissRequest: () -> Unit,
    onSelectionConfirmed: (Int) -> Unit,
) {
    var currentIndex by remember { mutableIntStateOf(initialSelectedIndex) }
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 48.dp)
        ) {
            RadialPicker(
                values,
                { currentIndex },
            ) { index ->
                currentIndex = index
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
            ) {
                Button(
                    onClick = onDismissRequest,
                    shape = CircleShape,
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.radial_picker_cancel)
                    )
                }
                Button(
                    onClick = { onSelectionConfirmed(currentIndex) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(id = R.string.radial_picker_ok))
                }
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun RadialPicker(
    values: List<String>,
    initialSelectedIndex: () -> Int,
    onSelectedIndexChange: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        RadialLines(
            values = values,
            currentIndex = initialSelectedIndex,
            onSelectedIndexChange = onSelectedIndexChange,
        )
    }
}

@Composable
private fun RadialLines(
    values: List<String>,
    currentIndex: () -> Int,
    onSelectedIndexChange: (Int) -> Unit,
) {
    val numberOfValues = values.size
    val radiansPerValue = (2 * PI / numberOfValues).toFloat()
    val baseAngle = -(PI * 0.5).toFloat()
    val segmentOffset = -radiansPerValue * 0.5f

    val selectedIndexPosition = (baseAngle + radiansPerValue * currentIndex()).toOffset()
    var cachedCenter by remember { mutableStateOf(Offset.Zero) }
    var dragPosition by remember { mutableStateOf(selectedIndexPosition) }

    val textMeasurer = rememberTextMeasurer(cacheSize = numberOfValues)
    val textColor = LocalContentColor.current
    val lineColor = MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.5f)
    val valueLineColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragPosition = it - cachedCenter
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragPosition += dragAmount
                        val dragSegment =
                            ((dragPosition.toRadians() - baseAngle - segmentOffset).mod(2 * PI) / radiansPerValue).toInt()
                        if (dragSegment != currentIndex()) {
                            onSelectedIndexChange(dragSegment)
                        }
                    },
                    onDragEnd = {
                        dragPosition = (baseAngle + radiansPerValue * currentIndex()).toOffset()
                    },
                    onDragCancel = {
                        dragPosition = (baseAngle + radiansPerValue * currentIndex()).toOffset()
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    val segment =
                        (((it - cachedCenter).toRadians() - baseAngle - segmentOffset).mod(2 * PI) / radiansPerValue).toInt()
                    onSelectedIndexChange(segment)
                    dragPosition = (baseAngle + radiansPerValue * currentIndex()).toOffset()
                }
            }
    ) {
        cachedCenter = center
        val length = size.width * 0.4f
        values.forEachIndexed { index, text ->
            val angle = baseAngle + radiansPerValue * index

            val lineVector = (angle + segmentOffset).toOffset()
            drawLine(
                color = lineColor,
                start = center + lineVector * length * 0.1f,
                end = center + lineVector * length,
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )

            val result = textMeasurer.measure(text)
            val textSize = result.size
            val textVector = angle.toOffset()
            val textCenter = center + textVector * size.width * 0.4f
            val isSelected = index == currentIndex()
            drawCircle(
                color = lineColor,
                radius = textSize.toSize().maxDimension,
                center = textCenter,
                style = if (isSelected) {
                    Fill
                } else {
                    Stroke(
                        width = 2.dp.toPx()
                    )
                }
            )

            drawText(
                textLayoutResult = result,
                color = textColor,
                topLeft = textCenter + Offset(-textSize.width * 0.5f, -textSize.height * 0.5f)
            )
        }

        drawLine(
            color = valueLineColor,
            start = center,
            end = center + dragPosition.normalised() * length * 0.8f,
            strokeWidth = 12f,
            cap = StrokeCap.Round,
        )
    }
}

private fun Float.toOffset() =
    Offset(
        x = cos(this),
        y = sin(this),
    )

private fun Offset.toRadians() =
    atan2(y = y, x = x)

private fun Offset.normalised() =
    with(sqrt(x * x + y * y)) {
        Offset(
            x / this, y / this
        )
    }

@Preview
@Composable
private fun RadialNumberPickerPreview() {
    StretchPingTheme(isDarkTheme = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            RadialPicker(
                values = listOf("a", "b", "c", "d", "e"),
                initialSelectedIndex = 4,
                onDismissRequest = {},
                onSelectionConfirmed = {},
            )
        }
    }
}
