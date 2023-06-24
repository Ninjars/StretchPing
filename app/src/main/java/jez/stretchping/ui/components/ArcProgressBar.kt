package jez.stretchping.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jez.stretchping.ui.theme.StretchPingTheme
import java.lang.Float.min

private const val ArcStartDeg = 120f
private const val ArcSweepDeg = 300f
private val ArcBackThickness = 4.dp
private val ArcForeThickness = 12.dp

@Composable
fun ArcProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val arcForeground = MaterialTheme.colorScheme.tertiary
    val arcBackground = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
    ) {
        val originOffset = Offset(ArcForeThickness.toPx(), ArcForeThickness.toPx())
        val arcBoundsSize = Size(
            size.width - (ArcForeThickness * 2).toPx(),
            size.height - (ArcForeThickness * 2).toPx()
        )

        // Background Arc
        drawArc(
            color = arcBackground,
            startAngle = ArcStartDeg,
            sweepAngle = ArcSweepDeg,
            useCenter = false,
            topLeft = originOffset,
            size = arcBoundsSize,
            style = Stroke(
                width = ArcBackThickness.toPx(),
                cap = StrokeCap.Round,
            )
        )

        // Foreground Arc
        drawArc(
            color = arcForeground,
            startAngle = ArcStartDeg,
            sweepAngle = ArcSweepDeg * min(progress, 1f),
            useCenter = false,
            topLeft = originOffset,
            size = arcBoundsSize,
            style = Stroke(
                width = ArcForeThickness.toPx(),
                cap = StrokeCap.Round,
            )
        )
    }
}

@Preview
@Composable
private fun ArcProgressBarPreview() {
    StretchPingTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ArcProgressBar(0.33f)
                ArcProgressBar(0.66f)
                ArcProgressBar(1f)
            }
        }
    }
}
