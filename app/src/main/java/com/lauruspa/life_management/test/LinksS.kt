package com.lauruspa.life_management.test

//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.layout.size
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.Path
//import androidx.compose.ui.graphics.PathEffect
//import androidx.compose.ui.graphics.StrokeCap
//import androidx.compose.ui.graphics.drawscope.DrawScope
//import androidx.compose.ui.graphics.drawscope.Stroke
//import androidx.compose.ui.unit.IntOffset
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.toOffset

//val links = nodes.map { node ->
//	val linkedNodes = nodes
//		.filter { otherNode -> linked(node.info.data, otherNode.info.data) }
//
//	linkedNodes.map { linkedNode ->
//		val lineStart: IntOffset
//		val lineCenter: IntOffset
//		val lineEnd: IntOffset
//
//		// Linked node is on the same X
//		if (node.position.x == linkedNode.position.x) {
//			val linkSideIsOnTheRight = sameColumnLinkSideOnTheRight(
//				node.info.data,
//				linkedNode.info.data
//			)
//
//			if (linkSideIsOnTheRight) {
//				lineStart = IntOffset(
//					x = node.position.x + node.placeable.width,
//					y = node.position.y + node.placeable.height / 2
//				)
//				lineEnd = IntOffset(
//					x = linkedNode.position.x + node.placeable.width,
//					y = linkedNode.position.y + linkedNode.placeable.height / 2
//				)
//				lineCenter = IntOffset(
//					x = lineStart.x + sameColumnLinkPadding.roundToPx(),
//					y = lineStart.y + (lineEnd.y - lineStart.y) / 2
//				)
//			} else {
//				lineStart = IntOffset(
//					x = node.position.x,
//					y = node.position.y + node.placeable.height / 2
//				)
//				lineEnd = IntOffset(
//					x = linkedNode.position.x,
//					y = linkedNode.position.y + linkedNode.placeable.height / 2
//				)
//				lineCenter = IntOffset(
//					x = lineStart.x - sameColumnLinkPadding.roundToPx(),
//					y = lineStart.y + (lineEnd.y - lineStart.y) / 2
//				)
//			}
//		} else {
//			if (node.position.x < linkedNode.position.x) {
//				lineStart = IntOffset(
//					x = node.position.x + node.placeable.width,
//					y = node.position.y + node.placeable.height / 2
//				)
//				lineEnd = IntOffset(
//					x = linkedNode.position.x,
//					y = linkedNode.position.y + linkedNode.placeable.height / 2
//				)
//			} else {
//				lineStart = IntOffset(
//					x = node.position.x,
//					y = node.position.y + node.placeable.height / 2
//				)
//
//				lineEnd = IntOffset(
//					x = linkedNode.position.x + linkedNode.placeable.width,
//					y = linkedNode.position.y + linkedNode.placeable.height / 2
//				)
//			}
//
//			lineCenter = IntOffset(
//				x = lineStart.x + (lineEnd.x - lineStart.x) / 2,
//				y = lineStart.y + (lineEnd.y - lineStart.y) / 2
//			)
//		}
//
//		Link(
//			start = lineStart,
//			center = lineCenter,
//			end = lineEnd
//		)
//	}
//}
//	.flatten()
//
//val linesPlaceable = subcompose(GraphSlot.LINES) {
//	Canvas(
//		Modifier.size(
//			width = constraints.maxWidth.toDp(),
//			height = constraints.maxHeight.toDp()
//		)
//	) {
//		links.forEach { link ->
//			drawArrow(
//				start = link.start.toOffset(),
//				center = link.center.toOffset(),
//				end = link.end.toOffset(),
//				color = Color.Gray,
//				maxCornerRadiusPx = 8.dp.toPx(),
//				lineWidthPx = 1.dp.toPx(),
//				triangleLengthPx = 3.dp.toPx(),
//				triangleWidthPx = 7.dp.toPx()
//			)
//		}
//	}
//}.first()
//	.measure(constraints.copy(minWidth = 0, minHeight = 0))

//linesPlaceable.place(0, 0)
//
//private fun DrawScope.drawArrow(
//	start: Offset,
//	center: Offset,
//	end: Offset,
//	color: Color,
//	maxCornerRadiusPx: Float,
//	lineWidthPx: Float,
//	triangleLengthPx: Float,
//	triangleWidthPx: Float
//) {
//	val path = Path()
//	path.moveTo(start.x, start.y)
//
//	path.lineTo(center.x, start.y)
//	val lastLineSegmentStart = Offset(x = center.x, end.y)
//	path.lineTo(lastLineSegmentStart.x, lastLineSegmentStart.y)
//	path.lineTo(end.x, end.y)
//
//	drawPath(
//		path = path,
//		color = color,
//		style = Stroke(
//			width = lineWidthPx,
//			pathEffect = PathEffect.cornerPathEffect(maxCornerRadiusPx)
//		)
//	)
//
//	val isRight = if (end.x - lastLineSegmentStart.x >= 0) 1 else -1
//	val triangleBottomX = end.x - isRight * triangleLengthPx
//	val triangleHalfWidth = triangleWidthPx / 2
//	path.rewind()
//	path.moveTo(triangleBottomX, end.y - triangleHalfWidth)
//	path.lineTo(end.x, end.y)
//	path.lineTo(triangleBottomX, end.y + triangleHalfWidth)
//
//	drawPath(
//		path = path,
//		color = color,
//		style = Stroke(
//			width = lineWidthPx,
//			cap = StrokeCap.Round,
//			pathEffect = PathEffect.cornerPathEffect(lineWidthPx)
//		)
//	)
//}

//private data class Link(
//	val start: IntOffset,
//	val center: IntOffset,
//	val end: IntOffset
//)

//sameColumnLinkPadding: Dp = 16.dp,
//sameColumnLinkSideOnTheRight: (item1: T, item2: T) -> Boolean = { _, _ -> true },

//sameColumnLinkSideOnTheRight = { item1, item2 ->
//	val columnIndex = itemLevel(item1)
//	columnIndex < 0
//},