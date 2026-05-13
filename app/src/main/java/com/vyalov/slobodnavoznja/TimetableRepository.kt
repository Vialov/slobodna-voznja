package com.vyalov.slobodnavoznja

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class TimetableRepository(private val context: Context) {
    fun load(): Timetable {
        val json = context.assets.open("timetable.json")
            .bufferedReader()
            .use { it.readText() }
        return parse(json)
    }

    fun parse(json: String): Timetable {
        val root = JSONObject(json)
        return Timetable(
            validFrom = root.getString("validFrom"),
            weekday = parseServiceDay(root.getJSONObject("weekday")),
            weekend = parseServiceDay(root.getJSONObject("weekend"))
        )
    }

    private fun parseServiceDay(json: JSONObject): Map<Direction, List<DepartureTime>> {
        return Direction.values().associateWith { direction ->
            json.getJSONArray(direction.configKey).toDepartures()
        }
    }

    private fun JSONArray.toDepartures(): List<DepartureTime> {
        return List(length()) { index -> DepartureTime.parse(getString(index)) }
            .sortedBy { it.serviceMinute }
    }
}
