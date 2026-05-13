package com.vyalov.slobodnavoznja

import java.util.Calendar
import kotlin.math.abs

enum class Direction(val configKey: String, val title: String) {
    Center("center", "из Центра"),
    Petrovaradin("petrovaradin", "из Петроварадина")
}

enum class ServiceDayType {
    Weekday,
    Weekend
}

data class DepartureTime(
    val label: String,
    val minuteOfDay: Int,
    val serviceMinute: Int
) {
    companion object {
        fun parse(label: String): DepartureTime {
            val parts = label.split(":")
            require(parts.size == 2) { "Invalid time: $label" }
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            require(hour in 0..23 && minute in 0..59) { "Invalid time: $label" }
            val minuteOfDay = hour * 60 + minute
            val serviceMinute = if (hour < LATE_NIGHT_CUTOFF_HOUR) minuteOfDay + MINUTES_PER_DAY else minuteOfDay
            return DepartureTime(label, minuteOfDay, serviceMinute)
        }
    }
}

data class Timetable(
    val validFrom: String,
    val weekday: Map<Direction, List<DepartureTime>>,
    val weekend: Map<Direction, List<DepartureTime>>
) {
    fun departures(dayType: ServiceDayType, direction: Direction): List<DepartureTime> {
        val table = if (dayType == ServiceDayType.Weekday) weekday else weekend
        return table.getValue(direction)
    }
}

data class DepartureResult(
    val departure: DepartureTime,
    val relativeDay: RelativeDay
)

enum class RelativeDay(val title: String) {
    Today("данас"),
    Tomorrow("сутра")
}

class ScheduleService(private val timetable: Timetable) {
    fun dayType(dayOfWeek: Int): ServiceDayType {
        return when (dayOfWeek) {
            Calendar.SATURDAY, Calendar.SUNDAY -> ServiceDayType.Weekend
            else -> ServiceDayType.Weekday
        }
    }

    fun nearestNow(
        currentDayOfWeek: Int,
        currentMinuteOfDay: Int,
        direction: Direction
    ): DepartureResult {
        val currentServiceDay = if (currentMinuteOfDay < LATE_NIGHT_CUTOFF_HOUR * 60) {
            previousDayOfWeek(currentDayOfWeek)
        } else {
            currentDayOfWeek
        }
        val currentServiceMinute = if (currentMinuteOfDay < LATE_NIGHT_CUTOFF_HOUR * 60) {
            currentMinuteOfDay + MINUTES_PER_DAY
        } else {
            currentMinuteOfDay
        }

        val todayDepartures = timetable.departures(dayType(currentServiceDay), direction)
        val todayMatch = todayDepartures.firstOrNull { it.serviceMinute >= currentServiceMinute }
        if (todayMatch != null) {
            val relativeDay = if (todayMatch.serviceMinute >= MINUTES_PER_DAY &&
                currentMinuteOfDay >= LATE_NIGHT_CUTOFF_HOUR * 60
            ) {
                RelativeDay.Tomorrow
            } else {
                RelativeDay.Today
            }
            return DepartureResult(todayMatch, relativeDay)
        }

        val nextServiceDay = nextDayOfWeek(currentServiceDay)
        val nextDepartures = timetable.departures(dayType(nextServiceDay), direction)
        val relativeDay = if (nextServiceDay == currentDayOfWeek) RelativeDay.Today else RelativeDay.Tomorrow
        return DepartureResult(nextDepartures.first(), relativeDay)
    }

    fun aroundTime(
        dayOfWeek: Int,
        direction: Direction,
        targetMinuteOfDay: Int,
        count: Int = 4
    ): List<DepartureTime> {
        val targetServiceMinute = if (targetMinuteOfDay < LATE_NIGHT_CUTOFF_HOUR * 60) {
            targetMinuteOfDay + MINUTES_PER_DAY
        } else {
            targetMinuteOfDay
        }
        val departures = timetable.departures(dayType(dayOfWeek), direction)
        return departures
            .sortedWith(compareBy<DepartureTime> { abs(it.serviceMinute - targetServiceMinute) }.thenBy { it.serviceMinute })
            .take(count)
            .sortedBy { it.serviceMinute }
    }

    private fun nextDayOfWeek(dayOfWeek: Int): Int {
        return if (dayOfWeek == Calendar.SATURDAY) Calendar.SUNDAY else dayOfWeek + 1
    }

    private fun previousDayOfWeek(dayOfWeek: Int): Int {
        return if (dayOfWeek == Calendar.SUNDAY) Calendar.SATURDAY else dayOfWeek - 1
    }
}

const val MINUTES_PER_DAY = 24 * 60
private const val LATE_NIGHT_CUTOFF_HOUR = 3
