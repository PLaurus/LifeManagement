package com.lauruspa.life_management.core.ui.modifier.transformations


import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

/**
 * A state object that manage scale and offset.
 *
 * @param maxScale The maximum scale of the content.
 * @param contentSize Size of content (i.e. image size.) If Zero, the composable layout size will
 * be used as content size.
 * @param velocityDecay The decay animation spec for fling behaviour.
 * @param initialScale The initial scale of the content.
 */
@Stable
class TransformationsState(
	@FloatRange(from = 1.0) val maxScale: Float = 5f,
	private var contentSize: Size = Size.Zero,
	private val velocityDecay: DecayAnimationSpec<Float> = exponentialDecay(),
	@FloatRange(from = 1.0) private val initialScale: Float = 1f,
) {
	init {
		require(maxScale >= 1.0f) { "maxScale must be at least 1.0." }
		require(initialScale >= 1.0f) { "initialScale must be at least 1.0." }
	}
	
	private var _scale = Animatable(initialScale).apply {
		updateBounds(0.9f, maxScale)
	}
	
	/**
	 * The scale of the content.
	 */
	val scale: Float
		get() = _scale.value
	
	private var _offsetX = Animatable(0f)
	
	/**
	 * The horizontal offset of the content.
	 */
	val offsetX: Float
		get() = _offsetX.value
	
	private var _offsetY = Animatable(0f)
	
	/**
	 * The vertical offset of the content.
	 */
	val offsetY: Float
		get() = _offsetY.value
	
	private var layoutSize = Size.Zero
	
	/**
	 * Set composable layout size.
	 *
	 * Basically This function is called from [Modifier.zoomable] only.
	 *
	 * @param size The size of composable layout size.
	 */
	fun setLayoutSize(size: Size) {
		layoutSize = if (size.isUnspecified) Size.Zero else size
		updateFitContentSize()
	}
	
	/**
	 * Set the content size.
	 *
	 * @param size The content size, for example an image size in pixel.
	 */
	fun setContentSize(size: Size) {
		contentSize = if (size.isUnspecified) Size.Zero else size
		updateFitContentSize()
	}
	
	private var fitContentSize = Size.Zero
	private fun updateFitContentSize() {
		if (layoutSize == Size.Zero) {
			fitContentSize = Size.Zero
			return
		}
		
		if (contentSize == Size.Zero) {
			fitContentSize = layoutSize
			return
		}
		
		val contentAspectRatio = contentSize.width / contentSize.height
		val layoutAspectRatio = layoutSize.width / layoutSize.height
		
		fitContentSize = if (contentAspectRatio > layoutAspectRatio) {
			contentSize * (layoutSize.width / contentSize.width)
		} else {
			contentSize * (layoutSize.height / contentSize.height)
		}
	}
	
	/**
	 * Reset the scale and the offsets.
	 */
	suspend fun reset(): Unit = coroutineScope {
		launch { _scale.snapTo(initialScale) }
		_offsetX.updateBounds(0f, 0f)
		launch { _offsetX.snapTo(0f) }
		_offsetY.updateBounds(0f, 0f)
		launch { _offsetY.snapTo(0f) }
	}
	
	private val velocityTracker = VelocityTracker()
	
	internal fun startGesture() {
		velocityTracker.resetTracking()
	}
	
	internal fun willChangeOffset(pan: Offset): Boolean {
		var willChange = true
		val ratio = (abs(pan.x) / abs(pan.y))
		if (ratio > 3) { // Horizontal drag
			if ((pan.x < 0) && (_offsetX.value == _offsetX.lowerBound)) {
				// Drag R to L when right edge of the content is shown.
				willChange = false
			}
			if ((pan.x > 0) && (_offsetX.value == _offsetX.upperBound)) {
				// Drag L to R when left edge of the content is shown.
				willChange = false
			}
		} else if (ratio < 0.33) { // Vertical drag
			if ((pan.y < 0) && (_offsetY.value == _offsetY.lowerBound)) {
				// Drag bottom to top when bottom edge of the content is shown.
				willChange = false
			}
			if ((pan.y > 0) && (_offsetY.value == _offsetY.upperBound)) {
				// Drag top to bottom when top edge of the content is shown.
				willChange = false
			}
		}
		return willChange
	}
	
	internal suspend fun applyGesture(
		pan: Offset,
		zoom: Float,
		position: Offset,
		timeMillis: Long,
		enableBounce: Boolean = true,
	) = coroutineScope {
		val minScale = if (enableBounce) 0.9f else 1f
		val newScale = (scale * zoom).coerceIn(minScale, maxScale)
		val newOffset = calculateNewOffset(newScale, position, pan)
		val newBounds = calculateNewBounds(newScale)
		
		_offsetX.updateBounds(newBounds.left, newBounds.right)
		launch {
			_offsetX.snapTo(newOffset.x)
		}
		
		_offsetY.updateBounds(newBounds.top, newBounds.bottom)
		launch {
			_offsetY.snapTo(newOffset.y)
		}
		
		launch {
			_scale.snapTo(newScale)
		}
		
		if (zoom == 1f) {
			velocityTracker.addPosition(timeMillis, position)
		} else {
			velocityTracker.resetTracking()
		}
	}
	
	internal fun applyPan(pan: Offset, coroutineScope: CoroutineScope): Offset {
		val bounds = calculateNewBounds(scale)
		val newOffsetX = (_offsetX.value + pan.x).coerceIn(bounds.left, bounds.right)
		val newOffsetY = (_offsetY.value + pan.y).coerceIn(bounds.top, bounds.bottom)
		val consumedX = newOffsetX - _offsetX.value
		val consumedY = newOffsetY - _offsetY.value
		coroutineScope.launch {
			_offsetX.snapTo(newOffsetX)
		}
		coroutineScope.launch {
			_offsetY.snapTo(newOffsetY)
		}
		return Offset(consumedX, consumedY)
	}
	
	/**
	 * Change the scale with animation.
	 *
	 * Zoom in or out to [targetScale] around the [position].
	 *
	 * @param targetScale The target scale value.
	 * @param position Zoom around this point.
	 * @param animationSpec The animation configuration.
	 */
	suspend fun changeScale(
		targetScale: Float,
		position: Offset,
		animationSpec: AnimationSpec<Float> = spring(),
	): Unit = coroutineScope {
		val newScale = targetScale.coerceIn(1f, maxScale)
		val newOffset = calculateNewOffset(newScale, position, Offset.Zero)
		val newBounds = calculateNewBounds(newScale)
		
		val x = newOffset.x.coerceIn(newBounds.left, newBounds.right)
		launch {
			_offsetX.updateBounds(null, null)
			_offsetX.animateTo(x, animationSpec)
			_offsetX.updateBounds(newBounds.left, newBounds.right)
		}
		
		val y = newOffset.y.coerceIn(newBounds.top, newBounds.bottom)
		launch {
			_offsetY.updateBounds(null, null)
			_offsetY.animateTo(y, animationSpec)
			_offsetY.updateBounds(newBounds.top, newBounds.bottom)
		}
		
		launch {
			_scale.animateTo(newScale, animationSpec)
		}
	}
	
	private fun calculateNewOffset(newScale: Float, position: Offset, pan: Offset): Offset {
		val size = fitContentSize * scale
		val newSize = fitContentSize * newScale
		val deltaWidth = newSize.width - size.width
		val deltaHeight = newSize.height - size.height
		
		// Position with the origin at the left top corner of the content.
		val xInContent = position.x - offsetX + (size.width - layoutSize.width) * 0.5f
		val yInContent = position.y - offsetY + (size.height - layoutSize.height) * 0.5f
		// Amount of offset change required to zoom around the position.
		val deltaX = (deltaWidth * 0.5f) - (deltaWidth * xInContent / size.width)
		val deltaY = (deltaHeight * 0.5f) - (deltaHeight * yInContent / size.height)
		
		val x = offsetX + pan.x + deltaX
		val y = offsetY + pan.y + deltaY
		
		return Offset(x, y)
	}
	
	private fun calculateNewBounds(newScale: Float): Rect {
		val newSize = fitContentSize * newScale
		val boundX = max((newSize.width - layoutSize.width), 0f) * 0.5f
		val boundY = max((newSize.height - layoutSize.height), 0f) * 0.5f
		return Rect(-boundX, -boundY, boundX, boundY)
	}
	
	internal suspend fun startFling() = coroutineScope {
		val velocity = velocityTracker.calculateVelocity()
		if (velocity.x != 0f) {
			launch {
				_offsetX.animateDecay(velocity.x, velocityDecay)
			}
		}
		if (velocity.y != 0f) {
			launch {
				_offsetY.animateDecay(velocity.y, velocityDecay)
			}
		}
	}
	
	/**
	 * Animates the centering of content by modifying the offset and scale based on content coordinates.
	 *
	 * @param offset The offset to apply for centering the content.
	 * @param scale The scale to apply for zooming the content.
	 * @param animationSpec AnimationSpec for centering and scaling.
	 */
	suspend fun centerByContentCoordinate(
		offset: Offset,
		scale: Float = 3f,
		animationSpec: AnimationSpec<Float> = tween(700),
	): Unit = coroutineScope {
		val fitContentSizeFactor = fitContentSize.width / contentSize.width
		
		val boundX = max((fitContentSize.width * scale - layoutSize.width), 0f) / 2f
		val boundY = max((fitContentSize.height * scale - layoutSize.height), 0f) / 2f
		
		suspend fun executeZoomWithAnimation() {
			listOf(
				async {
					val fixedTargetOffsetX =
						((fitContentSize.width / 2 - offset.x * fitContentSizeFactor) * scale)
							.coerceIn(
								minimumValue = -boundX,
								maximumValue = boundX,
							) // Adjust zoom target position to prevent execute zoom animation to out of content boundaries
					_offsetX.animateTo(fixedTargetOffsetX, animationSpec)
				},
				async {
					val fixedTargetOffsetY =
						((fitContentSize.height / 2 - offset.y * fitContentSizeFactor) * scale)
							.coerceIn(minimumValue = -boundY, maximumValue = boundY)
					_offsetY.animateTo(fixedTargetOffsetY, animationSpec)
				},
				async {
					_scale.animateTo(scale, animationSpec)
				},
			).awaitAll()
		}
		
		if (scale > _scale.value) {
			_offsetX.updateBounds(-boundX, boundX)
			_offsetY.updateBounds(-boundY, boundY)
			executeZoomWithAnimation()
		} else {
			executeZoomWithAnimation()
			_offsetX.updateBounds(-boundX, boundX)
			_offsetY.updateBounds(-boundY, boundY)
		}
	}
	
	/**
	 * Animates the centering of content by modifying the offset and scale based on layout coordinates.
	 *
	 * @param offset The offset to apply for centering the content.
	 * @param scale The scale to apply for zooming the content.
	 * @param animationSpec AnimationSpec for centering and scaling.
	 */
	public suspend fun centerByLayoutCoordinate(
		offset: Offset,
		scale: Float = 3f,
		animationSpec: AnimationSpec<Float> = tween(700),
	): Unit = coroutineScope {
		val boundX = max((fitContentSize.width * scale - layoutSize.width), 0f) / 2f
		val boundY = max((fitContentSize.height * scale - layoutSize.height), 0f) / 2f
		
		suspend fun executeZoomWithAnimation() {
			listOf(
				async {
					val fixedTargetOffsetX =
						((layoutSize.width / 2 - offset.x) * scale)
							.coerceIn(
								minimumValue = -boundX,
								maximumValue = boundX,
							) // Adjust zoom target position to prevent execute zoom animation to out of content boundaries
					_offsetX.animateTo(fixedTargetOffsetX, animationSpec)
				},
				async {
					val fixedTargetOffsetY = ((layoutSize.height / 2 - offset.y) * scale)
						.coerceIn(minimumValue = -boundY, maximumValue = boundY)
					_offsetY.animateTo(fixedTargetOffsetY, animationSpec)
				},
				async {
					_scale.animateTo(scale, animationSpec)
				},
			).awaitAll()
		}
		
		if (scale > _scale.value) {
			_offsetX.updateBounds(-boundX, boundX)
			_offsetY.updateBounds(-boundY, boundY)
			executeZoomWithAnimation()
		} else {
			executeZoomWithAnimation()
			_offsetX.updateBounds(-boundX, boundX)
			_offsetY.updateBounds(-boundY, boundY)
		}
	}
}

/**
 * Creates a [TransformationsState] that is remembered across compositions.
 *
 * @param maxScale The maximum scale of the content.
 * @param contentSize Size of content (i.e. image size.) If Zero, the composable layout size will
 * be used as content size.
 * @param velocityDecay The decay animation spec for fling behaviour.
 * @param initialScale The initial scale of the content.
 */
@Composable
fun rememberZoomState(
	@FloatRange(from = 1.0) maxScale: Float = 5f,
	contentSize: Size = Size.Zero,
	velocityDecay: DecayAnimationSpec<Float> = exponentialDecay(),
	@FloatRange(from = 1.0) initialScale: Float = 1f,
): TransformationsState = remember {
	TransformationsState(maxScale, contentSize, velocityDecay, initialScale)
}