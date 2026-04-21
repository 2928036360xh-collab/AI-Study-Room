package com.example.end_side.ui.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.end_side.R
import com.example.end_side.ui.settings.HelpActivity
import com.example.end_side.ui.settings.PostureGuideActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.appcompat.app.AppCompatDelegate

class SettingsFragment : Fragment() {

    companion object {
        private const val PREFS_NAME = "ai_study_settings"
        private const val KEY_ALERT_SOUND = "alert_sound_enabled"
        private const val KEY_SENSITIVITY = "detection_sensitivity"
        private const val KEY_DARK_MODE = "dark_mode_enabled"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 坐姿指导视频 (VideoView 页面)
        view.findViewById<View>(R.id.card_posture_guide).setOnClickListener {
            startActivity(Intent(requireContext(), PostureGuideActivity::class.java))
        }

        // 使用帮助 (WebView 页面)
        view.findViewById<View>(R.id.card_help).setOnClickListener {
            startActivity(Intent(requireContext(), HelpActivity::class.java))
        }

        // 提醒音效开关 (SharedPreferences)
        val switchAlertSound = view.findViewById<SwitchMaterial>(R.id.switch_alert_sound)
        switchAlertSound.isChecked = prefs.getBoolean(KEY_ALERT_SOUND, true)
        switchAlertSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_ALERT_SOUND, isChecked).apply()
        }

        // 检测灵敏度 (SharedPreferences)
        val seekbar = view.findViewById<SeekBar>(R.id.seekbar_sensitivity)
        val tvSensitivity = view.findViewById<TextView>(R.id.tv_sensitivity_value)
        val sensitivityLabels = arrayOf("低", "中", "高")

        seekbar.progress = prefs.getInt(KEY_SENSITIVITY, 1)
        tvSensitivity.text = sensitivityLabels[seekbar.progress]

        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                tvSensitivity.text = sensitivityLabels[progress]
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                prefs.edit().putInt(KEY_SENSITIVITY, seekBar.progress).apply()
            }
        })

        // 深色模式切换 (AppCompatDelegate)
        val switchDarkMode = view.findViewById<SwitchMaterial>(R.id.switch_dark_mode)
        switchDarkMode.isChecked = prefs.getBoolean(KEY_DARK_MODE, true)
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }
}
