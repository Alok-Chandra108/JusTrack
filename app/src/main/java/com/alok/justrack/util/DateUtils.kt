package com.alok.justrack.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {
    private val tmdbDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH)

    fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            LocalDate.parse(dateStr, tmdbDateFormatter)
        } catch (e: Exception) {
            try {
                LocalDate.parse(dateStr, displayDateFormatter)
            } catch (e2: Exception) {
                null
            }
        }
    }

    /**
     * Returns the number of days until the given air date.
     * Returns 0 if it airs today.
     * Returns a negative value if it aired in the past.
     * Returns null if the date is invalid or null.
     */
    fun getDaysUntil(airDate: String?): Long? {
        val targetDate = parseDate(airDate) ?: return null
        val today = LocalDate.now()
        return ChronoUnit.DAYS.between(today, targetDate)
    }
}
