package com.lauruspa.life_management.core.ui.component.graph

import androidx.compose.ui.geometry.Offset

object GraphDefaults {
	const val MIN_ZOOM = 0.5f
	const val MAX_ZOOM = 3f
	const val INITIAL_ZOOM = 1f
	const val INITIAL_ROTATION = 0f
	const val INITIAL_PAN_X = 0f
	const val INITIAL_PAN_Y = 0f
	val INITIAL_PAN = Offset(INITIAL_PAN_X, INITIAL_PAN_Y)
	const val FLING = true
	const val MOVE_TO_BOUNDS = true
	const val ZOOMABLE = true
	const val PANNABLE = true
	const val ROTATABLE = false
	const val LIMIT_PAN = true
	val INITIAL_LAYOUT_INFO = GraphLayoutInfo.Zero
}