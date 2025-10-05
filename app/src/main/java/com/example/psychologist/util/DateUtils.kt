package com.example.psychologist.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun formatMessageTime(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        val currentCalendar = Calendar.getInstance()

        return if (calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == currentCalendar.get(Calendar.DAY_OF_YEAR)) {
            // 今天
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        } else if (calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) {
            // 今年
            SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
        } else {
            // 其他年份
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun formatConversationTime(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        val currentCalendar = Calendar.getInstance()

        return if (calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == currentCalendar.get(Calendar.DAY_OF_YEAR)) {
            // 今天
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        } else if (calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) {
            // 今年
            SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timestamp))
        } else {
            // 其他年份
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
        }
    }
}