package jez.stretchping.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private const val ArcStartDeg = 120f
private const val ArcSweepDeg = 300f
private val ArcBackThickness = 4.dp
private val ArcForeThickness = 12.dp

@Composable
fun ArcProgressBar(
    progress: Float,
) {
    val arcForeground = MaterialTheme.colors.secondary
    val arcBackground = Color.LightGray

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.8f)
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
            sweepAngle = ArcSweepDeg * progress,
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