package com.lauruspa.life_management.core.ui.modifier.transformations

import androidx.compose.ui.Modifier

/**
 * [ScrollGesturePropagation] defines when [Modifier.zoomable] propagates scroll gestures to the
 * parent composable element.
 */
enum class ScrollGesturePropagation {
	
	/**
	 * Propagates the scroll gesture to the parent composable element when the content is scrolled
	 * to the edge and attempts to scroll further.
	 */
	ContentEdge,
	
	/**
	 * Propagates the scroll gesture to the parent composable element when the content is not zoomed.
	 */
	NotZoomed,
}