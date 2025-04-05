package com.lauruspa.life_management.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun IntRect.toPaddingValues(): PaddingValues {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    return remember(this, density, layoutDirection) {
        with(density) {
            val leftDp = left.toDp()
            val topDp = top.toDp()
            val rightDp = right.toDp()
            val bottomDp = bottom.toDp()

            PaddingValues(
                start = if (layoutDirection == LayoutDirection.Ltr) {
                    leftDp
                } else {
                    rightDp
                },
                top = topDp,
                end = if (layoutDirection == LayoutDirection.Ltr) {
                    rightDp
                } else {
                    leftDp
                },
                bottom = bottomDp
            )
        }

    }
}