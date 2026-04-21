package com.example.end_side.ui.history

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.end_side.R
import com.example.end_side.data.AppDatabase
import com.example.end_side.data.entity.StudySession
import com.example.end_side.ui.widget.ChartView
import com.example.end_side.ui.widget.ScoreGaugeView
import com.example.end_side.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 学习会话详情页
 * 展示单次学习的详细数据与趋势图
 */
class SessionDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SESSION_ID = "extra_session_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_detail)

        findViewById<ImageView>(R.id.btn_detail_back).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btn_detail_delete).setOnClickListener { confirmDelete() }

        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1)
        if (sessionId == -1L) { finish(); return }

        loadData(sessionId)
    }

    private fun loadData(sessionId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(this@SessionDetailActivity)
                val session = db.studySessionDao().getById(sessionId) ?: run {
                    withContext(Dispatchers.Main) { finish() }
                    return@launch
                }
                val snapshots = db.studySnapshotDao().getBySessionId(sessionId)

                withContext(Dispatchers.Main) {
                    bindSession(session)

                    // 趋势图数据
                    val postureData = snapshots.map { it.postureScore.toFloat() }
                    val focusData = snapshots.map { it.focusScore.toFloat() }

                    if (postureData.isNotEmpty()) {
                        findViewById<ChartView>(R.id.chart_detail_posture).setData(postureData)
                        findViewById<ChartView>(R.id.chart_detail_focus).setData(focusData)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun bindSession(session: StudySession) {
        findViewById<TextView>(R.id.tv_detail_date).text =
            TimeUtils.formatTimestamp(session.startTime)
        findViewById<TextView>(R.id.tv_detail_duration).text =
            TimeUtils.formatDurationChinese(session.duration)

        findViewById<ScoreGaugeView>(R.id.gauge_detail_posture).setScore(session.avgPostureScore)
        findViewById<ScoreGaugeView>(R.id.gauge_detail_focus).setScore(session.avgFocusScore)

        findViewById<TextView>(R.id.tv_detail_total_alerts).text =
            session.alertCount.toString()
        findViewById<TextView>(R.id.tv_detail_posture_alerts).text =
            session.postureAlertCount.toString()
        findViewById<TextView>(R.id.tv_detail_focus_alerts).text =
            session.focusAlertCount.toString()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("删除确认")
            .setMessage("确定删除此学习记录吗？")
            .setPositiveButton(R.string.delete) { _, _ -> deleteSession() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteSession() {
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1)
        if (sessionId == -1L) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(this@SessionDetailActivity)
                db.studySnapshotDao().deleteBySessionId(sessionId)
                db.studySessionDao().deleteById(sessionId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SessionDetailActivity, "已删除", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
