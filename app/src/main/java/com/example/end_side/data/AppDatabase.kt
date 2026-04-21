package com.example.end_side.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.end_side.data.dao.StudySessionDao
import com.example.end_side.data.dao.StudySnapshotDao
import com.example.end_side.data.dao.WordItemDao
import com.example.end_side.data.entity.StudySession
import com.example.end_side.data.entity.StudySnapshot
import com.example.end_side.data.entity.WordItem

@Database(
    entities = [StudySession::class, StudySnapshot::class, WordItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studySessionDao(): StudySessionDao
    abstract fun studySnapshotDao(): StudySnapshotDao
    abstract fun wordItemDao(): WordItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_study_room.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
