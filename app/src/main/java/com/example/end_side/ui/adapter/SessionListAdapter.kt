package com.example.end_side.ui.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.end_side.R
import com.example.end_side.data.entity.StudySession
import com.example.end_side.util.TimeUtils

/**
 * 学习记录列表适配器
 * 使用标准 RecyclerView.Adapter + ViewHolder 模式
 */
class SessionListAdapter(
    private val onItemClick: (StudySession) -> Unit
) : ListAdapter<StudySession, SessionListAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_study_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvScore: TextView = itemView.findViewById(R.id.tv_score)
        private val viewScoreCircle: View = itemView.findViewById(R.id.view_score_circle)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_session_date)
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_session_duration)
        private val tvAlerts: TextView = itemView.findViewById(R.id.tv_session_alerts)

        fun bind(session: StudySession) {
            val avgScore = (session.avgPostureScore + session.avgFocusScore) / 2
            tvScore.text = avgScore.toString()
            tvDate.text = TimeUtils.formatTimestamp(session.startTime)
            tvDuration.text = TimeUtils.formatDurationChinese(session.duration)
            tvAlerts.text = "${session.alertCount}次"

            // 圆形背景颜色
            val color = when {
                avgScore >= 80 -> Color.parseColor("#4CAF50")
                avgScore >= 60 -> Color.parseColor("#FF9800")
                else -> Color.parseColor("#F44336")
            }
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            viewScoreCircle.background = drawable

            itemView.setOnClickListener { onItemClick(session) }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<StudySession>() {
        override fun areItemsTheSame(oldItem: StudySession, newItem: StudySession) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: StudySession, newItem: StudySession) =
            oldItem == newItem
    }
}
