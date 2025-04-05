package com.lauruspa.life_management.core.utils.date

object DateTimeDiffFormatter {
    fun format(
        dateTimeDiff: DateTimeDiff,
        yearsText: ((Long, isPositive: Boolean) -> String?)? = null,
        monthsText: ((Long, isPositive: Boolean) -> String?)? = null,
        weeksText: ((Long, isPositive: Boolean) -> String?)? = null,
        daysText: ((Long, isPositive: Boolean) -> String?)? = null,
        hoursText: ((Long, isPositive: Boolean) -> String?)? = null,
        minutesText: ((Long, isPositive: Boolean) -> String?)? = null,
        secondsText: ((Long, isPositive: Boolean) -> String?)? = null,
        millisecondsText: ((Long, isPositive: Boolean) -> String?)? = null,
        microsecondsText: ((Long, isPositive: Boolean) -> String?)? = null,
        excludeZeros: Boolean = true,
        finalFormat: Format = DefaultFormat,
    ): String {

        val excl: (Long, ((Long) -> String?)) -> String? = { value, builder ->
            if (excludeZeros && value <= 0) {
                null
            } else {
                builder(value)
            }
        }

        val formatData = FormatData(
            yearsText = excl(dateTimeDiff.years) { yearsText?.invoke(it, dateTimeDiff.isPositive) },
            monthsText = excl(dateTimeDiff.months) {
                monthsText?.invoke(it, dateTimeDiff.isPositive)
            },
            weeksText = excl(dateTimeDiff.weeks) { weeksText?.invoke(it, dateTimeDiff.isPositive) },
            daysText = excl(dateTimeDiff.days) { daysText?.invoke(it, dateTimeDiff.isPositive) },
            hoursText = excl(dateTimeDiff.hours) { hoursText?.invoke(it, dateTimeDiff.isPositive) },
            minutesText = excl(dateTimeDiff.minutes) {
                minutesText?.invoke(it, dateTimeDiff.isPositive)
            },
            secondsText = excl(dateTimeDiff.seconds) {
                secondsText?.invoke(it, dateTimeDiff.isPositive)
            },
            millisecondsText = excl(dateTimeDiff.milliseconds) {
                millisecondsText?.invoke(it, dateTimeDiff.isPositive)
            },
            microsecondsText = excl(dateTimeDiff.microseconds) {
                microsecondsText?.invoke(it, dateTimeDiff.isPositive)
            },
        )

        return finalFormat.format(formatData)
    }

    fun interface Format {
        fun format(data: FormatData): String
    }

    internal object DefaultFormat : Format {
        private const val SPACE = " "
        override fun format(data: FormatData): String {
            val result = StringBuilder()

            if (data.yearsText != null) {
                result.append(data.yearsText)
                result.append(SPACE)
            }

            if (data.monthsText != null) {
                result.append(data.monthsText)
                result.append(SPACE)
            }

            if (data.weeksText != null) {
                result.append(data.weeksText)
                result.append(SPACE)
            }

            if (data.daysText != null) {
                result.append(data.daysText)
                result.append(SPACE)
            }

            if (data.hoursText != null) {
                result.append(data.hoursText)
                result.append(SPACE)
            }

            if (data.minutesText != null) {
                result.append(data.minutesText)
                result.append(SPACE)
            }

            if (data.secondsText != null) {
                result.append(data.secondsText)
                result.append(SPACE)
            }

            if (data.millisecondsText != null) {
                result.append(data.millisecondsText)
                result.append(SPACE)
            }

            if (data.microsecondsText != null) {
                result.append(data.microsecondsText)
                result.append(SPACE)
            }

            if (result.isNotEmpty()) result.removeSuffix(SPACE)

            return result.toString()
        }
    }

    data class FormatData(
        val yearsText: String?,
        val monthsText: String?,
        val weeksText: String?,
        val daysText: String?,
        val hoursText: String?,
        val minutesText: String?,
        val secondsText: String?,
        val millisecondsText: String?,
        val microsecondsText: String?
    )
}