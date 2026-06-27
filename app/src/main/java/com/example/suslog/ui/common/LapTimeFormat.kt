package com.example.suslog.ui.common

import java.util.Locale
import kotlin.math.roundToLong

fun parseLapTimeMillis(text: String): Long? {
    val value = text.trim()
    if (value.isEmpty()) return null

    val parts = value.split(":")
    val totalSeconds = when (parts.size) {
        1 -> parts.first().toDoubleOrNull()
        2 -> {
            val minutes = parts[0].toLongOrNull() ?: return null
            val seconds = parts[1].toDoubleOrNull() ?: return null
            if (seconds < 0.0 || seconds >= 60.0 || minutes < 0) return null
            minutes * 60.0 + seconds
        }
        else -> return null
    } ?: return null

    if (totalSeconds < 0.0) return null
    return (totalSeconds * 1000.0).roundToLong()
}

fun Long.formatLapTime(): String {
    val minutes = this / 60_000
    val seconds = (this % 60_000) / 1000
    val millis = this % 1000

    return if (minutes > 0) {
        String.format(Locale.US, "%d:%02d.%03d", minutes, seconds, millis)
    } else {
        String.format(Locale.US, "%d.%03d", seconds, millis)
    }
}
