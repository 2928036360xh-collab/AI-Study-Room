package com.example.end_side.ui.settings

import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.example.end_side.R

/**
 * 使用帮助页面
 * 使用 WebView 展示本地 HTML 帮助文档
 */
class HelpActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        val progressBar = findViewById<ProgressBar>(R.id.progress_help)

        findViewById<ImageView>(R.id.btn_help_back).setOnClickListener { finish() }

        webView = findViewById(R.id.webview_help)

        // WebView 安全配置：禁止 JS 文件访问，限制内容加载
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.domStorageEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // 拦截外部链接，确保只在 WebView 内部打开
        webView.webViewClient = WebViewClient()

        // 加载进度
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                } else {
                    progressBar.visibility = View.GONE
                }
            }
        }

        // 加载本地帮助页面（assets/help.html）
        webView.loadUrl("file:///android_asset/help.html")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
