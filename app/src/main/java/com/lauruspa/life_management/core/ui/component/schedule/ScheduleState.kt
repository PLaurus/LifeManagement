package com.lauruspa.life_management.core.ui.component.schedule

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Stable
class ScheduleState(
	private val initialFirstVisibleDate: LocalDateTime?
) {
	private var isHorizontalScrollStateInitialized = false
	internal val horizontalScrollState = ScrollState(initial = 0)
	
	@Suppress("MemberVisibilityCanBePrivate")
	val isScrollInProgress: Boolean
		get() = horizontalScrollState.isScrollInProgress
	
	private var layoutInfo by mutableStateOf<LayoutInfo?>(null)
	
	private val currentDateState by derivedStateOf(structuralEqualityPolicy()) {
		calcFirstVisibleDate(
			layoutInfo = layoutInfo,
			horizontalOffsetX = horizontalScrollState.value
		)
	}
	
	val currentDate: LocalDateTime? = initialFirstVisibleDate
		get() {
			return if (layoutInfo != null && isHorizontalScrollStateInitialized) {
				currentDateState
			} else {
				field
			}
		}
	
	private var programmaticScrollTargetDate: LocalDateTime? by mutableStateOf(null)
	val targetDate: LocalDateTime? by derivedStateOf(structuralEqualityPolicy()) {
		if (!isScrollInProgress) {
			currentDate
		} else if (programmaticScrollTargetDate != null) {
			programmaticScrollTargetDate
		} else {
			currentDate
		}
	}
	
	internal fun updateLayoutInfo(
		layoutInfo: LayoutInfo
	) {
		if (layoutInfo == this.layoutInfo) return
		val date = if (isHorizontalScrollStateInitialized) {
			currentDate
		} else {
			initialFirstVisibleDate
		}
		
		this.layoutInfo = layoutInfo
		
		initializeHorizontalScrollState(
			layoutInfo = layoutInfo,
			date = date
		)
	}
	
	private fun initializeHorizontalScrollState(
		layoutInfo: LayoutInfo,
		date: LocalDateTime?
	) {
		val initialScrollX = if (date != null) {
			calcDatePosX(
				date = date,
				layoutInfo = layoutInfo
			) ?: 0
		} else {
			0
		}
		
		horizontalScrollState.dispatchRawDelta(
			delta = initialScrollX.toFloat() - horizontalScrollState.value
		)
		isHorizontalScrollStateInitialized = true
	}
	
	private fun calcFirstVisibleDate(
		layoutInfo: LayoutInfo?,
		horizontalOffsetX: Int
	): LocalDateTime? {
		layoutInfo ?: return null
		val scheduleWidthPx =
			layoutInfo.scheduleWidthPx.takeIf { it > 0 } ?: return layoutInfo.dateFrom
		val firstVisibleDateOffsetMs =
			(horizontalOffsetX * layoutInfo.scheduleDurationMs) / scheduleWidthPx
		return layoutInfo.dateFrom.plus(firstVisibleDateOffsetMs, ChronoUnit.MILLIS)
	}
	
	suspend fun animateScrollToDate(
		date: LocalDateTime
	) {
		programmaticScrollTargetDate = date
		val dateX = calcDatePosX(
			date = date,
			layoutInfo = layoutInfo ?: return
		) ?: return
		horizontalScrollState.animateScrollTo(dateX)

//		horizontalScrollState.scroll {
//			snapshotFlow { layoutInfo }
//				.mapLatest { layoutInfo ->
//					calcDatePosX(
//						date = date,
//						layoutInfo = layoutInfo ?: return@mapLatest null
//					) ?: return@mapLatest null
//				}
//				.mapLatest { posX ->
//					var previousValue = 0f
//					if (posX != null) {
//						animate(
//							0f,
//							(posX - horizontalScrollState.value).toFloat(),
//							animationSpec = SpringSpec()
//						) { currentValue, _ ->
//							previousValue += scrollBy(currentValue - previousValue)
//						}
//					}
//
//					previousValue
//				}
//				.first()
//		}
		programmaticScrollTargetDate = null
	}
	
	@Suppress("unused")
	suspend fun scrollToDate(date: LocalDateTime) {
		programmaticScrollTargetDate = date
		val dateX = calcDatePosX(
			date = date,
			layoutInfo = layoutInfo ?: return
		) ?: return
		horizontalScrollState.scrollTo(dateX)
	}
	
	private fun calcDatePosX(
		date: LocalDateTime,
		layoutInfo: LayoutInfo
	): Int? {
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
	initialFirstVisibleDate: LocalDateTime? = null
): ScheduleState {
	return remember {
		ScheduleState(
			initialFirstVisibleDate = initialFirstVisibleDate
		)
	}
}