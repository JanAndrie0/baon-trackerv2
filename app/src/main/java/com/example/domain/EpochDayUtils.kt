package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object EpochDayUtils {

    private const val MILLIS_PER_DAY = 86_400_000L

    fun todayEpochDay(nowMillis: Long = System.currentTimeMillis()): Long {
        return millisToEpochDay(nowMillis)
    }

    fun millisToEpochDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tzOffset = cal.timeZone.getOffset(cal.timeInMillis)
        return (cal.timeInMillis + tzOffset) / MILLIS_PER_DAY
    }

    fun epochDayStartMillis(epochDay: Long): Long {
        val cal = epochDayToCalendar(epochDay)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun epochDayEndMillis(epochDay: Long): Long {
        val cal = epochDayToCalendar(epochDay)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun epochDayNoonMillis(epochDay: Long): Long {
        val cal = epochDayToCalendar(epochDay)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun epochDayToCalendar(epochDay: Long): Calendar {
        val utcMillis = epochDay * MILLIS_PER_DAY
        val cal = Calendar.getInstance()
        val tzOffset = cal.timeZone.getOffset(utcMillis)
        cal.timeInMillis = utcMillis - tzOffset
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    fun formatShortDate(epochDay: Long): String {
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        return sdf.format(Date(epochDayNoonMillis(epochDay)))
    }

    fun formatFullDate(epochDay: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(epochDayNoonMillis(epochDay)))
    }

    fun formatDayHeader(epochDay: Long, today: Long = todayEpochDay()): String {
        return when (epochDay) {
            today -> "Today"
            today - 1L -> "Yesterday"
            else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                .format(Date(epochDayNoonMillis(epochDay)))
        }
    }

    fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatWeekdayShort(epochDay: Long): String {
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        return sdf.format(Date(epochDayNoonMillis(epochDay)))
    }

    fun formatMonthYear(year: Int, monthZeroBased: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    fun currentYearMonth(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
    }

    fun calendarDaysForMonth(year: Int, monthZeroBased: Int): List<Pair<Int, Long>?> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sun .. 7 = Sat
        val leadingBlanks = firstDayOfWeek - Calendar.SUNDAY
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val result = mutableListOf<Pair<Int, Long>?>()
        repeat(leadingBlanks) { result.add(null) }
        for (day in 1..maxDay) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            result.add(day to millisToEpochDay(cal.timeInMillis))
        }
        return result
    }

    fun suggestedNextBaonEpochDay(
        frequency: String,
        today: Long = todayEpochDay()
    ): Long {
        return when (frequency.uppercase()) {
            "DAILY" -> today + 1L
            "WEEKLY" -> today + 7L
            "MONTHLY" -> {
                val cal = epochDayToCalendar(today)
                cal.add(Calendar.MONTH, 1)
                millisToEpochDay(cal.timeInMillis)
            }
            else -> today + 7L
        }
    }
}
