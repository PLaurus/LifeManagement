package com.lauruspa.life_management.core.utils.date

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date

fun getLocalZoneOffset(): ZoneOffset {
    val instant = Instant.now()
    val systemZone = ZoneId.systemDefault()
    return systemZone.rules.getOffset(instant)
}

fun getYearMonthFromDate(date: Date): YearMonth {
    val dateTime = date.toInstant().atOffset(getLocalZoneOffset())
    return YearMonth.from(dateTime)
}