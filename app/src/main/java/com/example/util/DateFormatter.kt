package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val detailFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

    fun formatNoteDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        if (diff < 0) {
            return fullDateFormat.format(Date(timestamp))
        }

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        if (minutes < 1) {
            return "Just now"
        }
        if (minutes < 60) {
            return "${minutes}m ago"
        }

        val noteCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val isSameYear = noteCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
        val isToday = isSameYear && noteCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
        
        if (isToday) {
            return "Today, " + timeFormat.format(Date(timestamp))
        }

        val isYesterday = isSameYear && (nowCal.get(Calendar.DAY_OF_YEAR) - noteCal.get(Calendar.DAY_OF_YEAR) == 1)
        if (isYesterday) {
            return "Yesterday, " + timeFormat.format(Date(timestamp))
        }

        return fullDateFormat.format(Date(timestamp))
    }

    fun formatDetailDate(timestamp: Long): String {
        return detailFormat.format(Date(timestamp))
    }
}
