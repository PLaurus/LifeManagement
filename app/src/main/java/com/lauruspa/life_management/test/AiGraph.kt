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
		val itemPaddingLeft = itemPadding.calculateLeftPadding(layoutDirection).roundToPx()
		val itemPaddingTop = itemPadding.calculateTopPadding().roundToPx()
		val itemPaddingRight = itemPadding.calculateRightPadding(layoutDirection).roundToPx()
		val itemPaddingBottom = itemPadding.calculateBottomPadding().roundToPx()
		val itemPaddingHorizontal = itemPaddingLeft + itemPaddingRight
		val itemPaddingVertical = itemPaddingTop + itemPaddingBottom
		
		val contentPaddingLeft = contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
		val contentPaddingTop = contentPadding.calculateTopPadding().roundToPx()
		val contentPaddingRight = contentPadding.calculateRightPadding(layoutDirection).roundToPx()
		val contentPaddingBottom = contentPadding.calculateBottomPadding().roundToPx()
		val contentPaddingHorizontal = contentPaddingLeft + contentPaddingRight
		val contentPaddingVertical = contentPaddingTop + contentPaddingBottom
		
		val itemMeasurables = subcompose(GraphSlot.ITEMS) {
			items.forEach { item -> item(item) }
		}
		
		val placeables = itemMeasurables.map { measurable ->
			measurable.measure(
				constraints.copy(
					minWidth = 0,
					minHeight = 0
				)
			)
		}
		
		val columns = items.groupBy { item -> itemColumn(item) }
		val itemToPlaceableMap = items.mapIndexed { index, item -> item to placeables[index] }
			.toMap()
		val columnIndexToWidth = columns.map { (columnIndex, columnItems) ->
			columnIndex to (columnItems.maxOfOrNull { item ->
				itemToPlaceableMap[item]?.width ?: 0
			} ?: 0)
		}.toMap()
		
		val itemPositions = mutableMapOf<T, IntOffset>()
		var itemX = 0
		// Собираем информацию о родителях для каждого элемента
		// Создаем карту родителей для каждого элемента
		val parentsMap = items.associateWith { item ->
			items.filter { parent -> linked(parent, item) }
		}
		
		columns.toSortedMap().forEach { (currentColumnIndex, columnItems) ->
			val columnWidth = columnIndexToWidth[currentColumnIndex] ?: 0
			itemX += itemPaddingLeft
			
			// Сортируем элементы: сначала те, у которых есть родители в предыдущих колонках
			val sortedItems = columnItems.sortedBy { item ->
				val hasParentInPreviousColumn = parentsMap[item]?.any { parent ->
					itemColumn(parent) < currentColumnIndex
				} ?: false
				if (hasParentInPreviousColumn) 0 else 1
			}
			
			var itemY = 0
			sortedItems.forEach { item ->
				// Получаем всех родителей из предыдущих колонок
				val parentsFromPreviousColumns = parentsMap[item]
					?.filter { parent -> itemColumn(parent) < currentColumnIndex }
					.orEmpty()
				
				// Если это первый элемент в колонке и есть родители
				val isFirstWithParent = sortedItems.indexOf(item) == 0 && parentsFromPreviousColumns.isNotEmpty()
				
				// Y-координата родителя (если есть)
				val parentY = parentsFromPreviousColumns.maxOfOrNull { parent ->
					itemPositions[parent]?.y ?: 0
				} ?: 0
				
				// Определяем стартовую Y-координату
				val startY = when {
					isFirstWithParent -> parentY // Привязка к родителю
					else -> maxOf(itemY, parentY) // Максимум между текущей позицией и родителем
				}
				
				// Фиксируем позицию элемента
				itemPositions[item] = IntOffset(x = itemX, y = startY)
				
				// Обновляем Y для следующих элементов
				itemY = startY + (itemToPlaceableMap[item]?.height ?: 0) + itemPaddingVertical
			}
			
			itemX += columnWidth + itemPaddingRight
		}
		
		val nodes: List<Node<T>> = items.mapNotNull { item ->
			Node(
				item = item,
				placeable = itemToPlaceableMap[item] ?: return@mapNotNull null,
				position = itemPositions[item] ?: return@mapNotNull null
			)
		}
		
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
		}.first()
			.measure(constraints.copy(minWidth = 0, minHeight = 0))
		
		layout(constraints.maxWidth, constraints.maxHeight) {
			linesPlaceable.place(0, 0)
			nodes.forEach { node ->
				node.placeable.place(node.position.x, node.position.y)
			}
		}
	}
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
			item1 == 15 && item2 == 26 ||
			item1 == 15 && item2 == 1 ||
			item1 == 26 && item2 == 22 ||
			item1 == 26 && item2 == 2 ||
			item1 == 7 && item2 == 3 ||
			item1 == 7 && item2 == 8 ||
			item1 == 8 && item2 == 49 ||
			item1 == 49 && item2 == 50 ||
			item1 == 50 && item2 == 55 ||
			item1 == 55 && item2 == 61 ||
			item1 == 55 && item2 == 66 ||
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