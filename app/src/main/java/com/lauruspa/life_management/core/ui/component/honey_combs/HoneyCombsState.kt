package com.lauruspa.life_management.core.ui.component.honey_combs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.lauruspa.life_management.core.ui.coerceIn
import com.lauruspa.life_management.core.ui.toOffset
import kotlin.math.roundToInt

@Stable
class HoneyCombsState internal constructor(
    initialZoom: Float = HoneyCombsDefaults.INITIAL_ZOOM,
    initialRotation: Float = HoneyCombsDefaults.INITIAL_ROTATION,
    internal val initialPan: Offset = HoneyCombsDefaults.INITIAL_PAN,
    internal val minZoom: Float = HoneyCombsDefaults.MIN_ZOOM,
    maxZoom: Float = HoneyCombsDefaults.MAX_ZOOM,
    val fling: Boolean = HoneyCombsDefaults.FLING,
    val moveToBounds: Boolean = HoneyCombsDefaults.MOVE_TO_BOUNDS,
    internal val zoomable: Boolean = HoneyCombsDefaults.ZOOMABLE,
    internal val pannable: Boolean = HoneyCombsDefaults.PANNABLE,
    internal val rotatable: Boolean = HoneyCombsDefaults.ROTATABLE,
    internal val limitPan: Boolean = HoneyCombsDefaults.LIMIT_PAN,
    initialLayoutInfo: HoneyCombsLayoutInfo = HoneyCombsDefaults.INITIAL_LAYOUT_INFO
) {
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
    var layoutInfo: HoneyCombsLayoutInfo = initialLayoutInfo
        internal set

    @Suppress("MemberVisibilityCanBePrivate")
    internal val animatablePan = Animatable(initialPan, Offset.VectorConverter)

    @Suppress("MemberVisibilityCanBePrivate")
    internal val animatableZoom = Animatable(this.initialZoom).apply {
        updateBounds(this@HoneyCombsState.minZoom, this@HoneyCombsState.maxZoom)
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

    private val cameraPositionCalculator: CameraPositionCalculator = CameraPositionCalculatorImpl()

    internal fun updatePanBounds(lowerBound: Offset?, upperBound: Offset?) {
        animatablePan.updateBounds(lowerBound, upperBound)
    }

    internal fun getPanBounds(layoutInfo: HoneyCombsLayoutInfo): Rect {
        val offsetArea = calcOffsetArea(
            zoom = zoom,
            movableArea = layoutInfo.movableAreaRect,
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
        newZoomProvider: suspend (HoneyCombsLayoutInfo) -> CameraPosition
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
        cameraPositionProvider: suspend CameraPositionCalculator.(HoneyCombsState) -> CameraPosition
    ) = cameraPositionChangeMutex.mutate {
        coroutineScope {
            val newZoomState = cameraPositionCalculator.cameraPositionProvider(this@HoneyCombsState)
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
    ): CameraPosition {
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

        return CameraPosition(
            zoom = newZoom,
            rotation = newRotation,
            pan = newPan
        )
    }

    interface CameraPositionCalculator {
        fun containerCenter(zoom: Float): CameraPosition
        fun itemsCenter(zoom: Float): CameraPosition
        fun fitItems(withPadding: Boolean = true): CameraPosition
    }

    private inner class CameraPositionCalculatorImpl : CameraPositionCalculator {
        override fun containerCenter(zoom: Float): CameraPosition {
            val containerCenter = layoutInfo.containerSize.center.toOffset()
            return calcNewCameraPosition(
                centroid = containerCenter,
                panChange = Offset.Zero,
                zoomChange = zoom / this@HoneyCombsState.zoom,
                rotationChange = 0f
            )
        }

        override fun itemsCenter(zoom: Float): CameraPosition {
            val itemsCenter = layoutInfo.itemsRect.center.toOffset()
            return calcNewCameraPosition(
                centroid = itemsCenter,
                panChange = Offset.Zero,
                zoomChange = zoom / this@HoneyCombsState.zoom,
                rotationChange = 0f
            )
        }

        override fun fitItems(withPadding: Boolean): CameraPosition {
            val containerSize = layoutInfo.containerSize
            val itemsSize = layoutInfo.itemsRect.size
            val paddingLeft = layoutInfo.paddingLeft
            val paddingTop = layoutInfo.paddingLeft
            val paddingRight = layoutInfo.paddingRight
            val paddingBottom = layoutInfo.paddingRight
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
                layoutInfo.itemsRect.run {
                    copy(
                        left = left - (paddingLeft * newZoom).roundToInt(),
                        top = top - (paddingTop * newZoom).roundToInt(),
                        right = right + (paddingRight * newZoom).roundToInt(),
                        bottom = bottom + (paddingBottom * newZoom).roundToInt()
                    )
                }
            } else {
                layoutInfo.itemsRect
            }

            return CameraPosition(
                zoom = newZoom,
                rotation = 0f,
                pan = -(targetItemsRect.center.toOffset() * newZoom - containerSize.toOffset() / 2f)
            )
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
                HoneyCombsState(
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
	                initialLayoutInfo = savedStateMap[INITIAL_LAYOUT_INFO_KEY] as HoneyCombsLayoutInfo
                )
            }
        )
    }
}

@Composable
fun rememberHoneyCombsState(
    initialZoom: Float = HoneyCombsDefaults.INITIAL_ZOOM,
    initialRotation: Float = HoneyCombsDefaults.INITIAL_ROTATION,
    initialPan: Offset = HoneyCombsDefaults.INITIAL_PAN,
    minZoom: Float = HoneyCombsDefaults.MIN_ZOOM,
    maxZoom: Float = HoneyCombsDefaults.MAX_ZOOM,
    fling: Boolean = HoneyCombsDefaults.FLING,
    moveToBounds: Boolean = HoneyCombsDefaults.MOVE_TO_BOUNDS,
    zoomable: Boolean = HoneyCombsDefaults.ZOOMABLE,
    pannable: Boolean = HoneyCombsDefaults.PANNABLE,
    rotatable: Boolean = HoneyCombsDefaults.ROTATABLE,
    limitPan: Boolean = HoneyCombsDefaults.LIMIT_PAN
): HoneyCombsState {
    return rememberSaveable(saver = HoneyCombsState.Saver) {
        HoneyCombsState(
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