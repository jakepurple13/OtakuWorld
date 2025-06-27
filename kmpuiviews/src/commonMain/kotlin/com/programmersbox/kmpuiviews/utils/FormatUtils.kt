package com.programmersbox.kmpuiviews.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val DateFormatItem = LocalDate.Format {
    monthNumber()
    char('/')
    day(padding = Padding.ZERO)
    char('/')
    year()
}

private val Format24 = LocalTime.Format {
    hour()
    char(':')
    minute()
}

private val Format12 = LocalTime.Format {
    amPmHour()
    char(':')
    minute()
    char(' ')
    amPmMarker("AM", "PM")
}

internal fun DateTimeFormatItem(isUsing24HourTime: Boolean) = LocalDateTime.Format {
    date(DateFormatItem)
    chars(", ")
    time(if (isUsing24HourTime) Format24 else Format12)
}

@OptIn(ExperimentalTime::class)
fun Long.toLocalDateTime() = Instant
    .fromEpochMilliseconds(this)
    .toLocalDateTime(TimeZone.currentSystemDefault())