package com.lauruspa.life_management.test

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
	contentPadding: PaddingValues = PaddingValues(horizontal = 0.dp),
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
	) { constraints ->
		// Измеряем все элементы
		val itemMeasurables = subcompose(GraphSlot.ITEMS) {
			items.forEach { item -> item(item) }
		}
		
		// Группируем элементы по колонкам
		val columns = items.groupBy { itemColumn(it) }
			.toSortedMap()
			.values
			.toList()
		
		// Рассчитываем позиции для каждой колонки
		var xPos = contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
		val columnWidths = mutableListOf<Int>()
		val nodePositions = mutableListOf<GraphNode<T>>()
		
		columns.forEach { columnItems ->
			var yPos = contentPadding.calculateTopPadding().roundToPx()
			val columnNodes = mutableListOf<Placeable>()
			
			// Рассчитываем доступную ширину для колонки с учетом общего padding
			val columnWidth = (constraints.maxWidth
					- contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
					- contentPadding.calculateRightPadding(layoutDirection).roundToPx()
					) / columns.size
			
			// Горизонтальные отступы элемента (left + right)
			val itemHorizontalPadding =
				itemPadding.calculateLeftPadding(layoutDirection).roundToPx() +
						itemPadding.calculateRightPadding(layoutDirection).roundToPx()
			
			// Измеряем элементы с учетом их внутренних отступов
			columnItems.forEach { item ->
				val measurable = itemMeasurables[items.indexOf(item)]
				val placeable = measurable.measure(
					constraints.copy(
						maxWidth = columnWidth - itemHorizontalPadding, // Учет горизонтальных отступов
						minWidth = 0,
						minHeight = 0
					)
				)
				columnNodes.add(placeable)
			}
			
			// Ширина колонки = макс. ширина элемента + его отступы
			val maxColumnWidth = columnNodes.maxOf { it.width } + itemHorizontalPadding
			columnWidths.add(maxColumnWidth)
			
			// Располагаем элементы с отступами
			columnItems.forEachIndexed { index, item ->
				val placeable = columnNodes[index]
				val padding = itemPadding
				yPos += padding.calculateTopPadding().roundToPx()
				
				// Добавляем горизонтальный отступ слева к позиции
				val xItemPos = xPos + padding.calculateLeftPadding(layoutDirection).roundToPx()
				
				nodePositions.add(
					GraphNode(
						info = GraphItem(item, emptyList(), itemColumn(item)),
						placeable = placeable,
						position = IntOffset(xItemPos, yPos)
					)
				)
				
				yPos += placeable.height + padding.calculateBottomPadding().roundToPx()
			}
			
			xPos += maxColumnWidth + contentPadding.calculateRightPadding(layoutDirection)
				.roundToPx()
		}
		
		val nodes = nodePositions
		
		val links = nodes.map { node ->
			val linkedNodes = nodes
				.filter { otherNode -> linked(node.info.data, otherNode.info.data) }
			
			linkedNodes.map { linkedNode ->
				val lineStart: IntOffset
				val lineCenter: IntOffset
				val lineEnd: IntOffset
				
				// Linked node is on the same X
				if (node.position.x == linkedNode.position.x) {
					val linkSideIsOnTheRight = sameColumnLinkSideOnTheRight(
						node.info.data,
						linkedNode.info.data
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
		}.flatten()
		
		val linesPlaceable = subcompose(GraphSlot.LINES) {
			Canvas(
				Modifier.size(
					width = constraints.maxWidth.toDp(),
					height = constraints.maxHeight.toDp()
				)
			) {
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
		
		layout(constraints.maxWidth, constraints.maxHeight) {
			linesPlaceable.place(0, 0)
			
			nodes.forEach { node ->
				node.placeable.place(node.position)
			}
		}
	}
}

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

private enum class GraphSlot {
	ITEMS,
	LINES,
	DEBUG
}

private data class GraphItem<T>(
	val data: T,
	val children: List<GraphItem<T>>,
	val columnIndex: Int
)

private data class GraphNode<T>(
	val info: GraphItem<T>,
	val placeable: Placeable,
	val position: IntOffset
)

private data class Link(
	val start: IntOffset,
	val center: IntOffset,
	val end: IntOffset
)

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
		items = remember {
			(0..100).toList()
		},
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
					
					item1 == 1 && item2 == 2 ||
					item1 == 1 && item2 == 7 ||
					item1 == 1 && item2 == 12 ||
					
					item1 == 2 && item2 == 3 ||
					item1 == 12 && item2 == 13 ||
					
					item1 == 3 && item2 == 4 ||
					item1 == 8 && item2 == 9 ||
					item1 == 13 && item2 == 14 ||
					
					item1 == 4 && item2 == 50 ||
					item1 == 9 && item2 == 50 ||
					item1 == 14 && item2 == 50 ||
					
					item1 == 50 && item2 == 51 ||
					item1 == 50 && item2 == 56 ||
					
					item1 == 51 && item2 == 52 ||
					
					item1 == 52 && item2 == 53 ||
					item1 == 9 && item2 == 18 ||
					
					item1 == 53 && item2 == 54
		},
		sameColumnLinkSideOnTheRight = { item1, item2 ->
			val columnIndex = itemLevel(item1)
			columnIndex < 0
		},
		modifier = Modifier
			.fillMaxSize()
			.background(Color.LightGray)
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