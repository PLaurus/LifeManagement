package com.lauruspa.life_management.core.ui.component.schedule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Stable
class ScheduleState {
	
	private var dateFrom: LocalDateTime? = null
	private var scheduleWidthPx: Int = 0
	private var scheduleDurationMs: Long = 0
	private var horizontalOffsetPx: Int = 0
	private var firstVisibleDateState by mutableStateOf<LocalDateTime?>(null)
	
	val firstVisibleDate
		get() = firstVisibleDateState
	
	internal fun updateHorizontalOffsetPx(offsetPx: Int) {
		horizontalOffsetPx = offsetPx
		updateFirstVisibleDate()
	}
	
	internal fun updateMeasuredValues(
		dateFrom: LocalDateTime,
		scheduleWidthPx: Int,
		scheduleDurationMs: Long,
	) {
		this.dateFrom = dateFrom
		this.scheduleWidthPx = scheduleWidthPx
		this.scheduleDurationMs = scheduleDurationMs
		updateFirstVisibleDate()
	}
	
	private fun updateFirstVisibleDate() {
		firstVisibleDateState = calcFirstVisibleDate()
	}
	
	private fun calcFirstVisibleDate(): LocalDateTime? {
		val dateFrom = dateFrom ?: return null
		val scheduleWidthPx = scheduleWidthPx.takeIf { it > 0 } ?: return dateFrom
		val firstVisibleDateOffsetMs = (horizontalOffsetPx * scheduleDurationMs / scheduleWidthPx)
		return dateFrom.plus(firstVisibleDateOffsetMs, ChronoUnit.MILLIS)
	}
}

@Composable
fun rememberScheduleState(): ScheduleState {
	return remember { ScheduleState() }
}