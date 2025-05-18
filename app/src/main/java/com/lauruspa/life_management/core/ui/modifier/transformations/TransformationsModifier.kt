package com.lauruspa.life_management.core.ui.modifier.transformations

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch

/**
 * A modifier function that allows content to be zoomable.
 *
 * @param zoomState A [TransformationsState] object.
 * @param zoomEnabled specifies if zoom behaviour is enabled or disabled. Even if this is false,
 * [onTap] and [onDoubleTap] will be called.
 * @param enableOneFingerZoom If true, enable one finger zoom gesture, double tap followed by
 * vertical scrolling.
 * @param scrollGesturePropagation specifies when scroll gestures are propagated to the parent
 * composable element.
 * @param onTap will be called when single tap is detected on the element.
 * @param onDoubleTap will be called when double tap is detected on the element. This is a suspend
 * function and called in a coroutine scope. The default is to toggle the scale between 1.0f and
 * 2.5f with animation.
 * @param onLongPress will be called when time elapses without the pointer moving
 */
fun Modifier.zoomable(
	zoomState: TransformationsState,
	zoomEnabled: Boolean = true,
	enableOneFingerZoom: Boolean = true,
	scrollGesturePropagation: ScrollGesturePropagation = ScrollGesturePropagation.ContentEdge,
	onTap: ((position: Offset) -> Unit)? = null,
	onDoubleTap: (suspend (position: Offset) -> Unit)? = { position ->
		if (zoomEnabled) zoomState.toggleScale(2.5f, position)
	},
	onLongPress: ((position: Offset) -> Unit)? = null
): Modifier = this then ZoomableElement(
	zoomState = zoomState,
	zoomEnabled = zoomEnabled,
	enableOneFingerZoom = enableOneFingerZoom,
	snapBackEnabled = false,
	scrollGesturePropagation = scrollGesturePropagation,
	onTap = onTap,
	onDoubleTap = onDoubleTap,
	onLongPress = onLongPress,
	enableNestedScroll = false,
)

private data class ZoomableElement(
	val zoomState: TransformationsState,
	val zoomEnabled: Boolean,
	val enableOneFingerZoom: Boolean,
	val snapBackEnabled: Boolean,
	val scrollGesturePropagation: ScrollGesturePropagation,
	val onTap: ((position: Offset) -> Unit)?,
	val onDoubleTap: (suspend (position: Offset) -> Unit)?,
	val onLongPress: ((position: Offset) -> Unit)?,
	val enableNestedScroll: Boolean,
) : ModifierNodeElement<ZoomableNode>() {
	override fun create(): ZoomableNode = ZoomableNode(
		zoomState,
		zoomEnabled,
		enableOneFingerZoom,
		snapBackEnabled,
		scrollGesturePropagation,
		onTap,
		onDoubleTap,
		onLongPress,
		enableNestedScroll,
	)
	
	override fun update(node: ZoomableNode) {
		node.update(
			zoomState,
			zoomEnabled,
			enableOneFingerZoom,
			snapBackEnabled,
			scrollGesturePropagation,
			onTap,
			onDoubleTap,
			onLongPress,
		)
	}
	
	override fun InspectorInfo.inspectableProperties() {
		name = "zoomable"
		properties["zoomState"] = zoomState
		properties["zoomEnabled"] = zoomEnabled
		properties["enableOneFingerZoom"] = enableOneFingerZoom
		properties["snapBackEnabled"] = snapBackEnabled
		properties["scrollGesturePropagation"] = scrollGesturePropagation
		properties["onTap"] = onTap
		properties["onDoubleTap"] = onDoubleTap
		properties["onLongPress"] = onLongPress
		properties["enableNestedScroll"] = enableNestedScroll
	}
}

private class ZoomableNode(
	var zoomState: TransformationsState,
	var zoomEnabled: Boolean,
	var enableOneFingerZoom: Boolean,
	var snapBackEnabled: Boolean,
	var scrollGesturePropagation: ScrollGesturePropagation,
	var onTap: ((position: Offset) -> Unit)?,
	var onDoubleTap: (suspend (position: Offset) -> Unit)?,
	var onLongPress: ((position: Offset) -> Unit)?,
	enableNestedScroll: Boolean,
) : PointerInputModifierNode, LayoutModifierNode, DelegatingNode() {
	var measuredSize = Size.Zero
	
	fun update(
		zoomState: TransformationsState,
		zoomEnabled: Boolean,
		enableOneFingerZoom: Boolean,
		snapBackEnabled: Boolean,
		scrollGesturePropagation: ScrollGesturePropagation,
		onTap: ((position: Offset) -> Unit)?,
		onDoubleTap: (suspend (position: Offset) -> Unit)?,
		onLongPress: ((position: Offset) -> Unit)?,
	) {
		if (this.zoomState != zoomState) {
			zoomState.setLayoutSize(measuredSize)
			this.zoomState = zoomState
		}
		this.zoomEnabled = zoomEnabled
		this.enableOneFingerZoom = enableOneFingerZoom
		this.scrollGesturePropagation = scrollGesturePropagation
		this.snapBackEnabled = snapBackEnabled
		if (((onTap == null) != (this.onTap == null)) ||
			((onDoubleTap == null) != (this.onDoubleTap == null)) ||
			((onLongPress == null) != (this.onLongPress == null))
		) {
			this.pointerInputNode.resetPointerInputHandler()
		}
		this.onTap = onTap
		this.onDoubleTap = onDoubleTap
		this.onLongPress = onLongPress
	}
	
	init {
		if (enableNestedScroll) {
			delegate(
				nestedScrollModifierNode(
					connection = object : NestedScrollConnection {
						override fun onPostScroll(
							consumed: Offset,
							available: Offset,
							source: NestedScrollSource,
						): Offset {
							return zoomState.applyPan(
								pan = available,
								coroutineScope = coroutineScope
							)
						}
					},
					dispatcher = null,
				)
			)
		}
	}
	
	val pointerInputNode = delegate(
		SuspendingPointerInputModifierNode {
			detectZoomableGestures(
				cancelIfZoomCanceled = { snapBackEnabled },
				onGestureStart = {
					resetConsumeGesture()
					zoomState.startGesture()
				},
				canConsumeGesture = { pan, zoom ->
					zoomEnabled && canConsumeGesture(pan, zoom)
				},
				onGesture = { centroid, pan, zoom, timeMillis ->
					if (zoomEnabled) {
						coroutineScope.launch {
							zoomState.applyGesture(
								pan = pan,
								zoom = zoom,
								position = centroid,
								timeMillis = timeMillis,
							)
						}
					}
				},
				onGestureEnd = {
					coroutineScope.launch {
						if (snapBackEnabled || zoomState.scale < 1f) {
							zoomState.changeScale(1f, Offset.Zero)
						} else {
							zoomState.startFling()
						}
					}
				},
				onTap = if (onTap != null) {
					{ onTap?.invoke(it) }
				} else {
					null
				},
				onDoubleTap = if (onDoubleTap != null) {
					{ coroutineScope.launch { onDoubleTap?.invoke(it) } }
				} else {
					null
				},
				onLongPress = if (onLongPress != null) {
					{ onLongPress?.invoke(it) }
				} else {
					null
				},
				enableOneFingerZoom = { enableOneFingerZoom },
			)
		}
	)
	
	private var consumeGesture: Boolean? = null
	
	private fun resetConsumeGesture() {
		consumeGesture = null
	}
	
	private fun canConsumeGesture(pan: Offset, zoom: Float): Boolean {
		val currentValue = consumeGesture
		if (currentValue != null) {
			return currentValue
		}
		
		val newValue = when {
			zoom != 1f -> true
			zoomState.scale == 1f -> false
			scrollGesturePropagation == ScrollGesturePropagation.NotZoomed -> true
			else -> zoomState.willChangeOffset(pan)
		}
		consumeGesture = newValue
		return newValue
	}
	
	override fun onPointerEvent(
		pointerEvent: PointerEvent,
		pass: PointerEventPass,
		bounds: IntSize,
	) {
		pointerInputNode.onPointerEvent(pointerEvent, pass, bounds)
	}
	
	override fun onCancelPointerInput() {
		pointerInputNode.onCancelPointerInput()
	}
	
	override fun MeasureScope.measure(
		measurable: Measurable,
		constraints: Constraints,
	): MeasureResult {
		val placeable = measurable.measure(constraints)
		measuredSize = IntSize(placeable.measuredWidth, placeable.measuredHeight).toSize()
		zoomState.setLayoutSize(measuredSize)
		return layout(placeable.width, placeable.height) {
			placeable.placeWithLayer(x = 0, y = 0) {
				scaleX = zoomState.scale
				scaleY = zoomState.scale
				translationX = zoomState.offsetX
				translationY = zoomState.offsetY
			}
		}
	}
}

/**
 * Toggle the scale between [targetScale] and 1.0f.
 *
 * @param targetScale Scale to be set if this function is called when the scale is 1.0f.
 * @param position Zoom around this point.
 * @param animationSpec The animation configuration.
 */
suspend fun TransformationsState.toggleScale(
	targetScale: Float,
	position: Offset,
	animationSpec: AnimationSpec<Float> = spring(),
) {
	val newScale = if (scale == 1f) targetScale else 1f
	changeScale(newScale, position, animationSpec)
}

@Composable
@Preview
private fun TransformationsPreview() {
	Box(
		modifier = Modifier.zoomable(
			zoomState = rememberZoomState()
		)
	) {
		Column {
			repeat(20) {
				Row(Modifier.border(1.dp, Color.Black)) {
					repeat(10) { index ->
						Spacer(
							modifier = Modifier
								.background(
									color = when (index % 3) {
										0 -> Color.Blue
										1 -> Color.Green
										2 -> Color.Red
										else -> Color.Yellow
									}
								)
								.size(50.dp, 50.dp)
						)
					}
				}
			}
		}
	}
}