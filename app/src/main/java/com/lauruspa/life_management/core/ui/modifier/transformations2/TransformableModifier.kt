package com.lauruspa.life_management.core.ui.modifier.transformations2

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

fun Modifier.transformable(
	state: TransformableState
) = this then TransformableElement(
	state = state
)

class TransformableState(
	@FloatRange(from = 1.0) val maxScale: Float = 5f,
	private val velocityDecay: DecayAnimationSpec<Float> = exponentialDecay(),
	@FloatRange(from = 1.0) private val initialScale: Float = 1f
) {
	init {
		require(maxScale >= 1.0f) { "maxScale must be at least 1.0." }
		require(initialScale >= 1.0f) { "initialScale must be at least 1.0." }
	}
	
	private var _scale = Animatable(initialScale).apply {
		updateBounds(1f, maxScale)
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
}

private data class TransformableElement(
	val state: TransformableState
) : ModifierNodeElement<TransformableNode>() {
	override fun create(): TransformableNode {
		return TransformableNode(
			state = state
		)
	}
	
	override fun update(node: TransformableNode) {
		if (node.state != state) node.state = state
	}
	
	override fun InspectorInfo.inspectableProperties() {
		name = "transformations"
		properties["state"] = state
	}
}

private class TransformableNode(
	var state: TransformableState,
) : DelegatingNode(), LayoutModifierNode {
	override fun MeasureScope.measure(
		measurable: Measurable,
		constraints: Constraints
	): MeasureResult {
		val childConstraints = constraints.copy(
			maxWidth = Constraints.Infinity,
			maxHeight = Constraints.Infinity
		)
		
		val placeable = measurable.measure(childConstraints)
		
		val contentSize = IntSize(
			width = placeable.width,
			height = placeable.height
		)
		
		val viewportSize = IntSize(
			width = contentSize.width.coerceAtMost(constraints.maxWidth),
			height = contentSize.height.coerceAtMost(constraints.maxHeight)
		)
		
		return layout(viewportSize.width, viewportSize.height) {
			placeable.placeWithLayer(x = 0, y = 0) {
				scaleX = state.scale
				scaleY = state.scale
				translationX = state.offsetX
				translationY = state.offsetY
			}
		}
	}
}

/**
 * Creates a [TransformableState] that is remembered across compositions.
 *
 * @param maxScale The maximum scale of the content.
 * @param velocityDecay The decay animation spec for fling behaviour.
 * @param initialScale The initial scale of the content.
 */
@Composable
fun rememberTransformableState(
	@FloatRange(from = 1.0) maxScale: Float = 5f,
	velocityDecay: DecayAnimationSpec<Float> = exponentialDecay(),
	@FloatRange(from = 1.0) initialScale: Float = 1f,
): TransformableState = remember {
	TransformableState(
		maxScale = maxScale,
		velocityDecay = velocityDecay,
		initialScale = initialScale
	)
}

@Composable
@Preview
private fun TransformablePreview() {
	Box(
		modifier = Modifier.transformable(
			state = rememberTransformableState()
		)
	) {
		Column {
			repeat(20) { i ->
				Row(Modifier.border(1.dp, Color.Black)) {
					repeat(10) { j ->
						Text(
							text = "{$i,$j}",
							modifier = Modifier
								.background(
									color = when (j % 3) {
										0 -> Color.Blue
										1 -> Color.Green
										2 -> Color.Red
										else -> Color.Yellow
									}
								)
								.size(50.dp, 50.dp),
							textAlign = TextAlign.Center
						)
					}
				}
			}
		}
	}
}