package com.example.end_side.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 时间格式化工具
 */
object TimeUtils {

    /**
     * 毫秒转 "HH:mm:ss" 格式
     */
    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * 毫秒转 "X小时X分钟" 中文格式
     */
    fun formatDurationChinese(millis: Long): String {
        val totalMinutes = millis / 1000 / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}小时${minutes}分钟"
            hours > 0 -> "${hours}小时"
            minutes > 0 -> "${minutes}分钟"
            else -> "不到1分钟"
        }
    }

    /**
     * 时间戳转 "yyyy-MM-dd HH:mm" 格式
     */
    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * 时间戳转 "MM月dd日" 格式
     */
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MM月dd日", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * 获取今天的起始时间戳 (00:00:00)
     */
    fun getTodayStartMillis(): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        return sdf.parse(todayStr)?.time ?: System.currentTimeMillis()
    }
}
