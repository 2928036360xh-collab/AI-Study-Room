package com.example.end_side.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.end_side.R
import com.example.end_side.ui.adapter.SessionListAdapter
import com.example.end_side.ui.history.SessionDetailActivity
import com.example.end_side.ui.study.StudySessionActivity
import com.example.end_side.ui.widget.ScoreGaugeView
import com.example.end_side.viewmodel.HomeViewModel
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var gaugePosture: ScoreGaugeView
    private lateinit var gaugeFocus: ScoreGaugeView
    private lateinit var tvTodayDuration: TextView
    private lateinit var tvTotalSessions: TextView
    private lateinit var btnStartStudy: MaterialButton
    private lateinit var rvRecentSessions: RecyclerView

    private val sessionAdapter = SessionListAdapter { session ->
        startActivity(Intent(requireContext(), SessionDetailActivity::class.java).apply {
            putExtra(SessionDetailActivity.EXTRA_SESSION_ID, session.id)
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupListeners()
        observeData()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadHomeData(requireContext())
    }

    private fun initViews(view: View) {
        gaugePosture = view.findViewById(R.id.gauge_posture)
        gaugeFocus = view.findViewById(R.id.gauge_focus)
        tvTodayDuration = view.findViewById(R.id.tv_today_duration)
        tvTotalSessions = view.findViewById(R.id.tv_total_sessions)
        btnStartStudy = view.findViewById(R.id.btn_start_study)
        rvRecentSessions = view.findViewById(R.id.rv_recent_sessions)

        gaugePosture.setLabel("坐姿")
        gaugeFocus.setLabel("专注力")

        rvRecentSessions.layoutManager = LinearLayoutManager(requireContext())
        rvRecentSessions.adapter = sessionAdapter
    }

    private fun setupListeners() {
        btnStartStudy.setOnClickListener {
            val intent = Intent(requireContext(), StudySessionActivity::class.java)
            startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
        }
    }

    private fun observeData() {
        viewModel.avgPosture.observe(viewLifecycleOwner) { score ->
            gaugePosture.setScore(score)
        }
        viewModel.avgFocus.observe(viewLifecycleOwner) { score ->
            gaugeFocus.setScore(score)
        }
        viewModel.todayDuration.observe(viewLifecycleOwner) { text ->
            tvTodayDuration.text = text
        }
        viewModel.totalSessions.observe(viewLifecycleOwner) { count ->
            tvTotalSessions.text = count.toString()
        }
        viewModel.recentSessions.observe(viewLifecycleOwner) { sessions ->
            sessionAdapter.submitList(sessions)
        }
    }
}
