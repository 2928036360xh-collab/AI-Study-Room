package com.example.end_side.ui.study

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.end_side.R
import com.example.end_side.data.AppDatabase
import com.example.end_side.ui.widget.ChartView
import com.example.end_side.ui.widget.ScoreGaugeView
import com.example.end_side.util.TimeUtils
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 学习报告页面
 * 展示本次学习的统计数据和趋势图
 */
class StudyReportActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SESSION_ID = "extra_session_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_study_report)

        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1)
        if (sessionId == -1L) {
            finish()
            return
        }

        loadReportData(sessionId)

        findViewById<MaterialButton>(R.id.btn_report_done).setOnClickListener {
            finish()
        }
    }

    private fun loadReportData(sessionId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(this@StudyReportActivity)
                val session = db.studySessionDao().getById(sessionId) ?: return@launch
                val snapshots = db.studySnapshotDao().getBySessionId(sessionId)

                withContext(Dispatchers.Main) {
                    // 学习时长
                    findViewById<TextView>(R.id.tv_report_duration).text =
                        TimeUtils.formatDurationChinese(session.duration)

                    // 评分仪表盘
                    val gaugePosture = findViewById<ScoreGaugeView>(R.id.gauge_report_posture)
                    val gaugeFocus = findViewById<ScoreGaugeView>(R.id.gauge_report_focus)
                    gaugePosture.setScore(session.avgPostureScore)
                    gaugePosture.setLabel("坐姿")
                    gaugeFocus.setScore(session.avgFocusScore)
                    gaugeFocus.setLabel("专注力")

                    // 提醒次数
                    findViewById<TextView>(R.id.tv_report_alerts).text =
                        "${session.alertCount} 次"

                    // 趋势图
                    if (snapshots.isNotEmpty()) {
                        val postureData = snapshots.map { it.postureScore.toFloat() }
                        val focusData = snapshots.map { it.focusScore.toFloat() }

                        findViewById<ChartView>(R.id.chart_posture).setData(
                            postureData, "坐姿趋势", Color.parseColor("#4CAF50")
                        )
                        findViewById<ChartView>(R.id.chart_focus).setData(
                            focusData, "专注力趋势", Color.parseColor("#2196F3")
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
