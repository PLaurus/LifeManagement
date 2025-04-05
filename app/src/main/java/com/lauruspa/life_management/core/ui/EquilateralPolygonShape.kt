package com.lauruspa.life_management.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.lauruspa.life_management.core.ui.utils.rotateByRadians
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan


data class EquilateralPolygonShape(
    val anglesCount: Int,
    val cornerSize: CornerSize
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {

        return Outline.Generic(Path().apply {
            reset()

            val cornerRadius = cornerSize.toPx(size, density)
            val pi = PI.toFloat()
            val fVertexAngle = 2 * pi / anglesCount
            val cornerAngle = pi * (anglesCount - 2) / anglesCount
            val center = Offset(x = size.width / 2, y = size.height / 2)
            val alpha = cornerAngle / 2
            val fi = pi / 2 - alpha
            val fVertex = Offset(x = center.x, y = 0f)
            val distFromFVertexToArcStart = tan(fi) * cornerRadius
            val fArcStart = Offset(
                x = fVertex.x - sin(alpha) * distFromFVertexToArcStart,
                y = fVertex.y + cos(alpha) * distFromFVertexToArcStart
            )
            val fArcRectCenter = Offset(
                x = fVertex.x,
                y = fVertex.y + cornerRadius / sin(alpha)
            )
            val fArcStartAngle = pi + alpha
            val fArcSweepAngle = fi * 2


            repeat(anglesCount) { i ->
                val vertexAngle = i * fVertexAngle

                val arcStart = fArcStart.rotateByRadians(
                    angle = vertexAngle,
                    pivot = center
                )

                val arcRectCenter = fArcRectCenter.rotateByRadians(
                    angle = vertexAngle,
                    pivot = center
                )
                val arcStartAngle = fArcStartAngle + vertexAngle

                if (i == 0) {
                    moveTo(
                        x = arcStart.x,
                        y = arcStart.y
                    )
                } else {
                    lineTo(
                        x = arcStart.x,
                        y = arcStart.y
                    )
                }

                arcTo(
                    rect = Rect(
                        center = arcRectCenter,
                        radius = cornerRadius
                    ),
                    startAngleDegrees = (arcStartAngle * 180 / PI).toFloat(),
                    sweepAngleDegrees = (fArcSweepAngle * 180 / PI).toFloat(),
                    forceMoveTo = false
                )

            }
            close()
        })
    }
}

@Preview
@Composable
private fun TriangleShapePreview() {
    Surface(
        modifier = Modifier.size(140.dp),
        shape = EquilateralPolygonShape(
            anglesCount = 3,
            cornerSize = CornerSize(16.dp)
        ),
        color = Color(0xFFB84343),
        content = {}
    )
}

@Preview
@Composable
private fun TetragonShapePreview() {
    Surface(
        modifier = Modifier.size(140.dp),
        shape = EquilateralPolygonShape(
            anglesCount = 4,
            cornerSize = CornerSize(16.dp)
        ),
        color = Color(0xFFB84343),
        content = {}
    )
}

@Preview
@Composable
private fun HexagonShapePreview() {
    Surface(
        modifier = Modifier.size(140.dp),
        shape = EquilateralPolygonShape(
            anglesCount = 6,
            cornerSize = CornerSize(16.dp)
        ),
        color = Color(0xFFB84343),
        content = {}
    )
}