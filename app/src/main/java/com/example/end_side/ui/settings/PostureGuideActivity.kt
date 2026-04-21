package com.example.end_side.ui.settings

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.example.end_side.R

/**
 * 坐姿指导视频页面
 * 使用 VideoView 播放指导视频，通过 setVideoPath() 设置路径 -> start()
 * 视频资源放置在 res/raw/ 目录
 */
class PostureGuideActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_posture_guide)

        findViewById<ImageView>(R.id.btn_guide_back).setOnClickListener { finish() }

        videoView = findViewById(R.id.video_posture)

        // 设置 MediaController 提供播放控制
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        // 尝试加载本地 raw 资源视频，若不存在则使用示范网络视频
        try {
            val rawId = resources.getIdentifier("posture_guide", "raw", packageName)
            if (rawId != 0) {
                videoView.setVideoURI(Uri.parse("android.resource://$packageName/$rawId"))
            } else {
                // 本地无视频资源时，显示提示文字
                findViewById<android.widget.TextView>(R.id.tv_video_hint).text =
                    "暂无视频资源\n请将坐姿指导视频放入 res/raw/posture_guide.mp4"
                return
            }
            videoView.setOnPreparedListener { it.isLooping = true }
            videoView.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        if (videoView.isPlaying) {
            videoView.pause()
        }
    }

    override fun onDestroy() {
        videoView.stopPlayback()
        super.onDestroy()
    }
}
