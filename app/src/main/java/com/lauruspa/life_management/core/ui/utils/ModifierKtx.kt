package com.lauruspa.life_management.core.ui.utils

import androidx.compose.ui.Modifier

fun Modifier.conditionalThen(
    condition: Boolean,
    ifFalse: (Modifier.() -> Modifier)? = null,
    ifTrue: Modifier.() -> Modifier
): Modifier = when {
    condition -> then(ifTrue(Modifier))
    ifFalse != null -> then(ifFalse(Modifier))
    else -> this
}