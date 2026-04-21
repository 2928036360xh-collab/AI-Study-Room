package com.example.end_side.data.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.example.end_side.data.AppDatabase
import kotlinx.coroutines.runBlocking

/**
 * StudyDataProvider - ContentProvider 提供学习数据给外部应用
 * 使用 UriMatcher 路由不同数据表
 * 必须在 AndroidManifest.xml 中 <provider> 注册并配置 android:authorities
 */
class StudyDataProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.example.end_side.provider"
        val BASE_URI: Uri = Uri.parse("content://$AUTHORITY")

        private const val CODE_SESSIONS = 1
        private const val CODE_SESSION_BY_ID = 2
        private const val CODE_SNAPSHOTS = 3

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "sessions", CODE_SESSIONS)
            addURI(AUTHORITY, "sessions/#", CODE_SESSION_BY_ID)
            addURI(AUTHORITY, "snapshots/#", CODE_SNAPSHOTS)
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri, projection: Array<out String>?,
        selection: String?, selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val ctx = context ?: return null
        val db = AppDatabase.getInstance(ctx)

        return when (uriMatcher.match(uri)) {
            CODE_SESSIONS -> {
                // 返回所有学习会话概要（ContentProvider 是同步的，需 runBlocking）
                val sessions = runBlocking { db.studySessionDao().getAll() }
                val cursor = MatrixCursor(
                    arrayOf("id", "start_time", "duration", "avg_posture", "avg_focus", "alert_count")
                )
                for (s in sessions) {
                    cursor.addRow(arrayOf(
                        s.id, s.startTime, s.duration,
                        s.avgPostureScore, s.avgFocusScore, s.alertCount
                    ))
                }
                cursor.setNotificationUri(ctx.contentResolver, uri)
                cursor
            }
            CODE_SESSION_BY_ID -> {
                val id = ContentUris.parseId(uri)
                val session = runBlocking { db.studySessionDao().getById(id) }
                val cursor = MatrixCursor(
                    arrayOf("id", "start_time", "end_time", "duration",
                        "avg_posture", "avg_focus", "alert_count",
                        "posture_alert_count", "focus_alert_count")
                )
                session?.let {
                    cursor.addRow(arrayOf(
                        it.id, it.startTime, it.endTime, it.duration,
                        it.avgPostureScore, it.avgFocusScore, it.alertCount,
                        it.postureAlertCount, it.focusAlertCount
                    ))
                }
                cursor.setNotificationUri(ctx.contentResolver, uri)
                cursor
            }
            CODE_SNAPSHOTS -> {
                val sessionId = ContentUris.parseId(uri)
                val snapshots = runBlocking { db.studySnapshotDao().getBySessionId(sessionId) }
                val cursor = MatrixCursor(
                    arrayOf("id", "session_id", "timestamp", "posture_score", "focus_score")
                )
                for (snap in snapshots) {
                    cursor.addRow(arrayOf(
                        snap.id, snap.sessionId, snap.timestamp,
                        snap.postureScore, snap.focusScore
                    ))
                }
                cursor.setNotificationUri(ctx.contentResolver, uri)
                cursor
            }
            else -> null
        }
    }

    override fun getType(uri: Uri): String? = when (uriMatcher.match(uri)) {
        CODE_SESSIONS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.sessions"
        CODE_SESSION_BY_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.sessions"
        CODE_SNAPSHOTS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.snapshots"
        else -> null
    }

    // 只读 ContentProvider，不支持写入操作
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
}
