package com.lauruspa.life_management.core.utils.date

import kotlin.time.Duration

/**
 * Convert this duration to its millisecond value.
 * Positive durations are coerced at least `1`.
 */
fun Duration.toDelayMillis(): Long =
    if (this > Duration.ZERO) inWholeMilliseconds.coerceAtLeast(1) else 0

operator fun Long.plus(offset: Duration): Long {
    val offsetMs = offset.inWholeMilliseconds
    return this + offsetMs
}

operator fun Long.minus(offset: Duration): Long {
    return this + (-offset)
}