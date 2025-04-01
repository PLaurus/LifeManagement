package com.lauruspa.life_management.test

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

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
	item: @Composable (item: T) -> Unit
) {
	SubcomposeLayout(
		modifier = modifier
	) { constraints ->
		val itemMeasurables = subcompose(GraphSlot.ITEMS) {
			items.forEach { item -> item(item) }
		}
		
		val columns = items.groupBy { itemColumn(it) }
			.toSortedMap()
			.values
			.toList()
		
		val columnIndices = columns.map { col -> itemColumn(col.first()) }
		
		var xPos = contentPadding.calculateLeftPadding(layoutDirection)
			.roundToPx()
		val columnWidths = mutableListOf<Int>()
		val nodePositions = mutableListOf<GraphNode<T>>()
		
		columns.forEachIndexed { i, columnItems ->
			val columnIndexCurrent = columnIndices[i]
			var yPos = contentPadding.calculateTopPadding()
				.roundToPx()
			val columnNodes = mutableListOf<Placeable>()
			
			val columnWidth = (constraints.maxWidth
					- contentPadding.calculateLeftPadding(layoutDirection)
				.roundToPx()
					- contentPadding.calculateRightPadding(layoutDirection)
				.roundToPx()
					) / columns.size
			
			val itemHorizontalPadding =
				itemPadding.calculateLeftPadding(layoutDirection)
					.roundToPx() +
						itemPadding.calculateRightPadding(layoutDirection)
							.roundToPx()
			
			columnItems.forEach { item ->
				val measurable = itemMeasurables[items.indexOf(item)]
				val placeable = measurable.measure(
					constraints.copy(
						maxWidth = columnWidth - itemHorizontalPadding,
						minWidth = 0,
						minHeight = 0
					)
				)
				columnNodes.add(placeable)
			}
			
			val maxColumnWidth = columnNodes.maxOf { it.width } + itemHorizontalPadding
			columnWidths.add(maxColumnWidth)
			
			columnItems.forEachIndexed { index, item ->
				val placeable = columnNodes[index]
				yPos += itemPadding.calculateTopPadding()
					.roundToPx()
				
				val parents = nodePositions.filter { parentNode ->
					linked(parentNode.info.data, item) && parentNode.info.columnIndex < columnIndexCurrent
				}
				val maxParentY = parents.maxOfOrNull { it.position.y } ?: 0
				yPos = maxOf(yPos, maxParentY)
				
				val xItemPos = xPos + itemPadding.calculateLeftPadding(layoutDirection)
					.roundToPx()
				
				nodePositions.add(
					GraphNode(
						info = GraphItem(item, emptyList(), itemColumn(item)),
						placeable = placeable,
						position = IntOffset(xItemPos, yPos)
					)
				)
				
				yPos += placeable.height + itemPadding.calculateBottomPadding()
					.roundToPx()
			}
			
			xPos += maxColumnWidth + contentPadding.calculateRightPadding(layoutDirection)
				.roundToPx()
		}
		
		layout(constraints.maxWidth, constraints.maxHeight) {
			
			nodePositions.forEach { node -> node.placeable.place(node.position) }
		}
	}
}

private enum class GraphSlot {
	ITEMS
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
					item1 == 5 && item2 == 6 ||
					
					item1 == 1 && item2 == 2 ||
					item1 == 1 && item2 == 7 ||
					item1 == 1 && item2 == 12 ||
					item1 == 6 && item2 == 17 ||
					
					item1 == 2 && item2 == 3 ||
					item1 == 12 && item2 == 13 ||
					
					item1 == 3 && item2 == 4 ||
					item1 == 8 && item2 == 9 ||
					item1 == 13 && item2 == 14 ||
					item1 == 18 && item2 == 9 ||
					
					item1 == 4 && item2 == 50 ||
					item1 == 9 && item2 == 50 ||
					item1 == 14 && item2 == 50 ||
					
					item1 == 50 && item2 == 55 ||
					item1 == 50 && item2 == 60 ||
					
					item1 == 55 && item2 == 51 ||
					
					item1 == 51 && item2 == 52 ||
					
					item1 == 52 && item2 == 53 ||
					item1 == 9 && item2 == 18 ||
					
					item1 == 53 && item2 == 54
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