package com.lauruspa.life_management.core.ui.utils

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun Canvas.drawArrowhead(
    endCoords: Pair<Float, Float>,
    angle: Float,
    paint: Paint,
    arrowheadRadiusPx: Float = 20f,
    arrowheadAngle: Float = 90f
) {
    // val angle: Float = atan2(arrowStopY - arrowStartY, arrowStopX - arrowStartX)
    val arrowheadAngleRad = arrowheadAngle.toRadians()
    drawLine(
        endCoords.first,
        endCoords.second,
        (endCoords.first - arrowheadRadiusPx * cos(angle - arrowheadAngleRad / 2.0)).toFloat(),
        (endCoords.second - arrowheadRadiusPx * sin(angle - arrowheadAngleRad / 2.0)).toFloat(),
        paint
    )
    drawLine(
        endCoords.first,
        endCoords.second,
        (endCoords.first - arrowheadRadiusPx * cos(angle + arrowheadAngleRad / 2.0)).toFloat(),
        (endCoords.second - arrowheadRadiusPx * sin(angle + arrowheadAngleRad / 2.0)).toFloat(),
        paint
    )
}

fun Float.toRadians(): Float {
    return this * PI.toFloat() / 180
}

/**
 * @param angle Degrees
 */
fun Pair<Float, Float>.rotateByDegrees(
    angle: Float,
    pivot: Pair<Float, Float> = 0f to 0f,
): Pair<Float, Float> {
    return rotateByRadians(
        pivot = pivot,
        angle = angle.toRadians()
    )
}

fun Pair<Float, Float>.rotateByRadians(
    angle: Float,
    pivot: Pair<Float, Float> = 0f to 0f,
): Pair<Float, Float> {
    val x = first
    val y = second
    val pivotX = pivot.first
    val pivotY = pivot.second
    val resX = (((x - pivotX) * cos(angle) - (y - pivotY) * sin(angle)) + pivotX)
    val resY = (((x - pivotX) * sin(angle) + (y - pivotY) * cos(angle)) + pivotY)
    return resX to resY
}

fun Offset.rotateByDegrees(
    angle: Float,
    pivot: Offset = Offset.Zero,
): Offset {
    return (x to y).rotateByDegrees(
        pivot = pivot.x to pivot.y,
        angle = angle
    ).let { (x, y) ->
        Offset(x = x, y = y)
    }
}

fun Offset.rotateByRadians(
    angle: Float,
    pivot: Offset = Offset.Zero,
): Offset {
    return (x to y).rotateByRadians(
        pivot = pivot.x to pivot.y,
        angle = angle
    ).let { (x, y) ->
        Offset(x = x, y = y)
    }
}
