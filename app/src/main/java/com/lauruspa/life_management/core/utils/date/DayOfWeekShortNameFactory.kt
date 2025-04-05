package com.lauruspa.life_management.core.utils.date

import android.content.Context
import com.lauruspa.life_management.R
import java.time.DayOfWeek

object DayOfWeekShortNameFactory {
    fun create(context: Context, dayOfWeek: DayOfWeek): String {
        val resId = when (dayOfWeek) {
            DayOfWeek.MONDAY -> R.string.week_day_monday_short_lowercase
            DayOfWeek.TUESDAY -> R.string.week_day_tuesday_short_lowercase
            DayOfWeek.WEDNESDAY -> R.string.week_day_wednesday_short_lowercase
            DayOfWeek.THURSDAY -> R.string.week_day_thursday_short_lowercase
            DayOfWeek.FRIDAY -> R.string.week_day_friday_short_lowercase
            DayOfWeek.SATURDAY -> R.string.week_day_saturday_short_lowercase
            DayOfWeek.SUNDAY -> R.string.week_day_sunday_short_lowercase
        }

        return context.getString(resId)
    }
}