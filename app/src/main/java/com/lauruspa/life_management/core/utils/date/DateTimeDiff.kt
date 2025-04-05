package com.lauruspa.life_management.core.utils.date

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class DateTimeDiff(
    val years: Long,
    val months: Long,
    val weeks: Long,
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val milliseconds: Long,
    val microseconds: Long,
    val isPositive: Boolean
) {

    companion object {
        fun from(
            firstMs: Long,
            secondMs: Long
        ): DateTimeDiff {
            val timeZone = ZoneId.systemDefault()

            val firstInstant = Instant.ofEpochMilli(firstMs)
            val firstLocalDateTime = LocalDateTime.ofInstant(firstInstant, timeZone)

            val secondInstant = Instant.ofEpochMilli(secondMs)
            val secondLocalDateTime = LocalDateTime.ofInstant(secondInstant, timeZone)

            return from(firstLocalDateTime, secondLocalDateTime)
        }

        fun from(
            first: LocalDateTime,
            second: LocalDateTime
        ): DateTimeDiff {
            val isPositive = first.isBefore(second)
            val start: LocalDateTime
            val end: LocalDateTime

            if (isPositive) {
                start = first
                end = second
            } else {
                start = second
                end = first
            }

            var tempDateTime = LocalDateTime.from(start)

            val years = tempDateTime.until(end, ChronoUnit.YEARS)
            tempDateTime = tempDateTime.plusYears(years)
            val months = tempDateTime.until(end, ChronoUnit.MONTHS)
            tempDateTime = tempDateTime.plusMonths(months)
            val weeks = tempDateTime.until(end, ChronoUnit.WEEKS)
            tempDateTime = tempDateTime.plusWeeks(weeks)
            val days = tempDateTime.until(end, ChronoUnit.DAYS)
            tempDateTime = tempDateTime.plusDays(days)
            val hours = tempDateTime.until(end, ChronoUnit.HOURS)
            tempDateTime = tempDateTime.plusHours(hours)
            val minutes = tempDateTime.until(end, ChronoUnit.MINUTES)
            tempDateTime = tempDateTime.plusMinutes(minutes)
            val seconds = tempDateTime.until(end, ChronoUnit.SECONDS)
            tempDateTime = tempDateTime.plusSeconds(seconds)
            val millis = tempDateTime.until(end, ChronoUnit.MILLIS)
            tempDateTime = tempDateTime.plus(millis, ChronoUnit.MILLIS)
            val micros = tempDateTime.until(end, ChronoUnit.MICROS)

            return DateTimeDiff(
                years = years,
                months = months,
                weeks = weeks,
                days = days,
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                milliseconds = millis,
                microseconds = micros,
                isPositive = isPositive
            )
        }
    }
}