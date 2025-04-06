package com.lauruspa.life_management.core.ui.component.graph

import android.os.Parcelable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import com.lauruspa.life_management.R
import com.lauruspa.life_management.core.ui.utils.coerceIn
import com.lauruspa.life_management.core.ui.utils.detectTransformGestures
import com.lauruspa.life_management.core.ui.utils.parceler.IntRectParceler
import com.lauruspa.life_management.core.ui.utils.parceler.IntSizeParceler
import com.lauruspa.life_management.core.ui.utils.toOffset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Immutable
@Parcelize
@TypeParceler<IntSize, IntSizeParceler>()
@TypeParceler<IntRect, IntRectParceler>()
data class GraphLayoutInfo(
	val itemRectList: List<IntRect>,
	val containerSize: IntSize,
	val movableArea: IntRect,
	val contentPaddingLeft: Int,
	val contentPaddingTop: Int,
	val contentPaddingRight: Int,
	val contentPaddingBottom: Int
): Parcelable {
	companion object {
		@Stable
		internal val Zero: GraphLayoutInfo
			get() = GraphLayoutInfo(
				itemRectList = emptyList(),
				containerSize = IntSize.Zero,
				movableArea = IntRect.Zero,
				contentPaddingLeft = 0,
				contentPaddingTop = 0,
				contentPaddingRight = 0,
				contentPaddingBottom = 0
			)
	}
}

@Stable
class GraphState internal constructor(
	initialZoom: Float = GraphDefaults.INITIAL_ZOOM,
	initialRotation: Float = GraphDefaults.INITIAL_ROTATION,
	internal val initialPan: Offset = GraphDefaults.INITIAL_PAN,
	internal val minZoom: Float = GraphDefaults.MIN_ZOOM,
	maxZoom: Float = GraphDefaults.MAX_ZOOM,
	val fling: Boolean = GraphDefaults.FLING,
	val moveToBounds: Boolean = GraphDefaults.MOVE_TO_BOUNDS,
	internal val zoomable: Boolean = GraphDefaults.ZOOMABLE,
	internal val pannable: Boolean = GraphDefaults.PANNABLE,
	internal val rotatable: Boolean = GraphDefaults.ROTATABLE,
	internal val limitPan: Boolean = GraphDefaults.LIMIT_PAN,
	initialLayoutInfo: GraphLayoutInfo = GraphDefaults.INITIAL_LAYOUT_INFO
) {
//	var maxXValue: Int
//		get() = _maxXValueState.intValue
//		internal set(newMax) {
//			_maxXValueState.intValue = newMax
//			Snapshot.withoutReadObservation {
//				if (xValue > newMax) {
//					xValue = newMax
//				}
//			}
//		}
	
	init {
		require(minZoom > 0) { "minZoom must be > 0" }
	}
	
	@Suppress("MemberVisibilityCanBePrivate")
	internal val maxZoom = maxZoom.coerceAtLeast(minZoom)
	
	@Suppress("MemberVisibilityCanBePrivate")
	internal val initialZoom = initialZoom.coerceIn(this.minZoom, this.maxZoom)
	
	@Suppress("MemberVisibilityCanBePrivate")
	internal val initialRotation = initialRotation % 360
	
	@Suppress("MemberVisibilityCanBePrivate")
	@Volatile
	var layoutInfo: GraphLayoutInfo = initialLayoutInfo
		internal set
	
	@Suppress("MemberVisibilityCanBePrivate")
	internal val animatablePan = Animatable(initialPan, Offset.VectorConverter)
	
	@Suppress("MemberVisibilityCanBePrivate")
	internal val animatableZoom = Animatable(this.initialZoom).apply {
		updateBounds(this@GraphState.minZoom, this@GraphState.maxZoom)
	}
	
	@Suppress("MemberVisibilityCanBePrivate")
	internal val animatableRotation = Animatable(this.initialRotation)
	
	private val velocityTracker = VelocityTracker()
	private val cameraPositionChangeMutex = MutatorMutex()
	
	val pan: Offset
		get() = animatablePan.value
	
	val zoom: Float
		get() = animatableZoom.value
	
	val rotation: Float
		get() = animatableRotation.value
	
	@Suppress("MemberVisibilityCanBePrivate")
	val isZooming: Boolean
		get() = animatableZoom.isRunning
	
	@Suppress("MemberVisibilityCanBePrivate")
	val isPanning: Boolean
		get() = animatablePan.isRunning
	
	@Suppress("MemberVisibilityCanBePrivate")
	val isRotating: Boolean
		get() = animatableRotation.isRunning
	
	@Suppress("unused")
	val isAnimationRunning: Boolean
		get() = isZooming || isPanning || isRotating
	
	private val cameraPositionCalculator: GraphCameraPositionCalculator = GraphCameraPositionCalculatorImpl()
	
	internal fun updatePanBounds(lowerBound: Offset?, upperBound: Offset?) {
		animatablePan.updateBounds(lowerBound, upperBound)
	}
	
	internal fun getPanBounds(layoutInfo: GraphLayoutInfo): Rect {
		val offsetArea = calcOffsetArea(
			zoom = zoom,
			movableArea = layoutInfo.movableArea,
			containerSize = layoutInfo.containerSize
		)
		val left = -offsetArea.right
		val right = (-offsetArea.left).coerceAtLeast(left)
		val top = -offsetArea.bottom
		val bottom = (-offsetArea.top).coerceAtLeast(top)
		
		return Rect(
			left = left,
			top = top,
			right = right,
			bottom = bottom
		)
	}
	
	private fun calcOffsetArea(
		zoom: Float,
		movableArea: IntRect,
		containerSize: IntSize
	): Rect {
		val left = zoom * movableArea.left.coerceAtMost(0)
		val top = (zoom * movableArea.top.coerceAtMost(0))
		
		val right =
			(zoom * movableArea.right.coerceAtLeast(containerSize.width) - containerSize.width)
				.coerceAtLeast(left)
		
		val bottom =
			(zoom * movableArea.bottom.coerceAtLeast(containerSize.height) - containerSize.height)
				.coerceAtLeast(top)
		
		return Rect(
			left = left,
			top = top,
			right = right,
			bottom = bottom,
		)
	}
	
	private fun getPanBounds(): Rect {
		return getPanBounds(layoutInfo)
	}
	
	internal suspend fun onGesture(
		centroid: Offset,
		pan: Offset,
		zoom: Float,
		rotation: Float,
		mainPointer: PointerInputChange,
		changes: List<PointerInputChange>
	) = coroutineScope {
		
		updateCameraPosition(
			centroid = centroid,
			zoomChange = zoom,
			panChange = pan,
			rotationChange = rotation
		)
		
		// Fling Gesture
		if (fling) {
			if (changes.size == 1) {
				addPosition(mainPointer.uptimeMillis, mainPointer.position)
			}
		}
	}
	
	internal suspend fun onGestureStart() = coroutineScope {}
	
	internal suspend fun onGestureEnd(onBoundsCalculated: () -> Unit) {
		if (fling && zoom > 1) {
			fling {
				// We get target value on start instead of updating bounds after
				// gesture has finished
				onBoundsCalculated()
			}
		} else {
			onBoundsCalculated()
		}
		
		if (moveToBounds) {
			resetToValidBounds()
		}
	}
	
	// TODO Add resetting back to bounds for rotated state as well
	/**
	 * Resets to bounds with animation and resets tracking for fling animation
	 */
	private suspend fun resetToValidBounds() {
		val zoom = zoom.coerceAtLeast(1f)
		val bounds = getPanBounds()
		val pan = pan.coerceIn(
			horizontalRange = bounds.left..bounds.right,
			verticalRange = bounds.top..bounds.bottom
		)
		resetWithAnimation(pan = pan, zoom = zoom)
		resetTracking()
	}
	
	/*
        Fling gesture
     */
	private fun addPosition(timeMillis: Long, position: Offset) {
		velocityTracker.addPosition(
			timeMillis = timeMillis,
			position = position
		)
	}
	
	/**
	 * Create a fling gesture when user removes finger from scree to have continuous movement
	 * until [velocityTracker] speed reached to lower bound
	 */
	private suspend fun fling(onFlingStart: () -> Unit) = coroutineScope {
		val velocityTracker = velocityTracker.calculateVelocity()
		val velocity = Offset(velocityTracker.x, velocityTracker.y)
		var flingStarted = false
		
		launch {
			animatablePan.animateDecay(
				velocity,
				exponentialDecay(absVelocityThreshold = 20f),
				block = {
					// This callback returns target value of fling gesture initially
					if (!flingStarted) {
						onFlingStart()
						flingStarted = true
					}
				}
			)
		}
	}
	
	private fun resetTracking() {
		velocityTracker.resetTracking()
	}
	
	suspend fun updateCameraPosition(
		centroid: Offset,
		panChange: Offset,
		zoomChange: Float,
		rotationChange: Float = 0f,
	) = updateCameraPosition {
		calcNewCameraPosition(
			centroid = centroid,
			panChange = panChange,
			zoomChange = zoomChange,
			rotationChange = rotationChange
		)
	}
	
	suspend fun updateCameraPosition(
		newZoomProvider: suspend (GraphLayoutInfo) -> GraphCameraPosition
	) = cameraPositionChangeMutex.mutate {
		val newZoomState = newZoomProvider(layoutInfo)
		
		snapZoomStateTo(newZoomState.zoom)
		snapRotationStateTo(newZoomState.rotation)
		
		if (pannable) {
			updatePanBounds()
			snapPanStateTo(newZoomState.pan)
		}
	}
	
	@Suppress("unused")
	suspend fun animateCameraPosition(
		centroid: Offset,
		panChange: Offset,
		zoomChange: Float,
		rotationChange: Float = 0f,
		animationSpec: AnimationSpec<Float> = spring()
	) = animateCameraPosition(animationSpec) {
		calcNewCameraPosition(
			centroid = centroid,
			panChange = panChange,
			zoomChange = zoomChange,
			rotationChange = rotationChange
		)
	}
	
	suspend fun animateCameraPosition(
		animationSpec: AnimationSpec<Float> = spring(),
		cameraPositionProvider: suspend GraphCameraPositionCalculator.(GraphState) -> GraphCameraPosition
	) = cameraPositionChangeMutex.mutate {
		coroutineScope {
			val newZoomState = cameraPositionCalculator.cameraPositionProvider(this@GraphState)
			val initialPan = pan
			val initialZoom = zoom
			val targetZoom = newZoomState.zoom
			val range = targetZoom - initialZoom
			animate(
				initialValue = zoom,
				targetValue = newZoomState.zoom,
				animationSpec = animationSpec
			) { value, _ ->
				launch {
					// Update scale here to ensure scale and translation values are updated
					// in the same snapshot
					snapZoomStateTo(value)
					if (pannable) {
						updatePanBounds()
						if (newZoomState.pan != Offset.Unspecified) {
							val fraction = if (range == 0f) 1f else (value - initialZoom) / range
							val nextPan = lerp(initialPan, newZoomState.pan, fraction)
							snapPanStateTo(nextPan)
						}
					}
				}
			}
			
			launch { animateRotationStateTo(newZoomState.rotation, animationSpec) }
			Unit
		}
	}
	
	private fun updatePanBounds() {
		val boundPan = limitPan && !rotatable
		
		if (boundPan) {
			val bound = getPanBounds(layoutInfo)
			updatePanBounds(bound.topLeft, bound.bottomRight)
		}
	}
	
	/**
	 * Reset [pan], [zoom] and [rotation] with animation.
	 */
	suspend fun resetWithAnimation(
		pan: Offset = Offset.Zero,
		zoom: Float = 1f,
		rotation: Float = 0f
	) = coroutineScope {
		launch { animatePanStateTo(pan) }
		launch { animateZoomStateTo(zoom) }
		launch { animateRotationStateTo(rotation) }
	}
	
	suspend fun animatePanStateTo(
		pan: Offset,
		block: (Animatable<Offset, AnimationVector2D>.() -> Unit)? = null
	) {
		if (pannable && this.pan != pan) {
			animatablePan.animateTo(pan, block = block)
		}
	}
	
	suspend fun animateZoomStateTo(
		zoom: Float,
		block: (Animatable<Float, AnimationVector1D>.() -> Unit)? = null
	) {
		if (zoomable && this.zoom != zoom) {
			val newZoom = zoom.coerceIn(minZoom, maxZoom)
			animatableZoom.animateTo(newZoom, block = block)
		}
	}
	
	suspend fun animateRotationStateTo(
		rotation: Float,
		animationSpec: AnimationSpec<Float> = spring()
	) {
		if (rotatable && this.rotation != rotation) {
			animatableRotation.animateTo(rotation, animationSpec)
		}
	}
	
	suspend fun snapPanStateTo(pan: Offset) {
		if (pannable) {
			animatablePan.snapTo(pan)
		}
	}
	
	suspend fun snapZoomStateTo(zoom: Float) {
		if (zoomable) {
			animatableZoom.snapTo(zoom.coerceIn(minZoom, maxZoom))
		}
	}
	
	suspend fun snapRotationStateTo(rotation: Float) {
		if (rotatable) {
			animatableRotation.snapTo(rotation)
		}
	}
	
	private fun calcNewCameraPosition(
		centroid: Offset,
		panChange: Offset,
		zoomChange: Float,
		rotationChange: Float,
		oldZoom: Float = this.zoom,
		oldRotation: Float = this.rotation,
		oldPan: Offset = this.pan
	): GraphCameraPosition {
		val newZoom = if (zoomable) {
			oldZoom * zoomChange
		} else {
			oldZoom
		}.coerceIn(minZoom, maxZoom)
		
		val newRotation = if (rotatable) {
			oldRotation + rotationChange
		} else {
			oldRotation
		}
		
		val newPan = if (pannable) {
			-((-oldPan / oldZoom + centroid / oldZoom) -
					(centroid / newZoom + panChange / oldZoom)) * newZoom
		} else {
			oldPan
		}
		
		return GraphCameraPosition(
			zoom = newZoom,
			rotation = newRotation,
			pan = newPan
		)
	}
	
	interface GraphCameraPositionCalculator {
		fun containerCenter(zoom: Float): GraphCameraPosition
		fun itemsCenter(zoom: Float): GraphCameraPosition
		fun itemsCenter(itemIndexes: List<Int>, zoom: Float): GraphCameraPosition
		fun fitItems(withPadding: Boolean = true): GraphCameraPosition
		fun fitItems(itemIndexes: List<Int>, withPadding: Boolean = true): GraphCameraPosition
	}
	
	private inner class GraphCameraPositionCalculatorImpl : GraphCameraPositionCalculator {
		override fun containerCenter(zoom: Float): GraphCameraPosition {
			val containerCenter = layoutInfo.containerSize.center.toOffset()
			return calcNewCameraPosition(
				centroid = containerCenter,
				panChange = Offset.Zero,
				zoomChange = zoom / this@GraphState.zoom,
				rotationChange = 0f
			)
		}
		
		override fun itemsCenter(zoom: Float): GraphCameraPosition {
			return itemsCenterImpl(
				itemRectList = layoutInfo.itemRectList,
				zoom = zoom
			)
		}
		
		override fun itemsCenter(itemIndexes: List<Int>, zoom: Float): GraphCameraPosition {
			return itemsCenterImpl(
				itemRectList = layoutInfo.itemRectList.filterIndexed { index, _ -> itemIndexes.contains(index) },
				zoom = zoom
			)
		}
		
		private fun itemsCenterImpl(
			itemRectList: List<IntRect>,
			zoom: Float
		): GraphCameraPosition {
			val itemsRect = calcItemsRect(itemRectList = itemRectList)
			val itemsCenter = itemsRect.center.toOffset()
			return calcNewCameraPosition(
				centroid = itemsCenter,
				panChange = Offset.Zero,
				zoomChange = zoom / this@GraphState.zoom,
				rotationChange = 0f
			)
		}
		
		override fun fitItems(withPadding: Boolean): GraphCameraPosition {
			return fitItemsImpl(
				itemRectList = layoutInfo.itemRectList,
				withPadding = withPadding
			)
		}
		
		override fun fitItems(itemIndexes: List<Int>, withPadding: Boolean): GraphCameraPosition {
			return fitItemsImpl(
				itemRectList = layoutInfo.itemRectList.filterIndexed { index, _ -> itemIndexes.contains(index) },
				withPadding = withPadding
			)
		}
		
		private fun fitItemsImpl(
			itemRectList: List<IntRect>,
			withPadding: Boolean
		): GraphCameraPosition {
			val containerSize = layoutInfo.containerSize
			val itemsRect = calcItemsRect(itemRectList = itemRectList)
			val itemsSize = itemsRect.size
			val paddingLeft = layoutInfo.contentPaddingLeft
			val paddingTop = layoutInfo.contentPaddingTop
			val paddingRight = layoutInfo.contentPaddingRight
			val paddingBottom = layoutInfo.contentPaddingBottom
			val maxZoom = maxZoom
			val minZoom = minZoom
			
			fun calcAxisZoom(
				itemsAxisSize: Int,
				containerAxisSize: Int,
				paddingStart: Int,
				paddingEnd: Int
			): Float {
				return if (itemsAxisSize > 0) {
					(containerAxisSize - (paddingStart + paddingEnd)).toFloat()
						.coerceAtLeast(0f) / itemsSize.width
				} else {
					0f
				}.coerceIn(minZoom, maxZoom)
			}
			
			val newZoomX = calcAxisZoom(
				itemsAxisSize = itemsSize.width,
				containerAxisSize = containerSize.width,
				paddingStart = paddingLeft,
				paddingEnd = paddingRight
			)
			
			val newZoomY = calcAxisZoom(
				itemsAxisSize = itemsSize.height,
				containerAxisSize = containerSize.height,
				paddingStart = paddingLeft,
				paddingEnd = paddingRight
			)
			
			val newZoom = minOf(newZoomX, newZoomY)
			
			val targetItemsRect = if (withPadding) {
				itemsRect.run {
					copy(
						left = left - (paddingLeft * newZoom).roundToInt(),
						top = top - (paddingTop * newZoom).roundToInt(),
						right = right + (paddingRight * newZoom).roundToInt(),
						bottom = bottom + (paddingBottom * newZoom).roundToInt()
					)
				}
			} else {
				itemsRect
			}
			
			return GraphCameraPosition(
				zoom = newZoom,
				rotation = 0f,
				pan = -(targetItemsRect.center.toOffset() * newZoom - containerSize.toOffset() / 2f)
			)
		}
		
		private fun calcItemsRect(
			itemRectList: List<IntRect>
		): IntRect {
			return if(itemRectList.isNotEmpty()) {
				IntRect(
					left = itemRectList.minOf { itemRect -> itemRect.left },
					top = itemRectList.minOf { itemRect -> itemRect.top },
					right = itemRectList.maxOf { itemRect -> itemRect.right },
					bottom = itemRectList.maxOf { itemRect -> itemRect.bottom }
				)
			} else {
				IntRect.Zero
			}
		}
	}
	
	companion object {
		private const val INITIAL_ZOOM_KEY = "honey_combs_initial_zoom"
		private const val INITIAL_ROTATION_KEY = "honey_combs_initial_rotation"
		private const val INITIAL_PAN_X_KEY = "honey_combs_initial_pan_x"
		private const val INITIAL_PAN_Y_KEY = "honey_combs_initial_pan_y"
		private const val MIN_ZOOM_KEY = "honey_combs_min_zoom"
		private const val MAX_ZOOM_KEY = "honey_combs_max_zoom"
		private const val FLING_KEY = "honey_combs_fling"
		private const val MOVE_TO_BOUNDS_KEY = "honey_combs_move_to_bounds"
		private const val ZOOMABLE_KEY = "honey_combs_zoomable"
		private const val PANNABLE_KEY = "honey_combs_pannable"
		private const val ROTATABLE_KEY = "honey_combs_rotatable"
		private const val LIMIT_PAN_KEY = "honey_combs_limit_pan"
		private const val INITIAL_LAYOUT_INFO_KEY = "honey_combs_initial_layout_info"
		
		val Saver = mapSaver(
			save = {
				mapOf(
					INITIAL_ZOOM_KEY to it.zoom,
					INITIAL_ROTATION_KEY to it.rotation,
					INITIAL_PAN_X_KEY to it.pan.x,
					INITIAL_PAN_Y_KEY to it.pan.y,
					MIN_ZOOM_KEY to it.minZoom,
					MAX_ZOOM_KEY to it.maxZoom,
					FLING_KEY to it.fling,
					MOVE_TO_BOUNDS_KEY to it.moveToBounds,
					ZOOMABLE_KEY to it.zoomable,
					PANNABLE_KEY to it.pannable,
					ROTATABLE_KEY to it.rotatable,
					LIMIT_PAN_KEY to it.limitPan,
					INITIAL_LAYOUT_INFO_KEY to it.layoutInfo
				)
			},
			restore = { savedStateMap ->
				GraphState(
					initialZoom = savedStateMap[INITIAL_ZOOM_KEY] as Float,
					initialRotation = savedStateMap[INITIAL_ROTATION_KEY] as Float,
					initialPan = Offset(
						savedStateMap[INITIAL_PAN_X_KEY] as Float,
						savedStateMap[INITIAL_PAN_Y_KEY] as Float
					),
					minZoom = savedStateMap[MIN_ZOOM_KEY] as Float,
					maxZoom = savedStateMap[MAX_ZOOM_KEY] as Float,
					fling = savedStateMap[FLING_KEY] as Boolean,
					moveToBounds = savedStateMap[MOVE_TO_BOUNDS_KEY] as Boolean,
					zoomable = savedStateMap[ZOOMABLE_KEY] as Boolean,
					pannable = savedStateMap[PANNABLE_KEY] as Boolean,
					rotatable = savedStateMap[ROTATABLE_KEY] as Boolean,
					limitPan = savedStateMap[LIMIT_PAN_KEY] as Boolean,
					initialLayoutInfo = savedStateMap[INITIAL_LAYOUT_INFO_KEY] as GraphLayoutInfo
				)
			}
		)
	}
}

@Composable
fun rememberGraphState(
	initialZoom: Float = GraphDefaults.INITIAL_ZOOM,
	initialRotation: Float = GraphDefaults.INITIAL_ROTATION,
	initialPan: Offset = GraphDefaults.INITIAL_PAN,
	minZoom: Float = GraphDefaults.MIN_ZOOM,
	maxZoom: Float = GraphDefaults.MAX_ZOOM,
	fling: Boolean = GraphDefaults.FLING,
	moveToBounds: Boolean = GraphDefaults.MOVE_TO_BOUNDS,
	zoomable: Boolean = GraphDefaults.ZOOMABLE,
	pannable: Boolean = GraphDefaults.PANNABLE,
	rotatable: Boolean = GraphDefaults.ROTATABLE,
	limitPan: Boolean = GraphDefaults.LIMIT_PAN
): GraphState {
	return rememberSaveable(saver = GraphState.Saver) {
		GraphState(
			initialZoom = initialZoom,
			initialRotation = initialRotation,
			initialPan = initialPan,
			minZoom = minZoom,
			maxZoom = maxZoom,
			fling = fling,
			moveToBounds = moveToBounds,
			zoomable = zoomable,
			pannable = pannable,
			rotatable = rotatable,
			limitPan = limitPan
		)
	}
}

@Composable
fun <T> Graph(
	items: List<T>,
	itemColumnKey: (item: T) -> Int,
	linked: (item1: T, item2: T) -> Boolean,
	modifier: Modifier = Modifier,
	state: GraphState = rememberGraphState(),
	mainColumnKey: Int = 0,
	contentPadding: PaddingValues = PaddingValues(16.dp),
	itemPadding: PaddingValues = PaddingValues(
		horizontal = 16.dp,
		vertical = 4.dp
	),
	sameColumnLinkPadding: Dp = 16.dp,
	sameColumnLinkSideOnTheRight: (item1: T, item2: T) -> Boolean = { _, _ -> true },
	item: @Composable (item: T) -> Unit
) {
	val coroutineScope = rememberCoroutineScope()
	SubcomposeLayout(
		modifier = modifier
			.clipToBounds()
			.pointerInput(Unit) {
				detectTransformGestures(
					pass = PointerEventPass.Initial,
					onGestureStart = {
						coroutineScope.launch {
							state.onGestureStart()
						}
					},
					onGestureEnd = {
						coroutineScope.launch {
							state.onGestureEnd {}
						}
					},
					onGesture = { gestureCentroid: Offset,
						gesturePan: Offset,
						gestureZoom: Float,
						rotationChange: Float,
						mainPointer: PointerInputChange,
						changes: List<PointerInputChange> ->
						
						coroutineScope.launch {
							state.onGesture(
								centroid = gestureCentroid,
								pan = gesturePan,
								zoom = gestureZoom,
								rotation = rotationChange,
								mainPointer = mainPointer,
								changes = changes
							)
						}
						
						// Consume touch when multiple fingers down
						// This prevents click and long click if your finger touches a
						// button while pinch gesture is being invoked
						val size = changes.size
						if (size > 1) {
							changes.forEach { it.consume() }
						}
					})
			}
			.graphicsLayer {
				scaleX = state.zoom
				scaleY = state.zoom
				translationX = state.pan.x
				translationY = state.pan.y
				transformOrigin = TransformOrigin(0f, 0f)
			}
	) { constraints ->
		val itemPaddingLeft = itemPadding.calculateLeftPadding(layoutDirection)
			.roundToPx()
		val itemPaddingTop = itemPadding.calculateTopPadding()
			.roundToPx()
		val itemPaddingRight = itemPadding.calculateRightPadding(layoutDirection)
			.roundToPx()
		val itemPaddingBottom = itemPadding.calculateBottomPadding()
			.roundToPx()
		
		val contentPaddingLeft = contentPadding.calculateLeftPadding(layoutDirection)
			.roundToPx()
		val contentPaddingTop = contentPadding.calculateTopPadding()
			.roundToPx()
		val contentPaddingRight = contentPadding.calculateRightPadding(layoutDirection)
			.roundToPx()
		val contentPaddingBottom = contentPadding.calculateBottomPadding()
			.roundToPx()
		
		val itemMeasurables = subcompose(GraphSlot.ITEMS) {
			items.forEach { item -> item(item) }
		}
		
		val placeables = itemMeasurables.map { measurable ->
			measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
		}
		
		val nodes = calcNodes(
			items = items,
			placeables = placeables,
			itemColumnKeyProvider = itemColumnKey,
			mainColumnKey = mainColumnKey,
			linked = linked,
			itemPaddingLeft = itemPaddingLeft,
			itemPaddingTop = itemPaddingTop,
			itemPaddingRight = itemPaddingRight,
			itemPaddingBottom = itemPaddingBottom
		)
		
		val links = nodes.map { node ->
			val linkedNodes = nodes
				.filter { otherNode -> linked(node.item, otherNode.item) }
			
			linkedNodes.map { linkedNode ->
				val lineStart: IntOffset
				val lineCenter: IntOffset
				val lineEnd: IntOffset
				
				// Linked node is on the same X
				if (node.position.x == linkedNode.position.x) {
					val linkSideIsOnTheRight = sameColumnLinkSideOnTheRight(
						node.item,
						linkedNode.item
					)
					
					if (linkSideIsOnTheRight) {
						lineStart = IntOffset(
							x = node.position.x + node.placeable.width,
							y = node.position.y + node.placeable.height / 2
						)
						lineEnd = IntOffset(
							x = linkedNode.position.x + node.placeable.width,
							y = linkedNode.position.y + linkedNode.placeable.height / 2
						)
						lineCenter = IntOffset(
							x = lineStart.x + sameColumnLinkPadding.roundToPx(),
							y = lineStart.y + (lineEnd.y - lineStart.y) / 2
						)
					} else {
						lineStart = IntOffset(
							x = node.position.x,
							y = node.position.y + node.placeable.height / 2
						)
						lineEnd = IntOffset(
							x = linkedNode.position.x,
							y = linkedNode.position.y + linkedNode.placeable.height / 2
						)
						lineCenter = IntOffset(
							x = lineStart.x - sameColumnLinkPadding.roundToPx(),
							y = lineStart.y + (lineEnd.y - lineStart.y) / 2
						)
					}
				} else {
					if (node.position.x < linkedNode.position.x) {
						lineStart = IntOffset(
							x = node.position.x + node.placeable.width,
							y = node.position.y + node.placeable.height / 2
						)
						lineEnd = IntOffset(
							x = linkedNode.position.x,
							y = linkedNode.position.y + linkedNode.placeable.height / 2
						)
					} else {
						lineStart = IntOffset(
							x = node.position.x,
							y = node.position.y + node.placeable.height / 2
						)
						
						lineEnd = IntOffset(
							x = linkedNode.position.x + linkedNode.placeable.width,
							y = linkedNode.position.y + linkedNode.placeable.height / 2
						)
					}
					
					lineCenter = IntOffset(
						x = lineStart.x + (lineEnd.x - lineStart.x) / 2,
						y = lineStart.y + (lineEnd.y - lineStart.y) / 2
					)
				}
				
				Link(
					start = lineStart,
					center = lineCenter,
					end = lineEnd
				)
			}
		}
			.flatten()
		
		val nodesRect = if (nodes.isNotEmpty()) {
			IntRect(
				topLeft = IntOffset(
					x = nodes.minOf { node -> node.position.x } - itemPaddingLeft,
					y = nodes.minOf { node -> node.position.y } - itemPaddingTop
				),
				bottomRight = IntOffset(
					x = nodes.maxOf { node -> node.position.x + node.placeable.width } + itemPaddingRight,
					y = nodes.maxOf { node -> node.position.y + node.placeable.height } + itemPaddingBottom
				)
			)
		} else {
			null
		}
		
		val linksRect = if (links.isNotEmpty()) {
			val linksPoints = links.flatMap { link ->
				listOf(
					link.start,
					link.center,
					link.end
				)
			}
			
			IntRect(
				topLeft = IntOffset(
					x = linksPoints.minOf { linkPoint -> linkPoint.x },
					y = linksPoints.minOf { linkPoint -> linkPoint.y }
				),
				bottomRight = IntOffset(
					x = linksPoints.maxOf { linkPoint -> linkPoint.x },
					y = linksPoints.maxOf { linkPoint -> linkPoint.y }
				)
			)
		} else {
			null
		}
		
		val graphRect = when {
			nodesRect != null && linksRect != null -> {
				IntRect(
					topLeft = IntOffset(
						x = min(nodesRect.left, linksRect.left),
						y = min(nodesRect.top, linksRect.top)
					),
					bottomRight = IntOffset(
						x = max(nodesRect.right, linksRect.right),
						y = max(nodesRect.bottom, linksRect.bottom)
					)
				)
			}
			
			nodesRect != null -> nodesRect
			linksRect != null -> linksRect
			else -> {
				IntRect(
					offset = IntOffset.Zero,
					size = IntSize(
						width = constraints.minWidth,
						height = constraints.minHeight
					)
				)
			}
		}
		
		val graphOffset = -graphRect.topLeft
		val linesComponentPosition = linksRect?.topLeft?.plus(graphOffset) ?: IntOffset.Zero
		
		val localLinks = if (linksRect != null) {
			links
				.map { link ->
					link.copy(
						start = link.start - linksRect.topLeft,
						center = link.center - linksRect.topLeft,
						end = link.end - linksRect.topLeft
					)
				}
		} else {
			emptyList()
		}
		
		val linesPlaceable = subcompose(GraphSlot.LINES) {
			Canvas(Modifier.fillMaxSize()) {
				localLinks.forEach { link ->
					drawArrow(
						start = link.start.toOffset(),
						center = link.center.toOffset(),
						end = link.end.toOffset(),
						color = Color.Gray,
						maxCornerRadiusPx = 8.dp.toPx(),
						lineWidthPx = 1.dp.toPx(),
						triangleLengthPx = 3.dp.toPx(),
						triangleWidthPx = 7.dp.toPx()
					)
				}
			}
		}
			.first()
			.measure(
				constraints.copy(
					minWidth = 0,
					minHeight = 0,
					maxWidth = linksRect?.width ?: 0,
					maxHeight = linksRect?.height ?: 0
				)
			)
		
		val containerSize = IntSize(
			width = graphRect.width.coerceAtMost(constraints.maxWidth),
			height = graphRect.height.coerceAtMost(constraints.maxHeight)
		)
		
		state.layoutInfo = GraphLayoutInfo(
			itemRectList = items.map { item ->
				nodes
					.firstOrNull { otherNode -> otherNode.item == item }
					?.let { node ->
						IntRect(
							offset = node.position,
							size = IntSize(
								width = node.placeable.width,
								height = node.placeable.height
							)
						)
					} ?: IntRect.Zero
			},
			containerSize = containerSize,
			movableArea = graphRect,
			contentPaddingLeft = contentPaddingLeft,
			contentPaddingTop = contentPaddingTop,
			contentPaddingRight = contentPaddingRight,
			contentPaddingBottom = contentPaddingBottom
		)
		
		layout(containerSize.width, containerSize.height) {
			linesPlaceable.place(linesComponentPosition.x, linesComponentPosition.y)
			nodes.forEach { node ->
				node.placeable.place(
					x = node.position.x + graphOffset.x,
					y = node.position.y + graphOffset.y
				)
			}
		}
	}
}

private fun <T> calcNodes(
	items: List<T>,
	placeables: List<Placeable>,
	itemColumnKeyProvider: (item: T) -> Int,
	mainColumnKey: Int,
	linked: (item1: T, item2: T) -> Boolean,
	itemPaddingLeft: Int,
	itemPaddingTop: Int,
	itemPaddingRight: Int,
	itemPaddingBottom: Int
): List<Node<T>> {
	val columns = items.zip(placeables)
		.groupBy { itemColumnKeyProvider(it.first) }
		.toSortedMap()
	
	val mainColumn = columns[mainColumnKey] ?: columns.entries.first().value
	
	val columnsOfItems = columns.mapValues { (_, itemAndPlaceableList) ->
		itemAndPlaceableList.map { itemAndPlaceable -> itemAndPlaceable.first }
	}
	
	val roots = mainColumn
		.map { (mainColumnItem, _) ->
			findRoots(
				item = mainColumnItem,
				itemColumnKeyProvider = itemColumnKeyProvider,
				columns = columnsOfItems,
				linked = linked
			)
		}
		.flatten()
		.toSet()
		.toList()
	
	return calcNodesByRoots(
		roots = roots,
		itemColumnKeyProvider = itemColumnKeyProvider,
		linked = linked,
		columns = columns,
		itemPaddingLeft = itemPaddingLeft,
		itemPaddingTop = itemPaddingTop,
		itemPaddingRight = itemPaddingRight,
		itemPaddingBottom = itemPaddingBottom
	)
}

private fun <T> findRoots(
	item: T,
	itemColumnKeyProvider: (item: T) -> Int,
	columns: Map<Int, List<T>>,
	linked: (item1: T, item2: T) -> Boolean,
): List<T> {
	val itemColumnKey = itemColumnKeyProvider(item)
	val parents = columns
		.filterKeys { columnKey ->
			columnKey < itemColumnKey
		}
		.mapValues { (_, otherItems) ->
			otherItems.filter { otherItem -> linked(otherItem, item) }
		}
		.toSortedMap()
		.flatMap { (_, parents) -> parents }
	
	return if (parents.isEmpty()) {
		listOf(item)
	} else {
		parents.flatMap { parent ->
			findRoots(
				item = parent,
				itemColumnKeyProvider = itemColumnKeyProvider,
				columns = columns,
				linked = linked
			)
		}
	}
}

private fun <T> calcNodesByRoots(
	roots: List<T>,
	itemColumnKeyProvider: (item: T) -> Int,
	linked: (item1: T, item2: T) -> Boolean,
	columns: Map<Int, List<Pair<T, Placeable>>>,
	itemPaddingLeft: Int,
	itemPaddingTop: Int,
	itemPaddingRight: Int,
	itemPaddingBottom: Int
): List<Node<T>> {
	val columnWidths = columns.mapValues { (_, itemToPlaceableList) ->
		val maxItemWidth = itemToPlaceableList.maxOfOrNull { (_, placeable) -> placeable.width }
		
		if (maxItemWidth != null) {
			itemPaddingLeft + maxItemWidth + itemPaddingRight
		} else {
			0
		}
	}
	
	val columnXPositions = columnWidths.toSortedMap()
		.mapValues(object : (Map.Entry<Int, Int>) -> Int {
			var x = 0
			override fun invoke(columnKeyToWidth: Map.Entry<Int, Int>): Int {
				val columnWidth = x
				x += columnKeyToWidth.value
				return columnWidth
			}
		})
	
	val yPositions = mutableMapOf<Int, Int>()
	val itemToNodeMap: MutableMap<T, Node<T>> = mutableMapOf()
	
	return calcNodesByRoots(
		roots = roots,
		itemColumnKeyProvider = itemColumnKeyProvider,
		linked = linked,
		itemPaddingLeft = itemPaddingLeft,
		itemPaddingTop = itemPaddingTop,
		itemPaddingBottom = itemPaddingBottom,
		columns = columns,
		columnXPositions = columnXPositions,
		yPositions = yPositions,
		itemToNodeMap = itemToNodeMap
	)
}

/**
 * @param yPositions Хранит следующий y на котором может быть размещен следующий item в колонке.
 * Ключ это номер колонки. Значение это минимально допкстимая позиция следующего элемента в колонке.
 * @param itemToNodeMap Хранит уже созданные ноды для item.
 */
private fun <T> calcNodesByRoots(
	roots: List<T>,
	itemColumnKeyProvider: (item: T) -> Int,
	linked: (item1: T, item2: T) -> Boolean,
	itemPaddingLeft: Int,
	itemPaddingTop: Int,
	itemPaddingBottom: Int,
	columns: Map<Int, List<Pair<T, Placeable>>>,
	columnXPositions: Map<Int, Int>,
	yPositions: MutableMap<Int, Int>,
	itemToNodeMap: MutableMap<T, Node<T>>,
): List<Node<T>> {
	val result = mutableListOf<Node<T>>()
	for (root in roots) {
		// Если item уже размещен, пропускаем его
		if (itemToNodeMap[root] != null) continue
		
		val parentColumnKey = itemColumnKeyProvider(root)
		
		val placeable = columns[parentColumnKey]
			?.firstOrNull { columnItem -> columnItem.first == root }
			?.second ?: continue
		
		val itemY = yPositions[parentColumnKey] ?: 0
		val nodeY = itemY + itemPaddingTop
		val nextItemPosition = nodeY + placeable.height + itemPaddingBottom
		
		val node = Node(
			item = root,
			placeable = placeable,
			position = IntOffset(
				x = (columnXPositions[parentColumnKey] ?: continue) + itemPaddingLeft,
				y = nodeY
			)
		)
		
		itemToNodeMap[root] = node
		result += node
		
		// Следующие элементы в предыдущих колонках должны быть ниже этой ноды
		columns.keys
			.filter { columnKey -> columnKey <= parentColumnKey }
			.forEach { columnKey ->
				val currentYPosition = yPositions[columnKey] ?: 0
				yPositions[columnKey] = maxOf(currentYPosition, nextItemPosition)
			}
		
		// Если в следующих колонках есть дети, то следующие элементы в этих колонках должны быть не выше этой ноды
		columns.keys
			.filter { columnKey -> columnKey > parentColumnKey }
			.forEach { columnKey ->
				val currentYPosition = yPositions[columnKey] ?: 0
				yPositions[columnKey] = maxOf(currentYPosition, itemY)
			}
		
		// Ищем детей начиная от последней колонки до колонки родителя (не включая колонки родителя)
		val childItemToPlaceableList = columns
			.filterKeys { otherColumnKey -> otherColumnKey > parentColumnKey }
			.mapValues { (_, otherItemToPlaceableList) ->
				otherItemToPlaceableList.filter { (otherItem, _) ->
					linked(root, otherItem)
				}
			}
			.toSortedMap(reverseOrder())
			.values
			.flatten()
			.toMap()
			.keys
			.toList()
		
		// Если дети есть, повоторяем алгоритм для них
		if (childItemToPlaceableList.isNotEmpty()) {
			result += calcNodesByRoots(
				roots = childItemToPlaceableList,
				itemColumnKeyProvider = itemColumnKeyProvider,
				linked = linked,
				itemPaddingLeft = itemPaddingLeft,
				itemPaddingTop = itemPaddingTop,
				itemPaddingBottom = itemPaddingBottom,
				columns = columns,
				columnXPositions = columnXPositions,
				yPositions = yPositions,
				itemToNodeMap = itemToNodeMap
			)
		}
	}
	
	return result
}

private data class Node<T>(
	val item: T,
	val placeable: Placeable,
	val position: IntOffset
)

private enum class GraphSlot {
	ITEMS,
	LINES
}

private data class Link(
	val start: IntOffset,
	val center: IntOffset,
	val end: IntOffset
)

private fun DrawScope.drawArrow(
	start: Offset,
	center: Offset,
	end: Offset,
	color: Color,
	maxCornerRadiusPx: Float,
	lineWidthPx: Float,
	triangleLengthPx: Float,
	triangleWidthPx: Float
) {
	val path = Path()
	path.moveTo(start.x, start.y)
	
	path.lineTo(center.x, start.y)
	val lastLineSegmentStart = Offset(x = center.x, end.y)
	path.lineTo(lastLineSegmentStart.x, lastLineSegmentStart.y)
	path.lineTo(end.x, end.y)
	
	drawPath(
		path = path,
		color = color,
		style = Stroke(
			width = lineWidthPx,
			pathEffect = PathEffect.cornerPathEffect(maxCornerRadiusPx)
		)
	)
	
	val isRight = if (end.x - lastLineSegmentStart.x >= 0) 1 else -1
	val triangleBottomX = end.x - isRight * triangleLengthPx
	val triangleHalfWidth = triangleWidthPx / 2
	path.rewind()
	path.moveTo(triangleBottomX, end.y - triangleHalfWidth)
	path.lineTo(end.x, end.y)
	path.lineTo(triangleBottomX, end.y + triangleHalfWidth)
	
	drawPath(
		path = path,
		color = color,
		style = Stroke(
			width = lineWidthPx,
			cap = StrokeCap.Round,
			pathEffect = PathEffect.cornerPathEffect(lineWidthPx)
		)
	)
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(
	showBackground = true,
	widthDp = 1800,
	heightDp = 1600
)
@Preview(showBackground = true)
@Composable
private fun GraphPreview() {
	fun itemLevel(item: Int): Int {
		return if (item == 50) {
			0
		} else if (item < 50) {
			(item % 5) - 5
		} else {
			(item % 5) + 1
		}
	}
	
	val graphState = rememberGraphState()
	val coroutineScope = rememberCoroutineScope()
	
	Column(Modifier.fillMaxSize()) {
		Graph(
			items = remember {
				(0..100).toList()
			},
			itemColumnKey = { item ->
				if (item == 50) {
					0
				} else {
					val columnIndex = itemLevel(item)
					if (columnIndex == -1 || columnIndex == 1) 0 else columnIndex
				}
				
			},
			linked = { item1, item2 ->
				// -3
				item1 == 11 && item2 == 27 ||
						item1 == 11 && item2 == 32 ||
						item1 == 11 && item2 == 37 ||
						item1 == 11 && item2 == 42 ||
						
						// -2
						item1 == 12 && item2 == 13 ||
						item1 == 17 && item2 == 13 ||
						item1 == 22 && item2 == 24 ||
						item1 == 27 && item2 == 28 ||
						item1 == 27 && item2 == 33 ||
						item1 == 27 && item2 == 38 ||
						item1 == 47 && item2 == 44 ||
						
						// -1
						item1 == 3 && item2 == 4 ||
						item1 == 8 && item2 == 4 ||
						item1 == 13 && item2 == 14 ||
						item1 == 18 && item2 == 19 ||
						item1 == 28 && item2 == 29 ||
						
						// 0
						item1 == 19 && item2 == 50 ||
						item1 == 50 && item2 == 55 ||
						item1 == 55 && item2 == 61 ||
						item1 == 55 && item2 == 71 ||
						item1 == 55 && item2 == 81 ||
						item1 == 65 && item2 == 81 ||
						item1 == 75 && item2 == 56 ||
						item1 == 75 && item2 == 66 ||
						item1 == 75 && item2 == 76 ||
						
						// 1
						item1 == 61 && item2 == 57 ||
						item1 == 71 && item2 == 67 ||
						item1 == 71 && item2 == 77 ||
						item1 == 71 && item2 == 82 ||
						item1 == 81 && item2 == 52 ||
						
						// 2
						item1 == 56 && item2 == 58 ||
						item1 == 56 && item2 == 68 ||
						item1 == 66 && item2 == 52
			},
			
			modifier = Modifier
				.weight(1f)
				.background(Color.LightGray),
			state = graphState,
			sameColumnLinkSideOnTheRight = { item1, _ ->
				val columnIndex = itemLevel(item1)
				columnIndex < 0
			},
		) { item ->
			Card(
				modifier = Modifier.width(100.dp),
				colors = CardDefaults.cardColors(
					containerColor = Color.White
				),
				border = BorderStroke(
					width = 1.dp,
					color = Color.Gray
				)
			) {
				Text(
					text = when (item) {
						13 -> "13\n13"
						34 -> "34\n".repeat(4)
						37 -> "37\n".repeat(4)
						else -> item.toString()
					},
					modifier = Modifier.padding(16.dp),
					color = Color.Black
				)
			}
		}
		
		FlowRow(Modifier.fillMaxWidth()) {
			Button(
				onClick = {
					coroutineScope.launch {
						graphState.animateCameraPosition {
							containerCenter(zoom = it.zoom - 0.5f)
						}
					}
				}
			) {
				Text(text = "-")
			}
			Button(
				onClick = {
					coroutineScope.launch {
						graphState.animateCameraPosition {
							containerCenter(zoom = it.zoom + 0.5f)
						}
					}
				}
			) {
				Text(text = "+")
			}
			Button(
				onClick = {
					coroutineScope.launch {
						graphState.animateCameraPosition {
							fitItems(itemIndexes = listOf(3))
						}
					}
				}
			) {
				Text(text = "4")
			}
			Button(
				onClick = {
					coroutineScope.launch {
						graphState.animateCameraPosition {
							fitItems(itemIndexes = listOf(55, 61, 57))
						}
					}
				}
			) {
				Text(text = "55, 61, 57")
			}
			Button(
				onClick = {
					coroutineScope.launch {
						graphState.animateCameraPosition {
							itemsCenter(
								itemIndexes = listOf(50),
								zoom = 1f
							)
						}
					}
				}
			) {
				Icon(
					painter = painterResource(R.drawable.ic_home_24dp),
					contentDescription = null
				)
			}
		}
	}
}