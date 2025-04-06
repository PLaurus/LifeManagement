package com.lauruspa.life_management.core.ui.utils

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

private const val DpVisibilityThreshold = 0.1f

val DpSize.Companion.VisibilityThreshold: DpSize
    get() = DpSize(DpVisibilityThreshold.dp, DpVisibilityThreshold.dp)