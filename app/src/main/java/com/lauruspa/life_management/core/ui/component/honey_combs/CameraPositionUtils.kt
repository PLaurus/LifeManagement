package com.lauruspa.life_management.core.ui.component.honey_combs

import com.lauruspa.life_management.core.ui.utils.coerceIn

fun CameraPosition.coerceIn(
	zoomRange: ClosedRange<Float> = zoom..zoom,
	rotationRange: ClosedRange<Float> = rotation..rotation,
	panXRange: ClosedRange<Float> = pan.x..pan.x,
	panYRange: ClosedRange<Float> = pan.y..pan.y,
): CameraPosition {
	return CameraPosition(
		zoom = zoom.coerceIn(zoomRange),
		rotation = rotation.coerceIn(rotationRange),
		pan = pan.coerceIn(panXRange, panYRange)
	)
}