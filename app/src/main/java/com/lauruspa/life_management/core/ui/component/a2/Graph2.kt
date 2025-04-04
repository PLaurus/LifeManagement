package com.lauruspa.life_management.core.ui.component.a2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Состояние графа, управляющее масштабом, смещением и позициями элементов.
 */
class GraphState<T>(
	initialScale: Float = 1f,
	initialOffset: Offset = Offset.Zero,
	val minScale: Float = 0.5f,
	val maxScale: Float = 2f
) {
	var scale by mutableStateOf(initialScale)
		private set
	
	var offset by mutableStateOf(initialOffset)
		private set
	
	var contentSize by mutableStateOf(Size.Zero)
	var viewportSize by mutableStateOf(Size.Zero)
	val itemPositions = mutableMapOf<T, IntRect>()
	
	private val scaleVelocity = Animatable(0f)
	private val offsetVelocity = Animatable(Offset.Zero, Offset.VectorConverter)
	
	fun updateScale(newScale: Float) {
		scale = newScale.coerceIn(minScale, maxScale)
	}
	
	fun updateOffset(newOffset: Offset) {
		offset = boundedTranslation(newOffset, contentSize, scale, viewportSize)
	}
	
	suspend fun animateScaleTo(targetScale: Float, animationSpec: AnimationSpec<Float> = tween(300)) {
		val startScale = scale
		val endScale = targetScale.coerceIn(minScale, maxScale)
		Animatable(startScale).animateTo(endScale, animationSpec) {
			scale = value
		}
	}
	
	/** Анимированно перемещает и масштабирует граф к элементу */
	suspend fun animateToItem(
		item: T,
		targetScale: Float = scale,
		scaleAnimationSpec: AnimationSpec<Float> = tween(300),
		offsetAnimationSpec: AnimationSpec<Offset> = tween(300)
	) {
		val itemRect = itemPositions[item] ?: return
		val itemCenter = Offset(itemRect.left + itemRect.width / 2f, itemRect.top + itemRect.height / 2f)
		val viewportCenter = Offset(
			x = viewportSize.width / 2f,
			y = viewportSize.height / 2f
		)
		val targetOffset = viewportCenter - (itemCenter * targetScale)
		val clampedTargetOffset = boundedTranslation(targetOffset, contentSize, targetScale, viewportSize)
		coroutineScope {
			launch { animateScaleTo(targetScale, scaleAnimationSpec) }
			launch {
				Animatable(offset, Offset.VectorConverter).animateTo(clampedTargetOffset, offsetAnimationSpec) {
					offset = value
				}
			}
		}
	}
	
	// New fling animation function
	suspend fun applyFling(velocity: Offset) {
		coroutineScope {
			launch {
				val scaleFling = scaleVelocity.animateDecay(
					initialVelocity = velocity.x / 1000f, // Adjust this factor as needed
					animationSpec = floatDecaySpec()
				)
				updateScale(scale + scaleFling.value)
			}
			launch {
				offsetVelocity.animateDecay(
					initialVelocity = velocity,
					animationSpec = offsetDecaySpec()
				) {
					updateOffset(offset + value)
				}
			}
		}
	}
	
	/** Ограничивает смещение, чтобы контент не выходил за пределы области просмотра */
	internal fun boundedTranslation(offset: Offset, contentSize: Size, scale: Float, viewportSize: Size): Offset {
		val scaledContentSize = contentSize * scale
		val maxX = if (scaledContentSize.width > viewportSize.width) {
			(scaledContentSize.width - viewportSize.width) / 2
		} else 0f
		val maxY = if (scaledContentSize.height > viewportSize.height) {
			(scaledContentSize.height - viewportSize.height) / 2
		} else 0f
		return Offset(
			offset.x.coerceIn(-maxX, maxX),
			offset.y.coerceIn(-maxY, maxY)
		)
	}
}

private fun floatDecaySpec() = exponentialDecay<Float>(frictionMultiplier = 0.1f)
private fun offsetDecaySpec() = exponentialDecay<Offset>(frictionMultiplier = 0.1f)

@Composable
fun <T> rememberGraphState(
	initialScale: Float = 1f,
	initialOffset: Offset = Offset.Zero,
	minScale: Float = 0.5f,
	maxScale: Float = 2f
): GraphState<T> {
	return remember {
		GraphState(
			initialScale = initialScale,
			initialOffset = initialOffset,
			minScale = minScale,
			maxScale = maxScale
		)
	}
}

/**
 * Компонент графа с поддержкой скроллинга, зума и программного управления.
 */
@Composable
fun <T> Graph(
	items: List<T>,
	itemColumnKey: (item: T) -> Int,
	linked: (item1: T, item2: T) -> Boolean,
	modifier: Modifier = Modifier,
	mainColumnKey: Int = 0,
	contentPadding: PaddingValues = PaddingValues(16.dp),
	itemPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
	sameColumnLinkPadding: Dp = 16.dp,
	sameColumnLinkSideOnTheRight: (item1: T, item2: T) -> Boolean = { _, _ -> true },
	graphState: GraphState<T> = remember { GraphState() },
	state: GraphState<T> = rememberGraphState(),
	item: @Composable (item: T) -> Unit
) {
	val coroutineScope = rememberCoroutineScope()
	
	val transformState = rememberTransformableState { zoomChange, panChange, rotationChange ->
		val newScale = (state.scale * zoomChange).coerceIn(state.minScale, state.maxScale)
		val scaledPanChange = panChange / state.scale // Adjust offset based on current scale
		var newOffset = state.offset + scaledPanChange
		newOffset = state.boundedTranslation(newOffset, state.contentSize, newScale, state.viewportSize)
		state.updateScale(newScale)
		state.updateOffset(newOffset)
	}
	
	BoxWithConstraints(
		modifier = modifier
			.clipToBounds()
			.transformable(
				state = transformState,
				onTransformationEnd = { velocity ->
					coroutineScope.launch {
						state.applyFling(velocity)
					}
				}
			)
	) {
		state.viewportSize = with(LocalDensity.current) { Size(maxWidth.toPx(), maxHeight.toPx()) }
		
		SubcomposeLayout(
			modifier = Modifier
				.padding(contentPadding)
				.graphicsLayer(
					scaleX = state.scale,
					scaleY = state.scale,
					translationX = state.offset.x,
					translationY = state.offset.y
				)
		) { constraints ->
			val itemPaddingLeft = itemPadding.calculateLeftPadding(layoutDirection).roundToPx()
			val itemPaddingTop = itemPadding.calculateTopPadding().roundToPx()
			val itemPaddingRight = itemPadding.calculateRightPadding(layoutDirection).roundToPx()
			val itemPaddingBottom = itemPadding.calculateBottomPadding().roundToPx()
			
			val itemMeasurables = subcompose(GraphSlot.ITEMS) {
				items.forEach { item(it) }
			}
			
			val placeables = itemMeasurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
			
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
			
			val links = nodes.flatMap { node ->
				nodes.filter { otherNode -> linked(node.item, otherNode.item) }.map { linkedNode ->
					// Логика расчета связей (без изменений)
					val lineStart: IntOffset
					val lineCenter: IntOffset
					val lineEnd: IntOffset
					if (node.position.x == linkedNode.position.x) {
						val linkSideIsOnTheRight = sameColumnLinkSideOnTheRight(node.item, linkedNode.item)
						if (linkSideIsOnTheRight) {
							lineStart = IntOffset(node.position.x + node.placeable.width, node.position.y + node.placeable.height / 2)
							lineEnd = IntOffset(linkedNode.position.x + linkedNode.placeable.width, linkedNode.position.y + linkedNode.placeable.height / 2)
							lineCenter = IntOffset(lineStart.x + sameColumnLinkPadding.roundToPx(), lineStart.y + (lineEnd.y - lineStart.y) / 2)
						} else {
							lineStart = IntOffset(node.position.x, node.position.y + node.placeable.height / 2)
							lineEnd = IntOffset(linkedNode.position.x, linkedNode.position.y + linkedNode.placeable.height / 2)
							lineCenter = IntOffset(lineStart.x - sameColumnLinkPadding.roundToPx(), lineStart.y + (lineEnd.y - lineStart.y) / 2)
						}
					} else {
						if (node.position.x < linkedNode.position.x) {
							lineStart = IntOffset(node.position.x + node.placeable.width, node.position.y + node.placeable.height / 2)
							lineEnd = IntOffset(linkedNode.position.x, linkedNode.position.y + linkedNode.placeable.height / 2)
						} else {
							lineStart = IntOffset(node.position.x, node.position.y + node.placeable.height / 2)
							lineEnd = IntOffset(linkedNode.position.x + linkedNode.placeable.width, linkedNode.position.y + linkedNode.placeable.height / 2)
						}
						lineCenter = IntOffset(lineStart.x + (lineEnd.x - lineStart.x) / 2, lineStart.y + (lineEnd.y - lineStart.y) / 2)
					}
					Link(start = lineStart, center = lineCenter, end = lineEnd)
				}
			}
			
			val graphRect = calculateGraphRect(nodes, links, constraints, itemPaddingLeft, itemPaddingTop, itemPaddingRight, itemPaddingBottom)
			val graphOffset = -graphRect.topLeft
			val linesComponentPosition = linksRect(nodes, links)?.topLeft?.plus(graphOffset) ?: IntOffset.Zero
			
			val localLinks = if (links.isNotEmpty()) {
				val linksRectTopLeft = linksRect(nodes, links)?.topLeft ?: IntOffset.Zero
				links.map { it.copy(start = it.start - linksRectTopLeft, center = it.center - linksRectTopLeft, end = it.end - linksRectTopLeft) }
			} else emptyList()
			
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
			}.first().measure(constraints.copy(minWidth = 0, minHeight = 0, maxWidth = linksRect(nodes, links)?.width ?: 0, maxHeight = linksRect(nodes, links)?.height ?: 0))
			
			// Сохранение размеров контента и позиций элементов
			graphState.contentSize = Size(graphRect.width.toFloat(), graphRect.height.toFloat())
			nodes.forEach { node ->
				graphState.itemPositions[node.item] = IntRect(node.position, IntSize(node.placeable.width, node.placeable.height))
			}
			
			layout(graphRect.width, graphRect.height) {
				linesPlaceable.place(linesComponentPosition.x, linesComponentPosition.y)
				nodes.forEach { node ->
					node.placeable.place(node.position.x + graphOffset.x, node.position.y + graphOffset.y)
				}
			}
		}
	}
}

// Вспомогательные функции (без изменений, но вынесены для читаемости)
private fun <T> calculateGraphRect(
	nodes: List<Node<T>>,
	links: List<Link>,
	constraints: Constraints,
	itemPaddingLeft: Int,
	itemPaddingTop: Int,
	itemPaddingRight: Int,
	itemPaddingBottom: Int
): IntRect {
	val nodesRect = if (nodes.isNotEmpty()) {
		IntRect(
			topLeft = IntOffset(
				nodes.minOf { it.position.x } - itemPaddingLeft,
				nodes.minOf { it.position.y } - itemPaddingTop
			),
			bottomRight = IntOffset(
				nodes.maxOf { it.position.x + it.placeable.width } + itemPaddingRight,
				nodes.maxOf { it.position.y + it.placeable.height } + itemPaddingBottom
			)
		)
	} else null
	
	val linksRect = linksRect(nodes, links)
	return when {
		nodesRect != null && linksRect != null -> IntRect(
			topLeft = IntOffset(min(nodesRect.left, linksRect.left), min(nodesRect.top, linksRect.top)),
			bottomRight = IntOffset(max(nodesRect.right, linksRect.right), max(nodesRect.bottom, linksRect.bottom))
		)
		nodesRect != null -> nodesRect
		linksRect != null -> linksRect
		else -> IntRect(IntOffset.Zero, IntSize(constraints.minWidth, constraints.minHeight))
	}
}

private fun <T> linksRect(nodes: List<Node<T>>, links: List<Link>): IntRect? {
	return if (links.isNotEmpty()) {
		val linksPoints = links.flatMap { listOf(it.start, it.center, it.end) }
		IntRect(
			topLeft = IntOffset(linksPoints.minOf { it.x }, linksPoints.minOf { it.y }),
			bottomRight = IntOffset(linksPoints.maxOf { it.x }, linksPoints.maxOf { it.y })
		)
	} else null
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
					item1 == 66 && item2 == 52 ||
					
					false
		},
		
		modifier = Modifier
			.fillMaxSize()
			.background(Color.LightGray),
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
}