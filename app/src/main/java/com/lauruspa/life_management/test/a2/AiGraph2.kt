package com.lauruspa.life_management.test.a2

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
import kotlin.math.min

@Composable
fun <T> AiGraph2(
	items: List<T>,
	itemColumn: (item: T) -> Int,
	linked: (item1: T, item2: T) -> Boolean,
	modifier: Modifier = Modifier,
	mainColumn: Int = 0,
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
		val itemPaddingLeft = itemPadding.calculateLeftPadding(layoutDirection)
			.roundToPx()
		val itemPaddingTop = itemPadding.calculateTopPadding()
			.roundToPx()
		val itemPaddingRight = itemPadding.calculateRightPadding(layoutDirection)
			.roundToPx()
		val itemPaddingBottom = itemPadding.calculateBottomPadding()
			.roundToPx()
		val itemPaddingHorizontal = itemPaddingLeft + itemPaddingRight
		val itemPaddingVertical = itemPaddingTop + itemPaddingBottom
		
		val contentPaddingLeft = contentPadding.calculateLeftPadding(layoutDirection)
			.roundToPx()
		val contentPaddingTop = contentPadding.calculateTopPadding()
			.roundToPx()
		val contentPaddingRight = contentPadding.calculateRightPadding(layoutDirection)
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
			itemColumn = itemColumn,
			mainColumn = mainColumn,
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

private fun <T> calcNodes(
	items: List<T>,
	placeables: List<Placeable>,
	itemColumn: (item: T) -> Int,
	mainColumn: Int,
	linked: (item1: T, item2: T) -> Boolean,
	itemPaddingLeft: Int,
	itemPaddingTop: Int,
	itemPaddingRight: Int,
	itemPaddingBottom: Int
): List<Node<T>> {
	val result = mutableListOf<Node<T>>()
	val columns = items.groupBy { item ->
		itemColumn(item)
	}
	
	val columnIndexes = columns.keys.sortedDescending()
	val xColumnPositions = columns
		.mapValues { (_, items) ->
			val maxColumnItemWidth = items.maxOfOrNull { item ->
				val itemIndex = items.indexOf(item)
				val placeable = placeables[itemIndex]
				placeable.width
			} ?: 0
			itemPaddingLeft + maxColumnItemWidth + itemPaddingRight
		}
		.toSortedMap(reverseOrder())
		.map(object : (Map.Entry<Int, Int>) -> Pair<Int, Int> {
			var x = 0
			override fun invoke(
				columnIndexToWidth: Map.Entry<Int, Int>
			): Pair<Int, Int> {
				x -= columnIndexToWidth.value
				return columnIndexToWidth.key to x
			}
		})
		.toMap()
		.toSortedMap()
	
	val yPositions = mutableMapOf<Int, Int>()
	val maxYLimits = mutableMapOf<Int, Int>()
	var currentRow = columns.maxOfOrNull { column -> column.value.size - 1 }
		?.takeIf { size -> size >= 0 } ?: -1
	
	while (currentRow >= 0) {
		for (columnIndex in columnIndexes) {
			val item = columns[columnIndex]?.getOrNull(currentRow) ?: continue
			val itemIndex = items.indexOf(item)
			val placeable = placeables[itemIndex]
			val rightColumnIndexes = columnIndexes.filter { otherColumnIndex -> otherColumnIndex > columnIndex }
			val rightColumns = columns.filter { column -> rightColumnIndexes.contains(column.key) }
			val itemHasChildren = rightColumns.any { rightColumn ->
				val rightColumnItems = rightColumn.value
				rightColumnItems.any { rightColumnItem -> linked(item, rightColumnItem) }
			}
			
			val leftColumnIndexes = columnIndexes.filter { otherColumnIndex -> otherColumnIndex < columnIndex }
			val leftColumns = columns.filter { column -> leftColumnIndexes.contains(column.key) }
			
			val itemHeight = itemPaddingBottom + placeable.height + itemPaddingTop
			val maxYLimit = maxYLimits[columnIndex] ?: 0
			val nodeY = yPositions[columnIndex] ?: -itemHeight
			leftColumnIndexes.forEach { leftColumnIndex ->
				val currentLeftColumnItemPosY = yPositions[leftColumnIndex] ?: 0
				yPositions[leftColumnIndex] = min(currentLeftColumnItemPosY, nodeY)
			}
			result += Node(
				item = item,
				placeable = placeable,
				position = IntOffset(
					x = xColumnPositions[columnIndex] ?: continue,
					y = nodeY
				)
			)
			yPositions[columnIndex] = nodeY
		}
		currentRow--
	}
	
	val minX = xColumnPositions.values.minOrNull() ?: 0
	val maxX = xColumnPositions.values.maxOrNull() ?: minX
	val columnsWidth = maxX - minX
	val minY = result.minOfOrNull { node -> node.position.y } ?: 0
	val maxY = result.maxOfOrNull { node -> node.position.y } ?: minY
	val columnsHeight = maxY - minY
	
	return result.map { node ->
		node.copy(
			position = IntOffset(
				x = node.position.x + 4000,
				y = node.position.y + 4000
			)
		)
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
	heightDp = 1600
)
@Composable
private fun AiGraphPreview2() {
	fun itemLevel(item: Int): Int {
		return if (item == 50) {
			0
		} else if (item < 50) {
			(item % 5) - 5
		} else {
			(item % 5) + 1
		}
	}
	
	AiGraph2(
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
			item1 == 10 && item2 == 11 ||
					item1 == 11 && item2 == 12 ||
					item1 == 12 && item2 == 13 ||
					item1 == 13 && item2 == 14 ||
					item1 == 14 && item2 == 50 ||
					item1 == 50 && item2 == 66 ||
					
					item1 == 1 && item2 == 2 ||
					
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
//					2 -> "2\n2"
//					10 -> "10\n10\n10\n10\n10\n10\n10\n10\n10"
//					41 -> "41\n41"
//					65 -> "65\n65"
					else -> item.toString()
				},
				modifier = Modifier.padding(16.dp),
				color = Color.Black
			)
		}
	}
}