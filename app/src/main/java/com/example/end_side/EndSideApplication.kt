package com.example.end_side

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate

class EndSideApplication : Application() {

    companion object {
        const val CHANNEL_STUDY_TIMER = "study_timer_channel"
        const val CHANNEL_ALERT = "study_alert_channel"
        private const val PREFS_NAME = "ai_study_settings"
        private const val KEY_DARK_MODE = "dark_mode_enabled"
    }

    override fun onCreate() {
        // 在 super.onCreate() 之前应用已保存的深色/浅色主题
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val timerChannel = NotificationChannel(
            CHANNEL_STUDY_TIMER,
            "学习计时",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示学习计时状态"
        }

        val alertChannel = NotificationChannel(
            CHANNEL_ALERT,
            "学习提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "坐姿和专注力提醒"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(timerChannel)
        manager.createNotificationChannel(alertChannel)
    }
}
