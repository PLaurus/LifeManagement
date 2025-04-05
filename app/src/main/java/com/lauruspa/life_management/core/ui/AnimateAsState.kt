package com.lauruspa.life_management.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize

private val dpSizeDefaultSpring = spring(visibilityThreshold = DpSize.VisibilityThreshold)


/**
 * Fire-and-forget animation function for [DpSize]. This Composable function is overloaded for
 * different parameter types such as [Dp], [Color][androidx.compose.ui.graphics.Color], [Offset],
 * etc. When the provided [targetValue] is changed, the animation will run automatically. If there
 * is already an animation in-flight when [targetValue] changes, the on-going animation will adjust
 * course to animate towards the new target value.
 *
 * [animateDpSizeAsState] returns a [State] object. The value of the state object will continuously be
 * updated by the animation until the animation finishes.
 *
 * Note, [animateDpSizeAsState] cannot be canceled/stopped without removing this composable function
 * from the tree. See [Animatable] for cancelable animations.
 *
 *     val size: DpSize by animateDpSizeAsState(
 *         if (selected) DpSize(20.dp, 20.dp) else Size(10.dp, 10.dp))
 *
 * @param targetValue Target value of the animation
 * @param animationSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param finishedListener An optional end listener to get notified when the animation is finished.
 * @return A [State] object, the value of which is updated by animation.
 */
@Composable
fun animateDpSizeAsState(
    targetValue: DpSize,
    animationSpec: AnimationSpec<DpSize> = dpSizeDefaultSpring,
    finishedListener: ((DpSize) -> Unit)? = null
): State<DpSize> {
    return animateValueAsState(
        targetValue,
        DpSize.VectorConverter,
        animationSpec,
        finishedListener = finishedListener
    )
}