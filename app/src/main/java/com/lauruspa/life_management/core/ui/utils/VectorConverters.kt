package com.lauruspa.life_management.core.ui.utils

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize

val DpSize.Companion.VectorConverter: TwoWayConverter<DpSize, AnimationVector2D>
    get() = DpSizeToVector

/**
 * A type converter that converts a [DpSize] to a [AnimationVector2D], and vice versa.
 */
private val DpSizeToVector: TwoWayConverter<DpSize, AnimationVector2D> =
    TwoWayConverter(
        convertToVector = { AnimationVector2D(it.width.value, it.height.value) },
        convertFromVector = { DpSize(Dp(it.v1), Dp(it.v2)) }
    )