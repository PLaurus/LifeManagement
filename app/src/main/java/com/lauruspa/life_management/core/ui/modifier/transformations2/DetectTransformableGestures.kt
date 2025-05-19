package com.lauruspa.life_management.core.ui.modifier.transformations2

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputScope

internal suspend fun PointerInputScope.detectTransformableGestures(
	onGestureStart: () -> Unit,
	onTransform: (centroid: Offset, pan: Offset, zoom: Float, timeMillis: Long) -> Unit,
	onGestureEnd: () -> Unit
) = awaitEachGesture {
	onGestureStart()


}

private suspend fun AwaitPointerEventScope.detectGesture(
	onTransform: (centroid: Offset, pan: Offset, zoom: Float, timeMillis: Long) -> Unit
) {

}