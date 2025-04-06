package com.lauruspa.life_management.core.ui.utils

import android.view.ViewParent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.overscroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.core.view.doOnAttach
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NestedScrollInteropBox(
    modifier: Modifier = Modifier,
    state: ScrollViewInteropState = rememberScrollViewInteropState(),
    isVertical: Boolean = true,
    isEnabled: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    showDebugInfo: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val nestedScrollInterop = rememberNestedScrollInteropConnection()

    var androidViewHolder by remember { mutableStateOf<ViewParent?>(null) }
    val view = LocalView.current

    key(view) {
        view.doOnAttach {
            androidViewHolder = it.findParentViewByClassName(
                className = "androidx.compose.ui.viewinterop.ViewFactoryHolder"
            )
        }
    }

    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        Box(
            modifier = modifier
                .nestedScroll(nestedScrollInterop)
                .conditionalThen(
                    condition = isVertical,
                    ifTrue = {
                        verticalScrollViewInterop(
                            state = state,
                            androidViewHolder = androidViewHolder
                        )
                    },
                    ifFalse = {
                        horizontalScrollViewInterop(
                            state = state,
                            androidViewHolder = androidViewHolder
                        )
                    }
                ),
            contentAlignment = contentAlignment,
            propagateMinConstraints = propagateMinConstraints
        ) {
            content()

            if (showDebugInfo) {
                Column(
                    modifier = Modifier
                        .background(Color.LightGray)
                        .align(Alignment.TopStart)
                ) {
                    Text(text = "Is enabled: $isEnabled")
                    Text(text = "Current position: ${state.value}")
                    Text(text = "Consumed delta by children: ${state.consumedDeltaByChildren}")
                    Text(text = "Available delta: ${state.availableDelta}")
                }
            }
        }
    }
}

@Composable
fun rememberScrollViewInteropState(): ScrollViewInteropState {
    return remember { ScrollViewInteropState() }
}

@Stable
class ScrollViewInteropState : ScrollableState {
    var value by mutableStateOf(MovementPosition.Center, structuralEqualityPolicy())
        internal set

    val maxValue = MovementPosition.End

    private val scrollableState = ScrollableState { 0f }

    var consumedDeltaByChildren: Float by mutableStateOf(0f)
        internal set

    var availableDelta: Float by mutableStateOf(0f)
        internal set

    /**
     * [InteractionSource] that will be used to dispatch drag events when this
     * list is being dragged. If you want to know whether the fling (or smooth scroll) is in
     * progress, use [isScrollInProgress].
     */
    @Suppress("unused")
    val interactionSource: InteractionSource get() = internalInteractionSource

    internal val internalInteractionSource: MutableInteractionSource = MutableInteractionSource()

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ): Unit = scrollableState.scroll(scrollPriority, block)

    override fun dispatchRawDelta(delta: Float): Float =
        scrollableState.dispatchRawDelta(delta)

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress
}

enum class MovementPosition {
    Start,
    Center,
    End
}

fun Modifier.verticalScrollViewInterop(
    state: ScrollViewInteropState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
    androidViewHolder: ViewParent? = null
) = scroll(
    state = state,
    isEnabled = enabled,
    reverseScrolling = reverseScrolling,
    flingBehavior = flingBehavior,
    isVertical = true,
    androidViewHolder = androidViewHolder
)

fun Modifier.horizontalScrollViewInterop(
    state: ScrollViewInteropState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
    androidViewHolder: ViewParent? = null
) = scroll(
    state = state,
    isEnabled = enabled,
    reverseScrolling = reverseScrolling,
    flingBehavior = flingBehavior,
    isVertical = false,
    androidViewHolder = androidViewHolder
)

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.scroll(
    state: ScrollViewInteropState,
    reverseScrolling: Boolean,
    flingBehavior: FlingBehavior?,
    isEnabled: Boolean,
    isVertical: Boolean,
    androidViewHolder: ViewParent? = null
) = composed(
    factory = {
        val overscrollEffect = ScrollableDefaults.overscrollEffect()
        val coroutineScope = rememberCoroutineScope()

        val semantics = Modifier.semantics {
            val accessibilityScrollState = ScrollAxisRange(
                value = { state.value.ordinal.toFloat() },
                maxValue = { state.maxValue.ordinal.toFloat() },
                reverseScrolling = reverseScrolling
            )

            if (isVertical) {
                this.verticalScrollAxisRange = accessibilityScrollState
            } else {
                this.horizontalScrollAxisRange = accessibilityScrollState
            }

            if (isEnabled) {
                scrollBy(
                    action = { x: Float, y: Float ->
                        coroutineScope.launch {
                            if (isVertical) {
                                (state as ScrollableState).animateScrollBy(y)
                            } else {
                                (state as ScrollableState).animateScrollBy(x)
                            }
                        }
                        return@scrollBy true
                    }
                )
            }
        }
        val orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal
        val scrolling = Modifier
            .scrollable(
                orientation = orientation,
                reverseDirection = run {
                    // A finger moves with the content, not with the viewport. Therefore,
                    // always reverse once to have "natural" gesture that goes reversed to layout
                    var reverseDirection = !reverseScrolling
                    // But if rtl and horizontal, things move the other way around
                    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                    if (isRtl && !isVertical) {
                        reverseDirection = !reverseDirection
                    }
                    reverseDirection
                },
                enabled = isEnabled,
                interactionSource = state.internalInteractionSource,
                flingBehavior = flingBehavior,
                state = state,
                overscrollEffect = overscrollEffect
            )

        val selfConnection = rememberSelfNestedScrollConnection(state, isVertical)
        val descendantConnection = rememberDescendantNestedScrollConnection(state, isVertical)

        semantics
            .clipScrollableContainer(orientation)
            .overscroll(overscrollEffect)
            .conditionalThen(isEnabled) { nestedScroll(selfConnection) }
            .pointerInput(isEnabled, androidViewHolder) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        androidViewHolder?.requestDisallowInterceptTouchEvent(isEnabled)
                        waitForUpOrCancellation()
                    }
                }
            }
            .then(scrolling)
            .conditionalThen(isEnabled) { nestedScroll(descendantConnection) }
    },
    inspectorInfo = debugInspectorInfo {
        name = "scroll"
        properties["state"] = state
        properties["reverseScrolling"] = reverseScrolling
        properties["flingBehavior"] = flingBehavior
        properties["isScrollable"] = isEnabled
        properties["isVertical"] = isVertical
    }
)

@Composable
private fun rememberSelfNestedScrollConnection(
    state: ScrollViewInteropState,
    isVertical: Boolean
): NestedScrollConnection {
    return remember(state, isVertical) { selfNestedScrollConnection(state, isVertical) }
}

private fun selfNestedScrollConnection(
    state: ScrollViewInteropState,
    isVertical: Boolean
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val availableByAxis = if (isVertical) available.y else available.x
        if (availableByAxis > 0) state.value = MovementPosition.Start
        if (availableByAxis < 0) state.value = MovementPosition.End
        return super.onPostScroll(consumed, available, source)
    }
}

@Composable
private fun rememberDescendantNestedScrollConnection(
    state: ScrollViewInteropState,
    isVertical: Boolean
): NestedScrollConnection {
    return remember(state, isVertical) { childrenNestedScrollConnection(state, isVertical) }
}

private fun childrenNestedScrollConnection(
    state: ScrollViewInteropState,
    isVertical: Boolean
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val consumedByAxis = if (isVertical) consumed.y else consumed.x
        val availableByAxis = if (isVertical) available.y else available.x
        if (consumedByAxis != 0f && availableByAxis == 0f) state.value = MovementPosition.Center
        return super.onPostScroll(consumed, available, source)
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return super.onPostFling(consumed, available)
    }
}