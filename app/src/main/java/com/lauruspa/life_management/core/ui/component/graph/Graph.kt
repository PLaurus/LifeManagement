package com.lauruspa.life_management.core.ui.component.graph

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@Immutable
data class GraphLayoutInfo(
	val itemRectList: List<IntRect>,
	val graphContentSize: IntSize
) {
	companion object {
		@Stable
		internal val Zero: GraphLayoutInfo
			get() = GraphLayoutInfo(
				itemRectList = emptyList(),
				graphContentSize = IntSize.Zero
			)
	}
}

@Stable
class GraphState internal constructor(
	initialLayoutInfo: GraphLayoutInfo,
) {
	
	@Suppress("MemberVisibilityCanBePrivate")
	@Volatile
	var layoutInfo: GraphLayoutInfo = initialLayoutInfo
		internal set
	
	/**
	 * Осуществляет анимированное изменение зума до нового значения [zoom].
	 */
	suspend fun animateZoom(zoom: Float) {
		// TODO:
	}
	
	/**
	 * Осуществялет анимированное смещение центра видимой области графа максимально близко к центру элемента графа, выбранного по
	 * [index] этого элемента из items. Зум при этом изменяется таким образом, что видимая область вмещает только этот
	 * элемент.
	 */
	suspend fun animateMoveToItem(
		index: Int
	) {
		// TODO
	}
	
	/**
	 * Осуществялет анимированное смещение центра видимой области графа максимально близко к центру элементов графа, выбранных по
	 * [indexes] этих элементов из items. Зум при этом изменяется таким образом, что видимая область вмещает только эти
	 * элементы.
	 */
	suspend fun animateMoveToItems(
		vararg indexes: Int
	) {
		// TODO
	}
}

@Composable
fun rememberGraphState(
	initialLayoutInfo: GraphLayoutInfo = GraphLayoutInfo.Zero
): GraphState {
	return remember {
		GraphState(
			initialLayoutInfo = initialLayoutInfo
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
	SubcomposeLayout(
		modifier = modifier
			.clipToBounds()
			.verticalScroll(rememberScrollState())
			.padding(contentPadding)
	) { constraints ->
		val itemPaddingLeft = itemPadding.calculateLeftPadding(layoutDirection)
			.roundToPx()
		val itemPaddingTop = itemPadding.calculateTopPadding()
			.roundToPx()
		val itemPaddingRight = itemPadding.calculateRightPadding(layoutDirection)
			.roundToPx()
		val itemPaddingBottom = itemPadding.calculateBottomPadding()
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
			graphContentSize = graphRect.size
		)
		
		layout(graphRect.width, graphRect.height) {
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
		
		Row(Modifier.fillMaxWidth()) {
			Button(
				onClick = {
					coroutineScope.launch {
						graphState.animateZoom(zoom = 0.5f)
					}
				}
			) {
				Text(text = "Zoom 0.5")
			}
			Button(
				onClick = {
					coroutineScope.launch {
						graphState.animateZoom(zoom = 2f)
					}
				}
			) {
				Text(text = "Zoom 2")
			}
			Button(
				onClick = {
					coroutineScope.launch {
						graphState.animateMoveToItem(index = 3)
					}
				}
			) {
				Text(text = "To 4")
			}
			Button(
				onClick = {
					coroutineScope.launch {
						graphState.animateMoveToItems(55, 61, 57)
					}
				}
			) {
				Text(text = "To 55, 61, 57")
			}
		}
	}
}