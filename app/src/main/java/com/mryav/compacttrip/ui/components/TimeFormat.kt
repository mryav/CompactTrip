package com.mryav.compacttrip.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SYDNEY: ZoneId = ZoneId.of("Australia/Sydney")
private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH)

/** "10:58pm", Sydney time. */
fun clockTime(epochMillis: Long): String =
    CLOCK.format(Instant.ofEpochMilli(epochMillis).atZone(SYDNEY)).lowercase(Locale.ENGLISH)

/** Whole minutes until [departsAt], rounded up, never negative. */
fun minutesUntil(departsAt: Long, now: Long): Int =
    ((departsAt - now + 59_999) / 60_000).coerceAtLeast(0).toInt()

/** Big number + small unit for the countdown box. Null number means "Now". */
data class Countdown(val number: String, val unit: String?)

/** Under an hour: "12 min". Then whole hours only ("1hr", "2hr"), then whole days ("1d", "2d"). */
fun countdown(minutes: Int): Countdown = when {
    minutes <= 0 -> Countdown("Now", null)
    minutes < 60 -> Countdown(minutes.toString(), "min")
    minutes < 24 * 60 -> Countdown((minutes / 60).toString(), "hr")
    else -> Countdown((minutes / (24 * 60)).toString(), "d")
}

/** "just now", "30s ago", "2 min ago". */
fun updatedAgo(updatedAt: Long, now: Long): String {
    val seconds = ((now - updatedAt) / 1000).coerceAtLeast(0)
    return when {
        seconds < 10 -> "just now"
        seconds < 60 -> "${seconds / 10 * 10}s ago"
        else -> "${seconds / 60} min ago"
    }
}
