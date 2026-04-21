package com.example.end_side.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.end_side.data.AppDatabase
import com.example.end_side.data.entity.StudySession
import com.example.end_side.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    val avgPosture = MutableLiveData(0)
    val avgFocus = MutableLiveData(0)
    val todayDuration = MutableLiveData("0分钟")
    val totalSessions = MutableLiveData(0)
    val recentSessions = MutableLiveData<List<StudySession>>(emptyList())

    fun loadHomeData(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)
                val dao = db.studySessionDao()

                // 所有会话
                val allSessions = dao.getAll()
                val count = allSessions.size

                // 今日学习时长
                val todayStart = TimeUtils.getTodayStartMillis()
                val todayTotal = dao.getTotalDurationSince(todayStart) ?: 0L

                // 平均分数（最近 7 次）
                val recent = allSessions.take(7)
                val postureAvg = if (recent.isNotEmpty()) recent.map { it.avgPostureScore }.average().toInt() else 0
                val focusAvg = if (recent.isNotEmpty()) recent.map { it.avgFocusScore }.average().toInt() else 0

                // 切回主线程更新 LiveData
                avgPosture.postValue(postureAvg)
                avgFocus.postValue(focusAvg)
                todayDuration.postValue(TimeUtils.formatDurationChinese(todayTotal))
                totalSessions.postValue(count)
                recentSessions.postValue(recent.take(3))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
