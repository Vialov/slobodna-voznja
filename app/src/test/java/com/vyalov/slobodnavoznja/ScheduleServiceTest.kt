package com.vyalov.slobodnavoznja

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ScheduleServiceTest {
    private val service = ScheduleService(testTimetable())

    @Test
    fun dayTypeUsesWeekdayForMonday() {
        assertEquals(ServiceDayType.Weekday, service.dayType(Calendar.MONDAY))
    }

    @Test
    fun dayTypeUsesWeekendForSaturday() {
        assertEquals(ServiceDayType.Weekend, service.dayType(Calendar.SATURDAY))
    }

    @Test
    fun nearestNowReturnsFirstDepartureBeforeScheduleStarts() {
        val result = service.nearestNow(Calendar.MONDAY, minutes("03:30"), Direction.Center)

        assertEquals("04:00", result.departure.label)
        assertEquals(RelativeDay.Today, result.relativeDay)
    }

    @Test
    fun nearestNowReturnsNextDepartureBetweenBuses() {
        val result = service.nearestNow(Calendar.MONDAY, minutes("10:57"), Direction.Center)

        assertEquals("11:00", result.departure.label)
        assertEquals(RelativeDay.Today, result.relativeDay)
    }

    @Test
    fun nearestNowFindsLateNightDepartureAfterLastRegularBus() {
        val result = service.nearestNow(Calendar.MONDAY, minutes("23:50"), Direction.Petrovaradin)

        assertEquals("00:31", result.departure.label)
        assertEquals(RelativeDay.Tomorrow, result.relativeDay)
    }

    @Test
    fun nearestNowFallsBackToMorningServiceAfterLateNightDeparture() {
        val result = service.nearestNow(Calendar.MONDAY, minutes("00:40"), Direction.Petrovaradin)

        assertEquals("06:31", result.departure.label)
        assertEquals(RelativeDay.Today, result.relativeDay)
    }

    @Test
    fun aroundTimeReturnsFourClosestDeparturesChronologically() {
        val results = service.aroundTime(Calendar.MONDAY, Direction.Center, minutes("10:00"))

        assertEquals(listOf("09:36", "09:41", "10:11", "10:26"), results.map { it.label })
    }

    @Test
    fun aroundTimeFillsFromStartWhenTargetIsNearFirstDeparture() {
        val results = service.aroundTime(Calendar.SATURDAY, Direction.Center, minutes("04:50"))

        assertEquals(listOf("05:00", "06:00", "06:26", "07:00"), results.map { it.label })
    }

    @Test
    fun nextDeparturesReturnsFourUpcomingDeparturesChronologically() {
        val results = service.nextDepartures(Calendar.MONDAY, Direction.Center, minutes("10:00"))

        assertEquals(listOf("10:11", "10:26", "11:00", "23:30"), results.map { it.departure.label })
        assertEquals(listOf(11, 26, 60, 810), results.map { it.waitMinutes })
    }

    @Test
    fun nextDeparturesFindsLateNightDepartureAfterLastRegularBus() {
        val results = service.nextDepartures(Calendar.MONDAY, Direction.Petrovaradin, minutes("23:50"))

        assertEquals(listOf("00:31", "06:31", "10:09", "10:30"), results.map { it.departure.label })
        assertEquals(listOf(41, 401, 619, 640), results.map { it.waitMinutes })
        assertEquals(RelativeDay.Tomorrow, results.first().relativeDay)
    }

    @Test
    fun nextDeparturesUsesPreviousServiceDayForAfterMidnightTarget() {
        val results = service.nextDepartures(Calendar.TUESDAY, Direction.Petrovaradin, minutes("00:15"))

        assertEquals(listOf("00:31", "06:31", "10:09", "10:30"), results.map { it.departure.label })
        assertEquals(listOf(16, 376, 594, 615), results.map { it.waitMinutes })
    }

    @Test
    fun nextDeparturesFillsFromNextServiceDayWhenNeeded() {
        val results = service.nextDepartures(Calendar.SATURDAY, Direction.Center, minutes("23:40"))

        assertEquals(listOf("05:00", "06:00", "06:26", "07:00"), results.map { it.departure.label })
        assertEquals(listOf(320, 380, 406, 440), results.map { it.waitMinutes })
    }

    @Test
    fun browseDeparturesReturnsWholeServiceDayWithNearestIndex() {
        val result = service.browseDepartures(Calendar.MONDAY, Direction.Center, minutes("10:00"))

        assertEquals(
            listOf("04:00", "09:36", "09:41", "10:11", "10:26", "11:00", "23:30"),
            result.departures.map { it.departure.label }
        )
        assertEquals(3, result.nearestIndex)
        assertEquals(listOf(-360, -24, -19, 11), result.departures.take(4).map { it.waitMinutes })
    }

    @Test
    fun browseDeparturesSelectsLastSameDayDepartureAfterLastDeparture() {
        val result = service.browseDepartures(Calendar.SATURDAY, Direction.Center, minutes("23:40"))

        assertEquals(listOf("05:00", "06:00", "06:26", "07:00", "23:30"), result.departures.map { it.departure.label })
        assertEquals(4, result.nearestIndex)
        assertEquals(-10, result.departures.last().waitMinutes)
    }

    @Test
    fun browseDeparturesCanUseExplicitServiceDayType() {
        val result = service.browseDepartures(ServiceDayType.Weekend, Direction.Petrovaradin, minutes("10:00"))

        assertEquals(listOf("06:31", "10:54", "23:40", "00:31"), result.departures.map { it.departure.label })
        assertEquals(1, result.nearestIndex)
    }

    private fun minutes(label: String): Int {
        val parts = label.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
}

private fun testTimetable(): Timetable {
    fun departures(vararg labels: String) = labels.map { DepartureTime.parse(it) }.sortedBy { it.serviceMinute }

    return Timetable(
        validFrom = "2026-01-01",
        weekday = mapOf(
            Direction.Center to departures(
                "04:00", "09:36", "09:41", "10:11", "10:26", "11:00", "23:30"
            ),
            Direction.Petrovaradin to departures(
                "06:31", "10:09", "10:30", "23:40", "00:31"
            )
        ),
        weekend = mapOf(
            Direction.Center to departures("05:00", "06:00", "06:26", "07:00", "23:30"),
            Direction.Petrovaradin to departures("06:31", "10:54", "23:40", "00:31")
        )
    )
}
