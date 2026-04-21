package com.example.end_side.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import com.example.end_side.ui.study.StudySessionActivity
import com.example.end_side.util.NotificationHelper
import com.example.end_side.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 学习计时前台服务
 * 在通知栏显示学习计时，确保后台不被系统回收
 * 使用 Foreground Service + 通知栏显示
 */
class StudyTimerService : Service() {

    companion object {
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
    }

    private val binder = TimerBinder()
    private var startTimeMillis: Long = 0L
    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    // 回调：通知 Activity 更新计时
    var onTickListener: ((elapsed: Long, formatted: String) -> Unit)? = null

    inner class TimerBinder : Binder() {
        fun getService(): StudyTimerService = this@StudyTimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer()
            ACTION_STOP -> stopTimer()
        }
        return START_NOT_STICKY
    }

    private fun startTimer() {
        startTimeMillis = SystemClock.elapsedRealtime()

        // 启动前台服务
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, StudySessionActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationHelper.buildTimerNotification(
            this, "00:00:00", contentIntent
        )
        startForeground(NotificationHelper.NOTIFICATION_ID_TIMER, notification)

        // 启动协程计时
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                val elapsed = SystemClock.elapsedRealtime() - startTimeMillis
                val formatted = TimeUtils.formatDuration(elapsed)

                // 更新通知
                val updatedNotification = NotificationHelper.buildTimerNotification(
                    this@StudyTimerService, formatted
                )
                try {
                    NotificationManagerCompat.from(this@StudyTimerService)
                        .notify(NotificationHelper.NOTIFICATION_ID_TIMER, updatedNotification)
                } catch (e: SecurityException) {
                    // 通知权限未授予
                }

                // 回调到 Activity
                onTickListener?.invoke(elapsed, formatted)

                delay(1000L)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun getElapsedMillis(): Long {
        return if (startTimeMillis > 0) {
            SystemClock.elapsedRealtime() - startTimeMillis
        } else 0L
    }

    override fun onDestroy() {
        timerJob?.cancel()
        super.onDestroy()
    }
}
