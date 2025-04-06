package com.lauruspa.life_management.core.ui.component.honey_combs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import com.lauruspa.life_management.R
import com.lauruspa.life_management.core.ui.shape.EquilateralPolygonShape
import com.lauruspa.life_management.core.ui.utils.detectTransformGestures
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun <T> HoneyCombs(
	items: List<T>,
	modifier: Modifier = Modifier,
	state: HoneyCombsState = rememberHoneyCombsState(),
	itemKey: ((index: Int, item: T) -> Any)? = null,
	minCellSize: Dp = HoneyCombsDefaults.CellSize,
	contentPadding: PaddingValues = HoneyCombsDefaults.ContentPadding,
	maxCrossAxisSize: Dp = Dp.Unspecified,
	orientation: Orientation = Orientation.Horizontal,
	alignment: Alignment = Alignment.Center,
	debugMode: Boolean = false,
	hEmptyItemsOverflow: Int = 2,
	vEmptyItemsOverflow: Int = 1,
	emptyItemSlot: @Composable BoxScope.() -> Unit = { },
	decorSlot: @Composable (layoutInfo: HoneyCombsLayoutInfo) -> Unit = { },
	itemSlot: @Composable (index: Int, item: T) -> Unit,
) {
	val coroutineScope = rememberCoroutineScope()
	SubcomposeLayout(
		modifier = modifier
			.clipToBounds()
			.pointerInput(Unit) {
				detectTransformGestures(
					pass = PointerEventPass.Initial,
					onGestureStart = {
						coroutineScope.launch {
							state.onGestureStart()
						}
					},
					onGestureEnd = {
						coroutineScope.launch {
							state.onGestureEnd {}
						}
					},
					onGesture = { gestureCentroid: Offset,
						gesturePan: Offset,
						gestureZoom: Float,
						rotationChange: Float,
						mainPointer: PointerInputChange,
						changes: List<PointerInputChange> ->
						
						coroutineScope.launch {
							state.onGesture(
								centroid = gestureCentroid,
								pan = gesturePan,
								zoom = gestureZoom,
								rotation = rotationChange,
								mainPointer = mainPointer,
								changes = changes
							)
						}
						
						// Consume touch when multiple fingers down
						// This prevents click and long click if your finger touches a
						// button while pinch gesture is being invoked
						val size = changes.size
						if (size > 1) {
							changes.forEach { it.consume() }
						}
					})
			}
			.graphicsLayer {
				scaleX = state.zoom
				scaleY = state.zoom
				translationX = state.pan.x
				translationY = state.pan.y
				transformOrigin = TransformOrigin(0f, 0f)
			}
	) { gridConstraints ->
		val containerSize = IntSize(
			width = gridConstraints.maxWidth,
			height = gridConstraints.maxHeight
		)
		val cellSizePx = minCellSize.toPx()
			.roundToInt()
		val evenRowOffset = IntOffset(
			x = cellSizePx / 2,
			y = -cellSizePx * 1 / 6
		)
		val itemMeasurables = subcompose(CellsGridSlot.Items) {
			items.onEachIndexed { index, item ->
				key(itemKey?.invoke(index, item) ?: index) {
					var scaleX by remember { mutableStateOf(1f) }
					var scaleY by remember { mutableStateOf(1f) }
					
					Layout(
						content = { itemSlot(index, item) },
						modifier = Modifier
							.fillMaxSize()
							.graphicsLayer {
								this.scaleX = scaleX
								this.scaleY = scaleY
								transformOrigin = TransformOrigin(0f, 0f)
							}
					) { measurables, constraints ->
						val placeable = measurables.firstOrNull()
							?.measure(
								gridConstraints.copy(
									minWidth = 0,
									minHeight = 0
								)
							)
						
						if (placeable != null) {
							scaleX = constraints.maxWidth.toFloat() / placeable.width
							scaleY = constraints.maxHeight.toFloat() / placeable.height
						}
						
						layout(constraints.maxWidth, constraints.maxHeight) {
							placeable?.placeRelative(0, 0)
						}
					}
				}
			}
		}
		
		val itemPlaceables = itemMeasurables
			.map { measurable ->
				measurable.measure(
					constraints = gridConstraints.copy(
						minWidth = cellSizePx,
						minHeight = cellSizePx,
						maxWidth = cellSizePx,
						maxHeight = cellSizePx
					)
				)
			}
		
		val itemsCountPerSquareSide = ceil(sqrt(items.size.toFloat())).toInt()
		
		val itemsSquareWidthPx = calcHoneyCombsRectWidth(
			honeyCombSizePx = cellSizePx,
			xHoneyCombsCount = itemsCountPerSquareSide,
			totalHoneyCombsCount = items.size,
			evenRowOffsetX = evenRowOffset.x
		)
		
		val itemsSquareHeightPx = calcHoneyCombsRectHeight(
			honeyCombSizePx = cellSizePx,
			yHoneyCombsCount = itemsCountPerSquareSide,
			evenRowOffsetY = evenRowOffset.y
		)
		
		val itemsCountPerRectY: Int
		val itemsCountPerRectX: Int
		
		val maxCrossAxisSizePx =
			if (maxCrossAxisSize.isFinite && maxCrossAxisSize.isSpecified) {
				maxCrossAxisSize.toPx()
					.roundToInt()
			} else {
				null
			}
		
		when (orientation) {
			Orientation.Horizontal -> {
				val coercedHeightPx = if (maxCrossAxisSizePx != null) {
					itemsSquareHeightPx.coerceAtMost(maxCrossAxisSizePx)
				} else {
					itemsSquareHeightPx
				}
				
				itemsCountPerRectY = calcColumnHoneyCombsCount(
					heightPx = coercedHeightPx,
					honeyCombSizePx = cellSizePx,
					evenRowOffsetY = evenRowOffset.y
				)
				itemsCountPerRectX = ceil(items.size.toFloat() / itemsCountPerRectY).toInt()
			}
			
			Orientation.Vertical -> {
				val coercedWidthPx = if (maxCrossAxisSizePx != null) {
					itemsSquareWidthPx.coerceAtMost(maxCrossAxisSizePx)
				} else {
					itemsSquareWidthPx
				}
				
				itemsCountPerRectX = calcRowHoneyCombsCount(
					widthPx = coercedWidthPx,
					honeyCombSizePx = cellSizePx
				)
				itemsCountPerRectY = ceil(items.size.toFloat() / itemsCountPerRectX).toInt()
			}
		}
		
		val itemsRectWidthPx = calcHoneyCombsRectWidth(
			honeyCombSizePx = cellSizePx,
			xHoneyCombsCount = itemsCountPerRectX,
			totalHoneyCombsCount = items.size,
			evenRowOffsetX = evenRowOffset.x
		)
		
		val itemsRectHeightPx = calcHoneyCombsRectHeight(
			honeyCombSizePx = cellSizePx,
			yHoneyCombsCount = itemsCountPerRectY,
			evenRowOffsetY = evenRowOffset.y
		)
		
		val itemsRectSize = IntSize(
			width = itemsRectWidthPx,
			height = itemsRectHeightPx
		)
		
		val itemsRectOffset = alignment.align(
			size = itemsRectSize,
			space = containerSize,
			layoutDirection = layoutDirection
		)
		
		val itemsRect = IntRect(
			offset = itemsRectOffset,
			size = itemsRectSize
		)
		
		val hEmptyItemsOverflowSafe = hEmptyItemsOverflow.coerceAtLeast(0)
		val vEmptyItemsOverflowSafe = vEmptyItemsOverflow.coerceAtLeast(0)
		
		val paddingLeftPx =
			contentPadding.calculateStartPadding(layoutDirection)
				.toPx()
				.roundToInt()
		val paddingTopPx = contentPadding.calculateTopPadding()
			.toPx()
			.roundToInt()
		val paddingRightPx = contentPadding.calculateEndPadding(layoutDirection)
			.toPx()
			.roundToInt()
		val paddingBottomPx = contentPadding.calculateBottomPadding()
			.toPx()
			.roundToInt()
		val movableArea = IntRect(
			left = minOf(itemsRect.left - paddingLeftPx, 0),
			top = minOf(itemsRect.top - paddingTopPx, 0),
			right = maxOf(itemsRect.right + paddingRightPx, containerSize.width),
			bottom = maxOf(itemsRect.bottom + paddingBottomPx, containerSize.height)
		)
		
		val emptySpaceLeft = (itemsRect.left - movableArea.left).coerceAtLeast(0)
		val emptySpaceTop = (itemsRect.top - movableArea.top).coerceAtLeast(0)
		val emptySpaceRight = (movableArea.right - itemsRect.right).coerceAtLeast(0)
		val emptySpaceBottom = (movableArea.bottom - itemsRect.bottom).coerceAtLeast(0)
		
		val emptyCellsCountLeft = calcRowHoneyCombsCount(
			widthPx = emptySpaceLeft,
			honeyCombSizePx = cellSizePx
		) + hEmptyItemsOverflowSafe
		
		val emptyCellsCountTop = calcColumnHoneyCombsCount(
			heightPx = emptySpaceTop + abs(evenRowOffset.y),
			honeyCombSizePx = cellSizePx,
			evenRowOffsetY = evenRowOffset.y
		) + vEmptyItemsOverflowSafe
		
		val emptyCellsCountRight = calcRowHoneyCombsCount(
			widthPx = emptySpaceRight,
			honeyCombSizePx = cellSizePx
		) + hEmptyItemsOverflowSafe
		
		val emptyCellsCountBottom = calcColumnHoneyCombsCount(
			heightPx = emptySpaceBottom + abs(evenRowOffset.y),
			honeyCombSizePx = cellSizePx,
			evenRowOffsetY = evenRowOffset.y
		) + vEmptyItemsOverflowSafe
		
		val actualGridRect = IntRect(
			left = itemsRect.left - calcHoneyCombsRectWidth(
				honeyCombSizePx = cellSizePx,
				xHoneyCombsCount = emptyCellsCountLeft,
				totalHoneyCombsCount = emptyCellsCountLeft,
				evenRowOffsetX = evenRowOffset.x
			),
			top = itemsRect.top - calcHoneyCombsRectHeight(
				honeyCombSizePx = cellSizePx,
				yHoneyCombsCount = emptyCellsCountTop,
				evenRowOffsetY = evenRowOffset.y
			) - evenRowOffset.y,
			right = itemsRect.right + calcHoneyCombsRectWidth(
				honeyCombSizePx = cellSizePx,
				xHoneyCombsCount = emptyCellsCountRight,
				totalHoneyCombsCount = emptyCellsCountRight,
				evenRowOffsetX = evenRowOffset.x
			),
			bottom = itemsRect.bottom + calcHoneyCombsRectHeight(
				honeyCombSizePx = cellSizePx,
				yHoneyCombsCount = emptyCellsCountBottom,
				evenRowOffsetY = evenRowOffset.y
			) + evenRowOffset.y
		)
		
		val gridSize = actualGridRect.size
		val itemsCountPerGridWidth = calcRowHoneyCombsCount(
			widthPx = gridSize.width,
			honeyCombSizePx = cellSizePx
		)
		val itemsCountPerGridHeight = calcColumnHoneyCombsCount(
			heightPx = gridSize.height,
			honeyCombSizePx = cellSizePx,
			evenRowOffsetY = evenRowOffset.y
		)
		val allCellsCount = itemsCountPerGridWidth * itemsCountPerGridHeight
		val emptyItemsCount = (allCellsCount - items.size).coerceAtLeast(0)
		
		val emptyItemMeasurables = subcompose(CellsGridSlot.EmptyItems) {
			repeat(emptyItemsCount) {
				Box(Modifier.fillMaxSize()) {
					emptyItemSlot()
				}
			}
		}
		
		val emptyItemPlaceables = emptyItemMeasurables.map { measurable ->
			measurable.measure(
				constraints = gridConstraints.copy(
					minWidth = cellSizePx,
					minHeight = cellSizePx,
					maxWidth = cellSizePx,
					maxHeight = cellSizePx
				)
			)
		}
		
		val debugInfoPlaceables = if (debugMode) {
			val debugInfoMeasurables = subcompose(CellsGridSlot.DebugPanel) {
				Text(
					text = """
                        itemsCount: ${items.size};
                        cellSizePx: $cellSizePx;
                        itemsCountPerSquareSide: $itemsCountPerSquareSide;
                        zoom: ${state.zoom};
                        panX: ${state.pan.x};
                        panY: ${state.pan.y};
                    """.trimIndent(),
					modifier = Modifier.background(Color(0x4D000000)),
					color = Color.Black
				)
				
				Canvas(modifier = Modifier.fillMaxSize()) {
					drawRect(
						color = Color(0xFF673AB7),
						topLeft = itemsRect.topLeft.toOffset(),
						size = itemsRect.size.toSize(),
						style = Stroke(width = 2.dp.toPx())
					)
					
					drawRect(
						color = Color(0xFF4CAF50),
						topLeft = actualGridRect.topLeft.toOffset(),
						size = actualGridRect.size.toSize(),
						style = Stroke(width = 2.dp.toPx())
					)
					
					drawRect(
						color = Color(0xFFE91E63),
						topLeft = movableArea.topLeft.toOffset(),
						size = movableArea.size.toSize(),
						style = Stroke(width = 2.dp.toPx())
					)
				}
			}
			
			debugInfoMeasurables.map { measurable ->
				measurable.measure(
					constraints = gridConstraints.copy(
						minWidth = 0,
						minHeight = 0
					)
				)
			}
		} else {
			null
		}
		
		state.layoutInfo = HoneyCombsLayoutInfo(
			itemsCount = items.size,
			containerSize = containerSize,
			cellSize = cellSizePx,
			itemsRect = itemsRect,
			paddingLeft = paddingLeftPx,
			paddingTop = paddingTopPx,
			paddingRight = paddingRightPx,
			paddingBottom = paddingBottomPx,
			movableAreaRect = movableArea,
			emptySpaceLeft = emptySpaceLeft,
			emptySpaceTop = emptySpaceTop,
			emptySpaceRight = emptySpaceRight,
			emptySpaceBottom = emptySpaceBottom,
			emptyCellsCountLeft = emptyCellsCountLeft,
			emptyCellsCountTop = emptyCellsCountTop,
			emptyCellsCountRight = emptyCellsCountRight,
			emptyCellsCountBottom = emptyCellsCountBottom,
			actualGridRect = actualGridRect,
			itemsCountPerGridWidth = itemsCountPerGridWidth,
			itemsCountPerGridHeight = itemsCountPerGridHeight,
			allCellsCount = allCellsCount,
			emptyItemsCount = emptyItemsCount
		)
		
		val decorPlaceables = subcompose(CellsGridSlot.Decor) { decorSlot(state.layoutInfo) }
			.map { measurable ->
				measurable.measure(
					gridConstraints.copy(
						minWidth = 0,
						minHeight = 0,
						maxWidth = movableArea.width,
						maxHeight = movableArea.height
					)
				)
			}
		
		layout(containerSize.width, containerSize.height) {
			
			val itemsIterator = itemPlaceables.iterator()
			val emptyItemsIterator = emptyItemPlaceables.iterator()
			
			for (row in 0 until itemsCountPerGridHeight) {
				val isEvenRow = (row + (emptyCellsCountTop % 2)) % 2
				for (column in 0 until itemsCountPerGridWidth) {
					val x = actualGridRect.left + column * cellSizePx + isEvenRow * evenRowOffset.x
					val y = actualGridRect.top + row * cellSizePx + row * evenRowOffset.y
					
					val placeable =
						if (itemsRect.contains(IntOffset(x, y)) &&
							x + cellSizePx <= itemsRect.right &&
							itemsIterator.hasNext()
						) {
							itemsIterator.next()
						} else if (emptyItemsIterator.hasNext()) {
							emptyItemsIterator.next()
						} else {
							break
						}
					
					placeable.placeRelative(
						x = x,
						y = y
					)
				}
			}
			
			debugInfoPlaceables?.forEach { placeable ->
				placeable.placeRelative(0, 0)
			}
			
			decorPlaceables.forEach { placeable ->
				placeable.placeRelative(movableArea.left, movableArea.top)
			}
		}
	}
}

private fun calcRowHoneyCombsCount(
	widthPx: Int,
	honeyCombSizePx: Int,
): Int {
	return widthPx / honeyCombSizePx
}

private fun calcColumnHoneyCombsCount(
	heightPx: Int,
	honeyCombSizePx: Int,
	evenRowOffsetY: Int
): Int {
	return (heightPx + evenRowOffsetY) / (honeyCombSizePx + evenRowOffsetY)
}

private fun calcHoneyCombsRectWidth(
	honeyCombSizePx: Int,
	xHoneyCombsCount: Int,
	totalHoneyCombsCount: Int,
	evenRowOffsetX: Int
): Int {
	val hasFullEvenRows = xHoneyCombsCount > 0 && totalHoneyCombsCount / xHoneyCombsCount > 1
	val evenRowOffset = if (hasFullEvenRows) evenRowOffsetX else 0
	return honeyCombSizePx * xHoneyCombsCount + evenRowOffset
}

private fun calcHoneyCombsRectHeight(
	honeyCombSizePx: Int,
	yHoneyCombsCount: Int,
	evenRowOffsetY: Int
): Int {
	return honeyCombSizePx * yHoneyCombsCount + evenRowOffsetY * (yHoneyCombsCount - 1)
}

private enum class CellsGridSlot {
	Items,
	EmptyItems,
	DebugPanel,
	Decor
}

@Preview(showBackground = true)
@Composable
private fun HoneyCombsPreview() {
	
	var itemsCount by remember { mutableStateOf(10) }
	val items by remember { derivedStateOf { List(itemsCount) { 0 } } }
	val honeyCombsState = rememberHoneyCombsState(
		maxZoom = 6f
	)
	
	Column(Modifier.fillMaxSize()) {
		var onFirstLayoutEnd: suspend () -> Unit by remember { mutableStateOf({}) }
		
		HoneyCombs(
			modifier = Modifier
				.weight(1f)
				.background(Color.White),
			items = items,
			state = honeyCombsState,
			minCellSize = 58.dp,
			alignment = Alignment.Center,
			orientation = Orientation.Vertical,
			debugMode = false,
			emptyItemSlot = {
				Surface(
					modifier = Modifier.fillMaxSize(),
					shape = EquilateralPolygonShape(
						anglesCount = 6,
						cornerSize = CornerSize(6.dp)
					),
					color = Color.LightGray
				) { }
			},
			decorSlot = {
				LaunchedEffect(onFirstLayoutEnd) {
					onFirstLayoutEnd()
				}
			}
		) { index, _ ->
			val zoom by remember {
				derivedStateOf {
					if (honeyCombsState.zoom > 2) 2f else 1f
				}
			}
			PreviewHoneyComb(index = index, zoom = zoom)
		}
		
		val maxItemsCount = remember { 100f }
		val minItemsCount = remember { 0f }
		val stepsCount = remember(maxItemsCount, minItemsCount) {
			(maxItemsCount - minItemsCount + 1).toInt()
		}
		
		Row(
			horizontalArrangement = Arrangement.spacedBy(16.dp),
			modifier = Modifier.padding(horizontal = 16.dp)
		) {
			val scope = rememberCoroutineScope()
			Button(
				onClick = {
					scope.launch {
						honeyCombsState.animateCameraPosition {
							containerCenter(zoom = it.zoom - 0.5f)
						}
					}
				},
				modifier = Modifier.weight(1f)
			) {
				Text(text = "-")
			}
			
			Button(
				onClick = {
					scope.launch {
						honeyCombsState.animateCameraPosition {
							containerCenter(zoom = it.zoom + 0.5f)
						}
					}
				},
				modifier = Modifier.weight(1f)
			) {
				Text(text = "+")
			}
			
			Button(
				onClick = {
					scope.launch {
						honeyCombsState.animateCameraPosition {
							fitItems()
						}
					}
				},
				modifier = Modifier.weight(1f)
			) {
				Icon(
					painter = painterResource(R.drawable.ic_home_24dp),
					contentDescription = null
				)
			}
		}
		
		var sliderItemsCount by remember { mutableIntStateOf(itemsCount) }
		var isManualItemsCountUpdate by remember { mutableStateOf(true) }
		
		Slider(
			value = sliderItemsCount.toFloat(),
			onValueChange = { newValue ->
				sliderItemsCount = newValue.roundToInt()
			},
			valueRange = 0f..1000f,
			steps = stepsCount
		)
		
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text("Manual items count update: ")
			Spacer(Modifier.height(16.dp))
			Checkbox(
				checked = isManualItemsCountUpdate,
				onCheckedChange = {
					isManualItemsCountUpdate = !isManualItemsCountUpdate
				}
			)
		}
		
		if (isManualItemsCountUpdate) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(16.dp)
			) {
				Button(
					onClick = {
						itemsCount = sliderItemsCount
					},
					modifier = Modifier.weight(1f)
				) {
					Text("Update")
				}
				
				Button(
					onClick = {
						itemsCount = sliderItemsCount
						onFirstLayoutEnd = {
							honeyCombsState.animateCameraPosition {
								fitItems()
							}
						}
					},
					modifier = Modifier.weight(1f)
				) {
					Text("Update with")
					Icon(
						painter = painterResource(R.drawable.ic_home_24dp),
						contentDescription = null
					)
				}
			}
		}
	}
}

@Composable
private fun PreviewHoneyComb(
	index: Int,
	zoom: Float,
	modifier: Modifier = Modifier
) {
	var honeyCombColor by remember { mutableStateOf(Color.White) }
	val borderColor = when (index % 3) {
		0 -> Color.Green
		1 -> Color.Red
		else -> Color.Unspecified
	}
	
	Surface(
		onClick = {
			honeyCombColor = if (honeyCombColor == Color.White) {
				Color.Red
			} else {
				Color.White
			}
		},
		modifier = modifier.size(140.dp),
		shape = EquilateralPolygonShape(anglesCount = 6, cornerSize = CornerSize(12.dp)),
		border = if (borderColor.isSpecified) {
			BorderStroke(
				width = 2.dp,
				color = borderColor
			)
		} else {
			null
		},
		color = honeyCombColor,
		shadowElevation = 4.dp
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(all = 15.dp),
			verticalArrangement = Arrangement.Center
		) {
			if (zoom >= 2) {
				Text(
					text = """
                            Развитие клиентского опыта B2C
                        """.trimIndent(),
					fontWeight = FontWeight.W500,
					fontSize = 12.sp,
					lineHeight = 16.sp,
					overflow = TextOverflow.Ellipsis,
					textAlign = TextAlign.Center
				)
			}
		}
	}
}

@Preview
@Composable
private fun PreviewHoneyCombPreview() {
	PreviewHoneyComb(
		index = 0,
		zoom = 1f
	)
}