package com.lauruspa.life_management.test.a3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset

@Composable
fun <T> AiGraph(
	items: List<T>,
	itemColumn: (item: T) -> Int,
	linked: (item1: T, item2: T) -> Boolean,
	modifier: Modifier = Modifier,
	mainColumn: Int = 0,
	contentPadding: PaddingValues = PaddingValues(horizontal = 0.dp),
	itemPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
	sameColumnLinkPadding: Dp = 16.dp,
	sameColumnLinkSideOnTheRight: (item1: T, item2: T) -> Boolean = { _, _ -> true },
	item: @Composable (item: T) -> Unit
) {
	SubcomposeLayout(modifier = modifier) { constraints ->
		val itemPaddingLeft = itemPadding.calculateLeftPadding(layoutDirection).roundToPx()
		val itemPaddingTop = itemPadding.calculateTopPadding().roundToPx()
		val itemPaddingRight = itemPadding.calculateRightPadding(layoutDirection).roundToPx()
		val itemPaddingBottom = itemPadding.calculateBottomPadding().roundToPx()
		val contentPaddingLeft = contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
		val contentPaddingTop = contentPadding.calculateTopPadding().roundToPx()
		val sameColumnLinkPaddingPx = sameColumnLinkPadding.roundToPx()
		
		val itemMeasurables = subcompose(GraphSlot.ITEMS) {
			items.forEach { item -> item(item) }
		}
		
		val placeables = itemMeasurables.map { measurable ->
			measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
		}
		
		val columnMap = items.zip(placeables).groupBy { itemColumn(it.first) }
		val sortedColumns = columnMap.keys.sorted()
		val columnWidths = sortedColumns.associateWith { column ->
			columnMap[column]!!.maxOf { it.second.width }
		}
		val horizontalSpacingPx = 32.dp.roundToPx()
		val verticalSpacingPx = (itemPadding.calculateTopPadding() + itemPadding.calculateBottomPadding()).roundToPx()
		
		val columnX = mutableMapOf<Int, Int>()
		var currentX = contentPaddingLeft
		for (column in sortedColumns) {
			columnX[column] = currentX
			currentX += columnWidths[column]!! + horizontalSpacingPx
		}
		
		// Расчет позиций узлов
		val nodes = calculateNodePositions(
			items = items,
			placeables = placeables,
			itemColumn = itemColumn,
			linked = linked,
			columnX = columnX,
			contentPaddingTop = contentPaddingTop,
			verticalSpacingPx = verticalSpacingPx
		)
		
		// Определение связей (стрелок)
		val links = nodes.flatMap { node ->
			val linkedNodes = nodes.filter { otherNode -> linked(node.item, otherNode.item) }
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
							x = lineStart.x + sameColumnLinkPaddingPx,
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
							x = lineStart.x - sameColumnLinkPaddingPx,
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
		
		// Отрисовка линий
		val linesPlaceable = subcompose(GraphSlot.LINES) {
			Canvas(Modifier.size(constraints.maxWidth.toDp(), constraints.maxHeight.toDp())) {
				links.forEach { link ->
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
		}.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
		
		// Размещение элементов и линий
		layout(constraints.maxWidth, constraints.maxHeight) {
			linesPlaceable.place(0, 0)
			nodes.forEach { node ->
				node.placeable.place(node.position.x, node.position.y)
			}
		}
	}
}

private fun <T> calculateNodePositions(
	items: List<T>,
	placeables: List<Placeable>,
	itemColumn: (item: T) -> Int,
	linked: (item1: T, item2: T) -> Boolean,
	columnX: Map<Int, Int>,
	contentPaddingTop: Int,
	verticalSpacingPx: Int
): MutableList<Node<T>> {
	val nodes: MutableList<Node<T>> = mutableListOf()
	val columnMap = items.zip(placeables).groupBy { itemColumn(it.first) }
	val sortedColumns = columnMap.keys.sorted()
	val nodeMap = mutableMapOf<T, Node<T>>()
	
	// Размещение узлов по колонкам
	for (column in sortedColumns) {
		val columnItems = columnMap[column]!!
		val sortedItems = columnItems.sortedBy { items.indexOf(it.first) }
		var currentY = contentPaddingTop
		
		for ((item, placeable) in sortedItems) {
			// Находим родителей в левых колонках
			val parents = items.filter { parent ->
				itemColumn(parent) < column && linked(parent, item)
			}
			val parentNodes = parents.mapNotNull { nodeMap[it] }
			
			// Определяем минимальную Y-координату родителей
			val minParentY = if (parentNodes.isNotEmpty()) {
				parentNodes.minOf { it.position.y }
			} else {
				contentPaddingTop
			}
			
			// Размещаем узел не выше минимальной Y-координаты родителей
			currentY = maxOf(currentY, minParentY)
			val position = IntOffset(columnX[column]!!, currentY)
			val node = Node(item, placeable, position)
			nodes.add(node)
			nodeMap[item] = node
			currentY += placeable.height + verticalSpacingPx
		}
	}
	
	// Корректировка позиций для поддеревьев
	val descendantsMap = mutableMapOf<T, List<T>>()
	for (item in items) {
		descendantsMap[item] = items.filter { otherItem ->
			itemColumn(otherItem) > itemColumn(item) && linked(item, otherItem)
		}
	}
	val allColumns = columnX.keys.sorted()
	val sortedNodesByY = nodes.sortedBy { it.position.y }
	for (node in sortedNodesByY) {
		val item = node.item
		val descendants = descendantsMap[item] ?: continue
		if (descendants.isEmpty()) continue
		val maxDescendantY = descendants.maxOf { descendant ->
			val descendantNode = nodeMap[descendant]!!
			descendantNode.position.y + descendantNode.placeable.height
		}
		val columnA = itemColumn(item)
		val yA = node.position.y
		for (col in allColumns.filter { it <= columnA }) {
			val columnNodes = nodes.filter { itemColumn(it.item) == col }
				.sortedBy { it.position.y }
			val index = columnNodes.indexOfFirst { it.position.y > yA }
			if (index != -1) {
				val firstBelow = columnNodes[index]
				val firstBelowY = firstBelow.position.y
				if (firstBelowY < maxDescendantY + verticalSpacingPx) {
					val delta = maxDescendantY + verticalSpacingPx - firstBelowY
					for (i in index until columnNodes.size) {
						val nodeToShift = columnNodes[i]
						nodeToShift.position = IntOffset(
							nodeToShift.position.x,
							nodeToShift.position.y + delta
						)
					}
				}
			}
		}
	}
	return nodes
}

// Класс Node с изменяемой позицией
private data class Node<T>(
	val item: T,
	val placeable: Placeable,
	var position: IntOffset
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
	val lastLineSegmentStart = Offset(x = center.x, y = end.y)
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
	heightDp = 1400
)
@Composable
private fun AiGraphPreview() {
	fun itemLevel(item: Int): Int {
		return if (item == 50) {
			0
		} else if (item < 50) {
			(item % 5) - 5
		} else {
			(item % 5) + 1
		}
	}
	
	AiGraph(
		items = remember { (0..100).toList() },
		itemColumn = { item ->
			if (item == 50) {
				0
			} else {
				val columnIndex = itemLevel(item)
				if (columnIndex == -1 || columnIndex == 1) 0 else columnIndex
			}
		},
		linked = { item1, item2 ->
			item1 == 0 && item2 == 1 ||
			item1 == 1 && item2 == 2||
//					item1 == 2 && item2 == 23 ||
//					item1 == 8 && item2 == 9 ||
//					item1 == 7 && item2 == 8 ||
//					item1 == 7 && item2 == 13 ||
//					item1 == 7 && item2 == 18 ||
//					item1 == 8 && item2 == 9 ||
//					item1 == 9 && item2 == 49 ||
//					item1 == 49 && item2 == 50 ||
//					item1 == 50 && item2 == 56 ||
//					item1 == 56 && item2 == 57 ||
//					item1 == 56 && item2 == 62 ||
//					item1 == 23 && item2 == 19 ||
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
			colors = CardDefaults.cardColors(containerColor = Color.White),
			border = BorderStroke(width = 1.dp, color = Color.Gray)
		) {
			Text(
				text = when (item) {
					2 -> "2\n2"
					10 -> "10\n10\n10\n10\n10\n10\n10\n10\n10"
					41 -> "41\n41"
					65 -> "65\n65"
					else -> item.toString()
				},
				modifier = Modifier.padding(16.dp),
				color = Color.Black
			)
		}
	}
}