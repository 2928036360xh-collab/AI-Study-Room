package com.example.end_side.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.end_side.data.entity.StudySession

@Dao
interface StudySessionDao {

    @Insert
    suspend fun insert(session: StudySession): Long

    @Query("UPDATE study_sessions SET endTime = :endTime, duration = :duration, avgPostureScore = :avgPosture, avgFocusScore = :avgFocus, alertCount = :alerts, postureAlertCount = :postureAlerts, focusAlertCount = :focusAlerts WHERE id = :id")
    suspend fun updateSessionEnd(
        id: Long, endTime: Long, duration: Long,
        avgPosture: Int, avgFocus: Int,
        alerts: Int, postureAlerts: Int, focusAlerts: Int
    )

    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    suspend fun getAll(): List<StudySession>

    @Query("SELECT * FROM study_sessions WHERE id = :id")
    suspend fun getById(id: Long): StudySession?

    @Query("SELECT * FROM study_sessions WHERE startTime >= :startTime ORDER BY startTime DESC")
    suspend fun getSessionsSince(startTime: Long): List<StudySession>

    @Query("SELECT SUM(duration) FROM study_sessions WHERE startTime >= :startTime")
    suspend fun getTotalDurationSince(startTime: Long): Long?

    @Query("SELECT COUNT(*) FROM study_sessions")
    suspend fun getCount(): Int

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
