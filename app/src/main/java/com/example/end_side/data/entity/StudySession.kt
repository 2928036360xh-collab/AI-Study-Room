package com.example.end_side.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 学习会话实体 - 记录每次学习的完整数据
 */
@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,          // 开始时间戳
    val endTime: Long = 0,        // 结束时间戳
    val duration: Long = 0,       // 学习时长(毫秒)
    val avgPostureScore: Int = 0, // 平均坐姿评分
    val avgFocusScore: Int = 0,   // 平均专注力评分
    val alertCount: Int = 0,      // 提醒次数
    val postureAlertCount: Int = 0,  // 坐姿异常提醒次数
    val focusAlertCount: Int = 0     // 专注力异常提醒次数
)
