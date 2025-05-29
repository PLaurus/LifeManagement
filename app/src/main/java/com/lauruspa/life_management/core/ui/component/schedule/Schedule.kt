package com.lauruspa.life_management.core.ui.component.schedule

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.max

@Composable
fun <T> Schedule(
	itemsByRows: List<List<T>>,
	dateFrom: LocalDateTime,
	dateTo: LocalDateTime,
	columnDuration: Duration,
	itemDateRange: (T) -> DateRange,
	columnTitle: @Composable (columnDateFrom: LocalDateTime, columnDateTo: LocalDateTime) -> Unit,
	modifier: Modifier = Modifier,
	state: ScheduleState = rememberScheduleState(),
	columnTitleAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
	columnDivider: @Composable () -> Unit = {
		Box(
			modifier = Modifier
				.fillMaxHeight()
				.width(1.dp)
				.background(Color.Gray)
		)
	},
	itemsPaddingVerticalDp: Dp = 25.dp,
	itemsVerticalSpacing: Dp = 8.dp,
	emptyRowHeight: Dp = 0.dp,
	nowDate: LocalDateTime? = null,
	nowIndicatorColor: Color = Color.Blue,
	nowShimmerColor: Color = Color.White.copy(alpha = 0.5f),
	itemSlot: @Composable (T) -> Unit
) {
	// Валидация входных параметров
	require(dateTo.isAfter(dateFrom)) { "dateTo must be after dateFrom" }
	require(!columnDuration.isNegative && !columnDuration.isZero) { "columnDuration must be positive" }
	
	val verticalScrollState = rememberScrollState()
	
	SubcomposeLayout(
		modifier = modifier
			.then(state.onRemeasuredModifier)
			.horizontalScroll(state.horizontalScrollState)
	) { constraints ->
		
		// Расчет общей длительности расписания
		val requiredScheduleDurationMs = Duration.between(dateFrom, dateTo)
			.toMillis()
		val columnDurationMs = columnDuration.toMillis()
		
		// Вычисление количества столбцов для покрытия всей длительности
		val wholeColumnsCount = requiredScheduleDurationMs / columnDurationMs
		val columnsCount = if (requiredScheduleDurationMs % columnDurationMs != 0L) {
			if (wholeColumnsCount >= 0) wholeColumnsCount + 1 else wholeColumnsCount - 1
		} else {
			wholeColumnsCount
		}.toInt()
		
		val scheduleDurationMs = columnsCount * columnDurationMs
		
		// Создание диапазонов дат для каждого столбца
		val columnDateRanges = List(columnsCount) { column ->
			val columnDateFrom = dateFrom.plus(Duration.ofMillis(column * columnDurationMs))
			DateRange(
				dateFrom = columnDateFrom,
				dateTo = columnDateFrom.plus(Duration.ofMillis(columnDurationMs))
			)
		}
		
		// Измерение заголовков столбцов
		val columnTitlePlaceables = subcompose(ScheduleSlot.ColumnTitles) {
			columnDateRanges.forEach { dateRange ->
				columnTitle(dateRange.dateFrom, dateRange.dateTo)
			}
		}.map { measurable -> measurable.measure(Constraints()) }
		
		// Определение максимальной ширины и высоты заголовков столбцов
		val maxColumnTitleWidthPx = columnTitlePlaceables.maxOfOrNull { it.width } ?: 0
		val maxColumnTitleHeightPx = columnTitlePlaceables.maxOfOrNull { it.height } ?: 0
		
		// Расчет ширины каждого столбца с учетом ограничений и заголовков
		val columnWidthByConstraints = if (constraints.hasBoundedWidth) {
			constraints.maxWidth / columnsCount
		} else {
			constraints.minWidth / columnsCount
		}
		val columnWidthPx = max(columnWidthByConstraints, maxColumnTitleWidthPx)
		val scheduleWidthPx = columnWidthPx * columnsCount
		
		val scheduleItemsPlaceable = subcompose(ScheduleSlot.ScheduleItems) {
			ScheduleItems(
				dateFrom = dateFrom,
				itemsByRows = itemsByRows,
				itemDateRange = itemDateRange,
				scheduleDurationMs = scheduleDurationMs,
				itemsPaddingVerticalDp = itemsPaddingVerticalDp,
				itemsVerticalSpacing = itemsVerticalSpacing,
				emptyRowHeight = emptyRowHeight,
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(verticalScrollState),
				itemSlot = itemSlot
			)
		}.map { measurable ->
			measurable.measure(
				Constraints(
					minWidth = scheduleWidthPx,
					maxWidth = scheduleWidthPx,
					minHeight = 0,
					maxHeight = constraints.maxHeight - maxColumnTitleHeightPx
				)
			)
		}
			.first()
		
		// Вычисление позиций x для столбцов
		val columnXPositions =
			(0 until columnsCount).map { columnIndex -> columnIndex * columnWidthPx }
		
		// Определение общего размера содержимого расписания
		val scheduleContentSize = IntSize(
			width = scheduleWidthPx,
			height = constraints.maxHeight
		)
		
		// Измерение разделителей столбцов
		val columnDividersCount = (columnsCount - 1).coerceAtLeast(0)
		val columnDividerPlaceables = subcompose(ScheduleSlot.ColumnDividers) {
			repeat(columnDividersCount) {
				columnDivider()
			}
		}.map { measurable ->
			measurable.measure(
				Constraints(
					minWidth = 0,
					minHeight = 0,
					maxWidth = scheduleContentSize.width,
					maxHeight = scheduleContentSize.height
				)
			)
		}
		
		// Обработка индикатора текущего времени и эффекта затемнения, если nowDate задан
		val timeBeforeNowPAP: PlaceableAndPosition?
		val nowIndicatorPAP: PlaceableAndPosition?
		if (nowDate != null) {
			val nowPosX = calcDatePosX(
				date = nowDate,
				dateFrom = dateFrom,
				scheduleWidthPx = scheduleWidthPx,
				scheduleDurationMs = scheduleDurationMs
			)
			
			val shimmerWidth = abs(nowPosX)
			val shimmerHeight = scheduleContentSize.height - maxColumnTitleHeightPx
			
			val timeBeforeNowShimmerPlaceable = subcompose(ScheduleSlot.TimeBeforeNowShimmer) {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(nowShimmerColor)
				)
			}.map { measurable ->
				measurable.measure(
					Constraints(
						minWidth = shimmerWidth,
						maxWidth = shimmerWidth,
						minHeight = shimmerHeight,
						maxHeight = shimmerHeight
					)
				)
			}
				.first()
			
			timeBeforeNowPAP = PlaceableAndPosition(
				placeable = timeBeforeNowShimmerPlaceable,
				position = IntOffset(
					x = nowPosX - shimmerWidth,
					y = maxColumnTitleHeightPx
				)
			)
			
			val nowIndicatorWidthPx = 5.dp.roundToPx()
			val nowIndicatorHeight = scheduleContentSize.height - maxColumnTitleHeightPx
			val nowIndicatorRadiusPx = (nowIndicatorWidthPx / 2f)
			val nowIndicatorLineWidth = 1.dp.toPx()
			val gapLengthPx = 3.dp.toPx()
			
			val nowIndicatorPlaceable = subcompose(ScheduleSlot.NowIndicator) {
				Canvas(Modifier.fillMaxSize()) {
					drawCircle(
						color = nowIndicatorColor,
						radius = nowIndicatorRadiusPx,
						center = center.copy(y = nowIndicatorRadiusPx)
					)
					drawLine(
						color = nowIndicatorColor,
						start = center.copy(y = nowIndicatorRadiusPx),
						end = center.copy(y = size.height),
						strokeWidth = nowIndicatorLineWidth,
						cap = StrokeCap.Round,
						pathEffect = PathEffect.dashPathEffect(
							intervals = floatArrayOf(gapLengthPx, gapLengthPx),
						)
					)
				}
			}.map { measurable ->
				measurable.measure(
					Constraints(
						minWidth = nowIndicatorWidthPx,
						maxWidth = nowIndicatorWidthPx,
						minHeight = nowIndicatorHeight,
						maxHeight = nowIndicatorHeight
					)
				)
			}
				.first()
			
			nowIndicatorPAP = PlaceableAndPosition(
				placeable = nowIndicatorPlaceable,
				position = IntOffset(
					x = nowPosX - nowIndicatorWidthPx / 2,
					y = maxColumnTitleHeightPx
				)
			)
		} else {
			timeBeforeNowPAP = null
			nowIndicatorPAP = null
		}
		
		state.updateLayoutInfo(
			layoutInfo = ScheduleState.LayoutInfo(
				dateFrom = dateFrom,
				scheduleWidthPx = scheduleWidthPx,
				scheduleDurationMs = scheduleDurationMs
			)
		)
		
		layout(scheduleContentSize.width, scheduleContentSize.height) {
			// Размещение заголовков столбцов
			columnTitlePlaceables.forEachIndexed { index, placeable ->
				val localPosX = columnTitleAlignment.align(
					size = placeable.width,
					space = columnWidthPx,
					layoutDirection = layoutDirection
				)
				placeable.place(
					x = columnXPositions[index] + localPosX,
					y = 0
				)
			}
			
			// Размещение разделителей столбцов
			columnDividerPlaceables.forEachIndexed { index, placeable ->
				placeable.place(
					x = columnXPositions[index + 1] - placeable.width / 2,
					y = 0
				)
			}
			
			scheduleItemsPlaceable.place(
				x = 0,
				y = maxColumnTitleHeightPx
			)
			
			// Размещение эффекта затемнения до текущего времени
			timeBeforeNowPAP?.placeable?.place(timeBeforeNowPAP.position)
			
			// Размещение индикатора текущего времени
			nowIndicatorPAP?.placeable?.place(nowIndicatorPAP.position)
		}
	}
}

@Composable
private fun <T> ScheduleItems(
	dateFrom: LocalDateTime,
	itemsByRows: List<List<T>>,
	itemDateRange: (T) -> DateRange,
	scheduleDurationMs: Long,
	itemsPaddingVerticalDp: Dp,
	itemsVerticalSpacing: Dp,
	emptyRowHeight: Dp,
	modifier: Modifier = Modifier,
	itemSlot: @Composable (T) -> Unit
) {
	SubcomposeLayout(
		modifier = modifier
	) { constraints ->
		val scheduleWidthPx = constraints.minWidth
		// Преобразование единиц измерения из dp в пиксели
		val itemsPaddingVerticalPx = itemsPaddingVerticalDp.roundToPx()
		val itemsVerticalSpacingPx = itemsVerticalSpacing.roundToPx()
		val emptyRowHeightPx = emptyRowHeight.roundToPx()
		
		// Получение всех элементов из itemsByRows
		val allItems = itemsByRows.flatten()
		
		// Ассоциация элементов с их диапазонами дат и длительностями
		val itemToDateRange = allItems.associateWith { item -> itemDateRange(item) }
		
		val itemToDurationMs = itemToDateRange.mapValues { (_, dateRange) ->
			Duration.between(dateRange.dateFrom, dateRange.dateTo)
				.toMillis()
		}
		
		// Расчет ширины каждого элемента пропорционально его длительности
		val itemToItemWidth = itemToDurationMs.mapValues { (_, itemDurationMs) ->
			(itemDurationMs * scheduleWidthPx / scheduleDurationMs).toInt()
		}
		
		// Измерение элементов расписания с фиксированной шириной
		val itemPlaceables = subcompose(null) {
			allItems.forEach { item ->
				itemSlot(item)
			}
		}.mapIndexed { index, measurable ->
			val itemWidth = itemToItemWidth[allItems[index]] ?: 0
			measurable.measure(
				Constraints(
					minWidth = itemWidth,
					maxWidth = itemWidth
				)
			)
		}
		
		// Группировка placeables по рядам
		var index = 0
		val placeablesByRows = itemsByRows.map { row ->
			val rowPlaceables = itemPlaceables.subList(index, index + row.size)
			index += row.size
			rowPlaceables
		}
		
		// Расчет высоты каждого ряда (максимальная высота элементов в ряду)
		val rowHeights = placeablesByRows.map { rowPlaceables ->
			rowPlaceables.maxOfOrNull { it.height } ?: emptyRowHeightPx
		}
		
		// Расчет y-позиций для каждого ряда
		var currentY = itemsPaddingVerticalPx
		val rowYPositions = mutableListOf<Int>()
		for (rowIndex in itemsByRows.indices) {
			rowYPositions.add(currentY)
			if (rowIndex < itemsByRows.size - 1) {
				currentY += rowHeights[rowIndex] + itemsVerticalSpacingPx
			}
		}
		
		// Расчет позиций для элементов расписания
		val scheduleItems = placeablesByRows.flatMapIndexed { rowIndex, rowPlaceables ->
			val y = rowYPositions[rowIndex]
			rowPlaceables.mapIndexed { itemIndex, placeable ->
				val item = itemsByRows[rowIndex][itemIndex]
				val itemDateRangeValue = itemToDateRange[item]!!
				val itemRelativeDateFromMs = Duration.between(dateFrom, itemDateRangeValue.dateFrom)
					.toMillis()
				val itemPosX =
					(itemRelativeDateFromMs * scheduleWidthPx / scheduleDurationMs).toInt()
				PlaceableAndPosition(
					placeable = placeable,
					position = IntOffset(itemPosX, y)
				)
			}
		}
		
		val scheduleHeightPx = rowHeights.sum() + itemsPaddingVerticalPx * 2 +
				(rowHeights.size - 1).coerceAtLeast(0) * itemsVerticalSpacingPx
		
		layout(
			width = scheduleWidthPx.coerceIn(
				minimumValue = constraints.minWidth,
				maximumValue = constraints.maxWidth
			),
			height = scheduleHeightPx.coerceAtLeast(constraints.minHeight)
		) {
			// Размещение элементов расписания
			scheduleItems.forEach { scheduleItem ->
				scheduleItem.placeable.place(scheduleItem.position)
			}
		}
	}
}

private fun calcDatePosX(
	date: LocalDateTime,
	dateFrom: LocalDateTime,
	scheduleWidthPx: Int,
	scheduleDurationMs: Long
): Int {
	val itemRelativeDateFromMs = Duration.between(dateFrom, date)
		.toMillis()
	return (itemRelativeDateFromMs * scheduleWidthPx / scheduleDurationMs).toInt()
}

data class DateRange(
	val dateFrom: LocalDateTime,
	val dateTo: LocalDateTime
)

data class PlaceableAndPosition(
	val placeable: Placeable,
	val position: IntOffset
)

private enum class ScheduleSlot {
	ColumnTitles,
	ColumnDividers,
	ScheduleItems,
	TimeBeforeNowShimmer,
	NowIndicator
}

@Preview(showBackground = true)
@Composable
private fun SchedulePreview() {
	val timeDateFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
	val dayDateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy \nHH:mm") }
	
	val dateFrom = remember {
		LocalDateTime.of(
			LocalDate.of(2025, Month.AUGUST, 1),
			LocalTime.of(0, 0, 0)
		)
	}
	
	val dateTo = remember {
		LocalDateTime.of(
			LocalDate.of(2025, Month.AUGUST, 27),
			LocalTime.of(0, 0, 0)
		)
	}
	
	val itemDateRange: (Int) -> DateRange = { item ->
		when (item) {
			0 -> DateRange(
				dateFrom = dateFrom.plusHours(0),
				dateTo = dateFrom.plusMinutes(30)
			)
			
			1 -> DateRange(
				dateFrom = dateFrom.plusHours(9),
				dateTo = dateFrom.plusHours(18)
			)
			
			2 -> DateRange(
				dateFrom = dateFrom.plusHours(19),
				dateTo = dateFrom.plusHours(28)
			)
			
			3 -> DateRange(
				dateFrom = dateFrom.minusHours(1),
				dateTo = dateFrom.plusDays(1)
			)
			
			4 -> DateRange(
				dateFrom = dateFrom.plusHours(12),
				dateTo = dateFrom.plusHours(13)
			)
			
			6 -> DateRange(
				dateFrom = dateFrom.plusHours(11),
				dateTo = dateFrom.plusHours(16)
			)
			
			7 -> DateRange(
				dateFrom = dateFrom.plusHours(18),
				dateTo = dateFrom.plusHours(24)
			)
			
			else -> DateRange(
				dateFrom = dateFrom,
				dateTo = dateFrom.plusMinutes(30)
			)
		}
	}
	
	var schedulePreviewState by remember {
		mutableStateOf(
			SchedulePreviewState(
				scheduleDateRange = dateFrom..dateTo,
				chosenDate = dateTo.minusDays(5),
				zoom = 0
			)
		)
	}
	
	val chosenDateState by rememberUpdatedState(schedulePreviewState.chosenDate)
	val scheduleState = rememberScheduleState(
		initialFirstVisibleDate = remember { chosenDateState }
	)
	
	LaunchedEffect(scheduleState) {
		val targetDateFlow = snapshotFlow { scheduleState.targetDate }
		val chosenDateFlow = snapshotFlow { chosenDateState }
		
		targetDateFlow
			.combine(chosenDateFlow) { targetDate, chosenDate ->
				targetDate to chosenDate
			}
			.collectLatest(
				object : suspend (Pair<LocalDateTime?, LocalDateTime>) -> Unit {
					var prevTargetDate: LocalDateTime? = null
					var prevChosenDate: LocalDateTime? = null
					override suspend fun invoke(value: Pair<LocalDateTime?, LocalDateTime>) {
						val (targetDate, chosenDate) = value
						
						if (chosenDate != targetDate && chosenDate != prevChosenDate) {
							launch {
								scheduleState.animateScrollToDate(chosenDate)
							}
							
							prevChosenDate = targetDate
						} else {
							if (targetDate != null) {
								schedulePreviewState = schedulePreviewState.updateChosenDate(targetDate)
								prevChosenDate = targetDate
							} else {
								prevChosenDate = chosenDate
							}
						}
						
						prevTargetDate = targetDate
					}
				}
			)
	}
	
	val coroutineScope = rememberCoroutineScope()
	
	Column(
		modifier = Modifier.fillMaxSize()
	) {
		Text(
			text = remember(dayDateFormatter, chosenDateState) {
				val value = dayDateFormatter.format(chosenDateState)
				"Chosen: $value"
			}
		)
		
		val scheduleTargetDate = scheduleState.targetDate
		Text(
			text = remember(dayDateFormatter, scheduleTargetDate) {
				val value = if (scheduleTargetDate != null) {
					dayDateFormatter.format(scheduleTargetDate)
				} else {
					"Нет данных"
				}
				"Target: $value"
			}
		)
		
		Row {
			Button(
				onClick = {
					schedulePreviewState = schedulePreviewState.zoomOut()
				}
			) {
				Text(text = "-")
			}
			Button(
				onClick = {
					schedulePreviewState = schedulePreviewState.zoomIn()
				}
			) {
				Text(text = "+")
			}
		}
		
		Schedule(
			itemsByRows = remember {
				buildList {
					add(listOf(0, 1, 2))
					add(emptyList())
					for (i in 3..50) {
						add(listOf(i))
					}
				}
			},
			dateFrom = dateFrom,
			dateTo = dateTo,
			columnDuration = remember(schedulePreviewState.zoom) {
				when (schedulePreviewState.zoom) {
					1 -> Duration.ofHours(2)
					else -> Duration.ofDays(1)
				}
			},
			itemDateRange = itemDateRange,
			columnTitle = { columnDateFrom: LocalDateTime, columnDateTo: LocalDateTime ->
				val formattedDateFrom = remember(schedulePreviewState.zoom, columnDateFrom, timeDateFormatter) {
					when (schedulePreviewState.zoom) {
						1 -> columnDateFrom.format(timeDateFormatter)
						else -> columnDateFrom.format(dayDateFormatter)
					}
					
				}
				val formattedDateTo = remember(columnDateTo, timeDateFormatter) {
					when (schedulePreviewState.zoom) {
						1 -> columnDateTo.format(timeDateFormatter)
						else -> ""
					}
				}
				
				Column(
					horizontalAlignment = Alignment.CenterHorizontally,
					modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
				) {
					Text(
						text = formattedDateFrom,
						fontWeight = FontWeight.W400,
						fontSize = 12.sp,
						lineHeight = 16.sp,
						textAlign = TextAlign.Center,
						color = Color.Gray
					)
					Text(
						text = formattedDateTo,
						fontWeight = FontWeight.W400,
						fontSize = 12.sp,
						lineHeight = 16.sp,
						textAlign = TextAlign.Center,
						color = Color.Gray
					)
				}
			},
			modifier = Modifier.weight(1f),
			state = scheduleState,
			emptyRowHeight = 48.dp,
			nowDate = remember(dateFrom) { dateFrom.plusHours(9) }
		) { item ->
			val shape = RoundedCornerShape(2.dp)
			val color = remember(item) {
				when (item % 4) {
					0 -> Color.Yellow
					1 -> Color.Red
					2 -> Color.Green
					else -> Color.Magenta
				}
			}
			
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.clip(shape)
					.background(color = color)
					.drawWithContent {
						val strokeWidthPx = 2.dp.toPx()
						drawContent()
						drawLine(
							color = Color.Black.copy(alpha = 0.25f),
							start = Offset(strokeWidthPx / 2, 0f),
							end = Offset(strokeWidthPx / 2, size.height),
							strokeWidth = strokeWidthPx
						)
					}
					.clickable {
						coroutineScope.launch {
							val date = itemDateRange(item).dateFrom
							scheduleState.animateScrollToDate(date)
						}
					},
			) {
				Text(
					text = when (item) {
						1 -> "Item 0\n".repeat(2)
						else -> "Item $item"
					},
					modifier = Modifier
						.padding(start = 10.dp, top = 6.dp, bottom = 6.dp)
						.requiredWidthIn(max = Dp.Infinity),
					fontWeight = FontWeight.W500,
					fontSize = 14.sp,
					lineHeight = 20.sp,
					color = Color.Black,
				)
			}
		}
	}
}

@Immutable
private data class SchedulePreviewState(
	val scheduleDateRange: ClosedRange<LocalDateTime>,
	val chosenDate: LocalDateTime,
	val zoom: Int
) {
	private val minZoom = 0
	private val maxZoom = 1
	
	fun updateChosenDate(newDate: LocalDateTime): SchedulePreviewState {
		return copy(
			chosenDate = newDate
		)
	}
	
	fun zoomIn(): SchedulePreviewState {
		val newZoom = (zoom + 1).coerceAtMost(maxZoom)
		return copy(
			chosenDate = scheduleDateRange.endInclusive.minusDays(5),
			zoom = newZoom,
		)
	}
	
	fun zoomOut(): SchedulePreviewState {
		val newZoom = (zoom - 1).coerceAtLeast(minZoom)
		return copy(
			chosenDate = scheduleDateRange.endInclusive.minusDays(5),
			zoom = newZoom,
		)
	}
}