package com.lauruspa.life_management.core.ui.utils

import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/**
 * Returns `true` if the current state of the pointer events has all pointers up and `false`
 * if any of the pointers are down.
 */
internal fun AwaitPointerEventScope.allPointersUp(): Boolean =
    !currentEvent.changes.any { it.pressed }

/**
 * Waits for all pointers to be up before returning.
 */
internal suspend fun PointerInputScope.awaitAllPointersUp() {
    awaitPointerEventScope { awaitAllPointersUp() }
}

/**
 * Waits for all pointers to be up before returning.
 */
internal suspend fun AwaitPointerEventScope.awaitAllPointersUp() {
    if (!allPointersUp()) {
        do {
            val events = awaitPointerEvent(PointerEventPass.Final)
        } while (events.changes.any { it.pressed })
    }
}

/**
 * Repeatedly calls [block] to handle gestures. If there is a [CancellationException],
 * it will wait until all pointers are raised before another gesture is detected, or it
 * exits if [isActive] is `false`.
 *
 * [block] is run within [PointerInputScope.awaitPointerEventScope] and will loop entirely
 * within the [AwaitPointerEventScope] so events will not be lost between gestures.
 */
suspend fun PointerInputScope.awaitEachGesture(block: suspend AwaitPointerEventScope.() -> Unit) {
    val currentContext = currentCoroutineContext()
    awaitPointerEventScope {
        while (currentContext.isActive) {
            try {
                block()

                // Wait for all pointers to be up. Gestures start when a finger goes down.
                awaitAllPointersUp()
            } catch (e: CancellationException) {
                if (currentContext.isActive) {
                    // The current gesture was canceled. Wait for all fingers to be "up" before
                    // looping again.
                    awaitAllPointersUp()
                } else {
                    // detectGesture was cancelled externally. Rethrow the cancellation exception to
                    // propagate it upwards.
                    throw e
                }
            }
        }
    }
}
