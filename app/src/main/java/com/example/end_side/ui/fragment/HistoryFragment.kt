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
import com.example.end_side.viewmodel.HistoryViewModel

class HistoryFragment : Fragment() {

    private val viewModel: HistoryViewModel by viewModels()

    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmpty: TextView

    private val adapter = SessionListAdapter { session ->
        startActivity(Intent(requireContext(), SessionDetailActivity::class.java).apply {
            putExtra(SessionDetailActivity.EXTRA_SESSION_ID, session.id)
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvHistory = view.findViewById(R.id.rv_history)
        tvEmpty = view.findViewById(R.id.tv_history_empty)

        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = adapter

        observeData()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSessions(requireContext())
    }

    private fun observeData() {
        viewModel.sessions.observe(viewLifecycleOwner) { sessions ->
            adapter.submitList(sessions)
            tvEmpty.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
            rvHistory.visibility = if (sessions.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}
