package com.lauruspa.life_management.core.ui.utils

import android.content.res.TypedArray
import androidx.annotation.StyleableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified

/**
 * @return The color value for the attribute at [index] or [Color.Unspecified] if not specified.
 *
 * @see TypedArray.hasValue
 * @see TypedArray.getColor
 */
fun TypedArray.getComposeColor(@StyleableRes index: Int): Color {
    if (!hasValue(index)) return Color.Unspecified
    return getColor(index, 0).let(::Color)
}

/**
 * @return The color value for the attribute at [index] or null if not specified.
 *
 * @see TypedArray.hasValue
 * @see TypedArray.getColor
 */
fun TypedArray.getComposeColorOrNull(@StyleableRes index: Int): Color? {
    return getComposeColor(index).takeIf { it.isSpecified }
}