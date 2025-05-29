package com.lauruspa.life_management.core.ui.component.schedule

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.layout.OnRemeasuredModifier
import androidx.compose.ui.unit.IntSize
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Stable
class ScheduleState(
	initialDate: LocalDateTime?
) {
	private var initialDateState by mutableStateOf(initialDate)
	internal val horizontalScrollState = ScrollState(initial = 0)
	
	private var needsRemeasure by mutableStateOf(true)
	internal val onRemeasuredModifier = object : OnRemeasuredModifier {
		override fun onRemeasured(size: IntSize) {
			if (needsRemeasure) {
				val layoutInfo = layoutInfo
				if (layoutInfo != null) {
					initializeHorizontalScrollState(
						layoutInfo = layoutInfo,
						date = currentDate
					)
				}
			}
			needsRemeasure = false
		}
	}
	
	@Suppress("MemberVisibilityCanBePrivate")
	val isScrollInProgress: Boolean
		get() = horizontalScrollState.isScrollInProgress
	
	private var layoutInfo by mutableStateOf<LayoutInfo?>(
		value = null,
		policy = neverEqualPolicy()
	)
	
	private val scrollDate by derivedStateOf(structuralEqualityPolicy()) {
		calcFirstVisibleDate(
			layoutInfo = layoutInfo,
			horizontalOffsetX = horizontalScrollState.value
		)
	}
	
	val currentDate: LocalDateTime? by derivedStateOf(structuralEqualityPolicy()) {
		if (!needsRemeasure) {
			scrollDate
		} else {
			this.initialDateState
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
		layoutInfo: LayoutInfo,
	) {
		if (layoutInfo == this.layoutInfo) return
		initialDateState = currentDate
		this.needsRemeasure = true
		this.layoutInfo = layoutInfo
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
		horizontalScrollState.scroll {
			programmaticScrollTargetDate = date
			val dateX = calcDatePosX(
				date = date,
				layoutInfo = layoutInfo ?: return@scroll
			) ?: return@scroll
			
			try {
				var previousValue = 0f
				animate(
					0f,
					(dateX - horizontalScrollState.value).toFloat(),
					animationSpec = SpringSpec()
				) { currentValue, _ ->
					previousValue += scrollBy(currentValue - previousValue)
				}
			} finally {
				programmaticScrollTargetDate = null
			}
		}
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
			initialDate = initialFirstVisibleDate
		)
	}
}