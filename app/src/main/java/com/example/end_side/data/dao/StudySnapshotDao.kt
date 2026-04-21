package com.example.end_side.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.end_side.data.entity.StudySnapshot

@Dao
interface StudySnapshotDao {

    @Insert
    suspend fun insert(snapshot: StudySnapshot)

    @Insert
    suspend fun insertAll(snapshots: List<StudySnapshot>)

    @Query("SELECT * FROM study_snapshots WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySessionId(sessionId: Long): List<StudySnapshot>

    @Query("DELETE FROM study_snapshots WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)
}
