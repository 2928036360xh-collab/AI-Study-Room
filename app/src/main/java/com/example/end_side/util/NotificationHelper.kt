package com.example.end_side.util

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.end_side.R

/**
 * 通知构建与提示音播放工具
 * 封装学习计时器通知和异常提醒通知
 */
object NotificationHelper {

    const val CHANNEL_STUDY_TIMER = "study_timer_channel"
    const val CHANNEL_ALERT = "study_alert_channel"
    const val NOTIFICATION_ID_TIMER = 1001
    const val NOTIFICATION_ID_ALERT = 1002

    /**
     * 构建学习计时前台服务通知
     */
    fun buildTimerNotification(
        context: Context,
        elapsedTime: String,
        contentIntent: PendingIntent? = null
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_STUDY_TIMER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("AI 自习室 - 学习中")
            .setContentText("已学习 $elapsedTime")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .apply {
                if (contentIntent != null) {
                    setContentIntent(contentIntent)
                }
            }
            .build()
    }

    /**
     * 构建坐姿/专注力异常提醒通知
     */
    fun buildAlertNotification(
        context: Context,
        title: String,
        message: String
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
    }

    /**
     * 播放提示音（用于坐姿/专注力异常提醒）
     * 使用 MediaPlayer API 播放系统默认提示音
     */
    fun playAlertSound(context: Context) {
        try {
            val uri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.prepare()
            mediaPlayer.start()
            // 播放完毕释放资源
            mediaPlayer.setOnCompletionListener { mp ->
                mp.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
