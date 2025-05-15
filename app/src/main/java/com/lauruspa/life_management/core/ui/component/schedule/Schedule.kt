package com.lauruspa.life_management.core.ui.component.schedule

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun <T> Schedule(
    items: List<T>,
    dateFrom: LocalDateTime,
    dateTo: LocalDateTime,
    columnDuration: Duration,
    itemDateRange: (T) -> DateRange,
    columnTitle: @Composable (columnDateFrom: LocalDateTime, columnDateTo: LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    columnTitleAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    columnDivider: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(Color.Gray)

        )
    },
    itemsPaddingVerticalDp: Dp = 29.dp,
    itemsVerticalSpacing: Dp = 8.dp,
    nowDate: LocalDateTime? = null,
    itemSlot: @Composable (T) -> Unit
) {
    SubcomposeLayout(
        modifier = modifier
            .background(backgroundColor)
    ) { constraints ->
        val itemsPaddingVerticalPx = itemsPaddingVerticalDp.toPx().roundToInt()
        val itemsVerticalSpacingPx = itemsVerticalSpacing.toPx().roundToInt()

        val requiredScheduleDuration = Duration.between(dateFrom, dateTo)
        val requiredScheduleDurationMs = requiredScheduleDuration.toMillis()

        val columnDurationMs = columnDuration.toMillis()
        val wholeColumnsCount = requiredScheduleDurationMs / columnDurationMs
        val columnsCount = if (requiredScheduleDurationMs % columnDurationMs != 0L) {
            if (wholeColumnsCount >= 0) {
                wholeColumnsCount + 1
            } else {
                wholeColumnsCount - 1
            }
        } else {
            wholeColumnsCount
        }.toInt()

        val scheduleDurationMs = columnsCount * columnDurationMs

        val columnDateRanges = List(columnsCount) { column ->
            val columnDateFrom = dateFrom.plus(
                column * columnDurationMs,
                ChronoUnit.MILLIS
            )

            DateRange(
                dateFrom = columnDateFrom,
                dateTo = columnDateFrom.plus(columnDurationMs, ChronoUnit.MILLIS)
            )
        }

        val columnTitlePlaceables = subcompose(ScheduleSlot.ColumnTitles) {
            columnDateRanges.forEach { dateRange ->
                columnTitle(dateRange.dateFrom, dateRange.dateTo)
            }
        }.map { measurable ->
            measurable.measure(Constraints())
        }

        val itemToDateRange = items.associateWith { item ->
            itemDateRange(item)
        }

        val itemToDurationMs = itemToDateRange.mapValues { (_, dateRange) ->
            Duration.between(dateRange.dateFrom, dateRange.dateTo)
                .toMillis()
        }

        val columnWidthByConstraints = if (constraints.hasBoundedWidth) {
            constraints.maxWidth / columnsCount
        } else {
            constraints.minWidth / columnsCount
        }

        val maxColumnTitleWidthPx = columnTitlePlaceables
            .maxOfOrNull { columnTitlePlaceable ->
                columnTitlePlaceable.width
            } ?: 0

        val maxColumnTitleHeightPx = columnTitlePlaceables
            .maxOfOrNull { columnTitlePlaceable ->
                columnTitlePlaceable.height
            } ?: 0

        val columnWidthPx = max(columnWidthByConstraints, maxColumnTitleWidthPx)
        val scheduleWidthPx = columnWidthPx * columnsCount

        val itemToItemWidth = itemToDurationMs.mapValues { (_, itemDurationMs) ->
            // itemDurationMs / durationMs = itemWidth / scheduleWidthPx
            (itemDurationMs * scheduleWidthPx / scheduleDurationMs).toInt()
        }

        val itemPlaceables = subcompose(ScheduleSlot.ScheduleItems) {
            items.forEach { item ->
                itemSlot(item)
            }
        }.mapIndexed { index, measurable ->
            val itemWidth = itemToItemWidth[items.elementAt(index)] ?: 0
            measurable.measure(
                Constraints(
                    minWidth = itemWidth,
                    maxWidth = itemWidth
                )
            )
        }

        val columnXPositions = (0 until columnsCount)
            .map { columnIndex ->
                columnIndex * columnWidthPx
            }

        val columnTitleXPositions = columnTitlePlaceables
            .mapIndexed { index, placeable ->
                val localPosX = columnTitleAlignment.align(
                    size = placeable.width,
                    space = columnWidthPx,
                    layoutDirection = layoutDirection
                )

                columnXPositions[index] + localPosX
            }

        val itemToItemPosX = itemToDateRange.mapValues { (_, dateRange) ->
            // itemDateFromMs / durationMs = itemX / scheduleWidthPx
            val itemRelativeDateFromMs = Duration.between(dateFrom, dateRange.dateFrom)
                .toMillis()
            (itemRelativeDateFromMs * scheduleWidthPx / requiredScheduleDurationMs).toInt()
        }

        var scheduleItemPosY = maxColumnTitleHeightPx + itemsPaddingVerticalPx
        val scheduleItems = itemPlaceables.mapIndexed { index, itemPlaceable ->
            val item = items.elementAt(index)
            val itemPlaceableDateRange = itemDateRange(item)
            val itemRelativeDateFromMs = Duration.between(
                dateFrom,
                itemPlaceableDateRange.dateFrom
            ).toMillis()
            val itemPosX = (itemRelativeDateFromMs * scheduleWidthPx / requiredScheduleDurationMs)
                .toInt()

            val scheduleItem = PlaceableAndPosition(
                placeable = itemPlaceable,
                position = IntOffset(
                    x = itemPosX,
                    y = scheduleItemPosY
                )
            )

            scheduleItemPosY += itemPlaceable.height + itemsVerticalSpacingPx

            scheduleItem
        }

        val scheduleContentSize = IntSize(
            width = scheduleWidthPx,
            height = scheduleItems
                .maxOf { scheduleItem ->
                    scheduleItem.position.y + scheduleItem.placeable.height
                }
                .plus(itemsPaddingVerticalPx)
                .coerceAtLeast(constraints.minHeight)
        )

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

        val timeBeforeNowPAP: PlaceableAndPosition?
        val nowIndicatorPAP: PlaceableAndPosition?
        if (nowDate != null) {
            val nowPosX = calcDatePosX(
                date = nowDate,
                dateFrom = dateFrom,
                scheduleWidthPx = scheduleWidthPx,
                scheduleDurationMs = scheduleDurationMs
            )

            val shimmerPosY = scheduleItems
                .minOf { scheduleItem -> scheduleItem.position.y }

            val shimmerWidth = abs(nowPosX)
            val shimmerHeight = scheduleContentSize.height - shimmerPosY

            val timeBeforeNowShimmerPlaceable = subcompose(ScheduleSlot.TimeBeforeNowShimmer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor.copy(alpha = 0.5f))
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
            }.first()

            timeBeforeNowPAP = PlaceableAndPosition(
                placeable = timeBeforeNowShimmerPlaceable,
                position = IntOffset(
                    x = nowPosX - shimmerWidth,
                    y = shimmerPosY
                )
            )

            val nowIndicatorWidthPx = 5.dp.roundToPx()
            val nowIndicatorPosY = maxColumnTitleHeightPx
            val nowIndicatorHeight = scheduleContentSize.height - nowIndicatorPosY
            val nowIndicatorRadiusPx = (nowIndicatorWidthPx / 2f)
            val nowIndicatorLineWidth = 1.dp.toPx()
            val gapLengthPx = 3.dp.toPx()

            val nowIndicatorPlaceable = subcompose(ScheduleSlot.NowIndicator) {
                val indicatorColor = Color.Blue
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        color = indicatorColor,
                        radius = nowIndicatorRadiusPx,
                        center = center.copy(y = nowIndicatorRadiusPx)
                    )

                    drawLine(
                        color = indicatorColor,
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
            }.first()

            nowIndicatorPAP = PlaceableAndPosition(
                placeable = nowIndicatorPlaceable,
                position = IntOffset(
                    x = nowPosX - nowIndicatorWidthPx / 2,
                    y = nowIndicatorPosY
                )
            )
        } else {
            timeBeforeNowPAP = null
            nowIndicatorPAP = null
        }

        layout(scheduleContentSize.width, scheduleContentSize.height) {

            columnTitlePlaceables.forEachIndexed { index, placeable ->
                placeable.place(
                    x = columnTitleXPositions[index],
                    y = 0
                )
            }

            columnDividerPlaceables.forEachIndexed { index, placeable ->
                placeable.place(
                    x = columnXPositions[index + 1] - placeable.width / 2,
                    y = 0
                )
            }

            var y = maxColumnTitleHeightPx + itemsPaddingVerticalPx
            itemPlaceables.forEachIndexed { index, itemPlaceable ->
                itemPlaceable.place(
                    x = itemToItemPosX[items.elementAt(index)] ?: 0,
                    y = y
                )

                y += itemPlaceable.height + itemsVerticalSpacingPx
            }

            timeBeforeNowPAP?.placeable?.place(
                timeBeforeNowPAP.position
            )

            nowIndicatorPAP?.placeable?.place(
                nowIndicatorPAP.position
            )
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
        val dateFrom = remember {
            LocalDateTime.of(
                LocalDate.of(
                    2025,
                    Month.AUGUST,
                    19
                ),
                LocalTime.of(
                    0,
                    0,
                    0
                )
            )
        }

        val dateTo = remember {
            LocalDateTime.of(
                LocalDate.of(
                    2025,
                    Month.AUGUST,
                    20
                ),
                LocalTime.of(
                    0,
                    0,
                    0
                )
            )
        }

        Schedule(
            items = List(10) { it },
            dateFrom = dateFrom,
            dateTo = dateTo,
            columnDuration = Duration.ofHours(10),
            itemDateRange = { item ->
                when (item) {
                    0 -> DateRange(
                        dateFrom = dateFrom.minusHours(0),
                        dateTo = dateFrom.plusHours(6)
                    )

                    1 -> DateRange(
                        dateFrom = dateFrom.plusHours(9),
                        dateTo = dateFrom.plusHours(18)
                    )

                    2 -> DateRange(
                        dateFrom = dateFrom,
                        dateTo = dateTo.plusHours(2)
                    )

                    3 -> DateRange(
                        dateFrom = dateFrom.minusHours(1),
                        dateTo = dateTo
                    )

                    4 -> DateRange(
                        dateFrom = dateFrom.plusHours(12),
                        dateTo = dateFrom.plusHours(13)
                    )

                    else -> DateRange(
                        dateFrom = dateFrom,
                        dateTo = dateTo
                    )
                }
            },
            columnTitle = { columnDateFrom: LocalDateTime, columnDateTo: LocalDateTime ->
                val formatter = remember {
                    DateTimeFormatter.ofPattern("HH:mm")
                }

                val formattedDateFrom = remember(columnDateFrom, formatter) {
                    columnDateFrom.format(formatter)
                }

                val formattedDateTo = remember(columnDateTo, formatter) {
                    columnDateTo.format(formatter)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 4.dp,
                        end = 16.dp
                    )
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
            modifier = Modifier.fillMaxSize(),
            nowDate = remember(dateFrom) {
                dateFrom.plusHours(9)
            }
        ) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (item % 4) {
                        0 -> Color.Yellow
                        1 -> Color.Red
                        2 -> Color.Green
                        else -> Color.Magenta
                    }
                )
            ) {
                Text(
                    text = item.toString(),
                    modifier = Modifier.padding(
                        start = 10.dp,
                        top = 6.dp,
                        bottom = 6.dp
                    ),
                    fontWeight = FontWeight.W500,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color.Black
                )
            }
        }
}