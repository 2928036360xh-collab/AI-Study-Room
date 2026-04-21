package com.example.end_side

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.end_side.ui.fragment.HistoryFragment
import com.example.end_side.ui.fragment.HomeFragment
import com.example.end_side.ui.fragment.OcrFragment
import com.example.end_side.ui.fragment.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    // 缓存 Fragment 实例，避免重复创建
    private val homeFragment by lazy { HomeFragment() }
    private val ocrFragment by lazy { OcrFragment() }
    private val historyFragment by lazy { HistoryFragment() }
    private val settingsFragment by lazy { SettingsFragment() }
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)
        setupBottomNavigation()

        // 首次加载首页
        if (savedInstanceState == null) {
            switchFragment(homeFragment)
        }
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(homeFragment)
                    true
                }
                R.id.nav_ocr -> {
                    switchFragment(ocrFragment)
                    true
                }
                R.id.nav_history -> {
                    switchFragment(historyFragment)
                    true
                }
                R.id.nav_settings -> {
                    switchFragment(settingsFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(target: Fragment) {
        if (activeFragment === target) return
        val transaction = supportFragmentManager.beginTransaction()

        // 切换时使用淡入淡出动画
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)

        // 隐藏当前 Fragment
        activeFragment?.let { transaction.hide(it) }

        // 显示或添加目标 Fragment
        if (target.isAdded) {
            transaction.show(target)
        } else {
            transaction.add(R.id.fragment_container, target)
        }

        transaction.commit()
        activeFragment = target
    }
}