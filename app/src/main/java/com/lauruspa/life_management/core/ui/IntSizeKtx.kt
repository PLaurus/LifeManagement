package com.lauruspa.life_management.core.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toOffset

fun IntSize.toIntOffset(): IntOffset {
    return IntOffset(
        x = width,
        y = height
    )
}

fun IntSize.toOffset(): Offset {
    return toIntOffset().toOffset()
}