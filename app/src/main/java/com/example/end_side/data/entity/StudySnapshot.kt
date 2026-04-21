package com.example.end_side.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 学习时间线快照 - 记录学习过程中的实时评分数据
 * 每 30 秒采集一次
 */
@Entity(tableName = "study_snapshots")
data class StudySnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,          // 关联的学习会话 ID
    val timestamp: Long,          // 采集时间戳
    val postureScore: Int,        // 当前坐姿评分
    val focusScore: Int,          // 当前专注力评分
    val isPersonDetected: Boolean, // 是否检测到人
    val isFaceDetected: Boolean    // 是否检测到人脸
)
