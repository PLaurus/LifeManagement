package com.lauruspa.life_management.core.ui.component.schedule

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Stable
class ScheduleState(
	internal val horizontalScrollState: ScrollState
) {
	
	private var layoutInfo by mutableStateOf<LayoutInfo?>(null)
	
	val firstVisibleDate by derivedStateOf {
		calcFirstVisibleDate(
			layoutInfo = layoutInfo,
			horizontalOffsetX = horizontalScrollState.value
		)
	}
	
	internal fun updateLayoutInfo(
		layoutInfo: LayoutInfo
	) {
		this.layoutInfo = layoutInfo
	}
	
	private fun calcFirstVisibleDate(
		layoutInfo: LayoutInfo?,
		horizontalOffsetX: Int
	): LocalDateTime? {
		layoutInfo ?: return null
		val scheduleWidthPx = layoutInfo.scheduleWidthPx.takeIf { it > 0 } ?: return layoutInfo.dateFrom
		val firstVisibleDateOffsetMs = (horizontalOffsetX * layoutInfo.scheduleDurationMs) / scheduleWidthPx
		return layoutInfo.dateFrom.plus(firstVisibleDateOffsetMs, ChronoUnit.MILLIS)
	}
	
	suspend fun animateScrollToDate(
		date: LocalDateTime
	) {
		val dateX = calcDatePosX(date) ?: return
		horizontalScrollState.animateScrollTo(dateX)
	}
	
	@Suppress("unused")
	suspend fun scrollToDate(date: LocalDateTime) {
		val dateX = calcDatePosX(date) ?: return
		horizontalScrollState.scrollTo(dateX)
	}
	
	private fun calcDatePosX(date: LocalDateTime): Int? {
		val layoutInfo = this.layoutInfo ?: return null
		val scheduleDurationMs = layoutInfo.scheduleDurationMs.takeIf { it > 0 } ?: return null
		val relDateMs = Duration.between(layoutInfo.dateFrom, date)
			.toMillis()
		return ((relDateMs * layoutInfo.scheduleWidthPx) / scheduleDurationMs).toInt()
	}
	
	internal data class LayoutInfo(
		val dateFrom: LocalDateTime,
		val scheduleWidthPx: Int,
		val scheduleDurationMs: Long,
	)
}

@Composable
fun rememberScheduleState(
	horizontalScrollState: ScrollState = rememberScrollState()
): ScheduleState {
	return remember(horizontalScrollState) {
		ScheduleState(
			horizontalScrollState = horizontalScrollState
		)
	}
}