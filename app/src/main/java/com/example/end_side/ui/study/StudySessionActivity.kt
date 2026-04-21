package com.example.end_side.ui.study

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.end_side.R
import com.example.end_side.data.AppDatabase
import com.example.end_side.data.entity.StudySession
import com.example.end_side.data.entity.StudySnapshot
import com.example.end_side.data.entity.WordItem
import com.example.end_side.data.network.RetrofitClient
import com.example.end_side.engine.StudyAnalysisPipeline
import com.example.end_side.service.StudyTimerService
import com.example.end_side.ui.widget.OverlayView
import com.example.end_side.util.BitmapUtils
import com.example.end_side.util.NotificationHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 学习会话页面
 * 功能：CameraX 实时预览 + AI 坐姿/专注力分析 + 前台服务计时
 */
class StudySessionActivity : AppCompatActivity() {

    companion object {
        private const val SNAPSHOT_INTERVAL_MS = 30_000L  // 每 30 秒采集一次快照
        const val EXTRA_SESSION_ID = "extra_session_id"
    }

    // UI 控件
    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var tvElapsedTime: TextView
    private lateinit var tvPostureScore: TextView
    private lateinit var tvFocusScore: TextView
    private lateinit var btnStop: MaterialButton
    private lateinit var btnOcr: MaterialButton

    // AI 分析流水线
    private val pipeline = StudyAnalysisPipeline()
    private lateinit var analysisExecutor: ExecutorService

    // ML Kit OCR（学习中使用）
    private val textRecognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )
    private var ocrPhotoUri: Uri? = null

    // 前台服务
    private var timerService: StudyTimerService? = null
    private var isServiceBound = false

    // 数据库
    private var sessionId: Long = 0L
    private val startTime = System.currentTimeMillis()
    private val snapshots = mutableListOf<StudySnapshot>()
    private var postureScoreSum = 0
    private var focusScoreSum = 0
    private var analyzeCount = 0
    private var alertCount = 0
    private var postureAlertCount = 0
    private var focusAlertCount = 0
    private var lastSnapshotTime = 0L
    private var lastPostureAlertTime = 0L
    private var lastFocusAlertTime = 0L

    // 服务连接
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as StudyTimerService.TimerBinder
            timerService = binder.getService()
            isServiceBound = true
            timerService?.onTickListener = { _, formatted ->
                runOnUiThread { tvElapsedTime.text = formatted }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
        }
    }

    // 权限请求
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // OCR 拍照回调（使用后置摄像头拍教材）
    private val ocrTakePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && ocrPhotoUri != null) {
            processOcrImage(ocrPhotoUri!!)
        }
    }

    // OCR 选图回调
    private val ocrPickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> processOcrImage(uri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_study_session)

        initViews()
        initPipeline()
        createSessionRecord()
        startTimerService()
        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    private fun initViews() {
        previewView = findViewById(R.id.preview_view)
        overlayView = findViewById(R.id.overlay_view)
        tvElapsedTime = findViewById(R.id.tv_elapsed_time)
        tvPostureScore = findViewById(R.id.tv_posture_score)
        tvFocusScore = findViewById(R.id.tv_focus_score)
        btnStop = findViewById(R.id.btn_stop_study)
        btnOcr = findViewById(R.id.btn_ocr_in_study)

        btnStop.setOnClickListener { stopStudySession() }
        btnOcr.setOnClickListener { showOcrSourceDialog() }
    }

    private fun initPipeline() {
        analysisExecutor = Executors.newSingleThreadExecutor()
        // 在后台线程初始化模型
        lifecycleScope.launch(Dispatchers.IO) {
            pipeline.init(this@StudySessionActivity)
        }
    }

    private fun createSessionRecord() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(this@StudySessionActivity).studySessionDao()
            val session = StudySession(startTime = startTime)
            sessionId = dao.insert(session)
        }
    }

    private fun startTimerService() {
        val intent = Intent(this, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // 预览
                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }

                // 图像分析 — 用于实时 AI 推理
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    try {
                        val bitmap = BitmapUtils.imageToBitmap(
                            imageProxy.image ?: return@setAnalyzer,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        val result = pipeline.analyze(bitmap)
                        bitmap.recycle()

                        runOnUiThread { updateUI(result) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        imageProxy.close()
                    }
                }

                // 使用前置摄像头
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 在主线程更新 UI（确保线程安全）
     */
    private fun updateUI(result: StudyAnalysisPipeline.AnalysisResult) {
        // 更新评分文字
        tvPostureScore.text = result.postureScore.toString()
        tvFocusScore.text = result.focusScore.toString()

        // 评分颜色
        tvPostureScore.setTextColor(getScoreColor(result.postureScore))
        tvFocusScore.setTextColor(getScoreColor(result.focusScore))

        // 更新叠加层
        overlayView.updateDetectionBoxes(
            result.personDetections.map { rect ->
                OverlayView.DetectionBox(rect, "person", 1.0f)
            }
        )
        overlayView.updatePoseKeypoints(result.keypoints, result.keypointConfidences)
        overlayView.updateFaceBoxes(result.faceBoxes)

        // 提醒逻辑
        val now = System.currentTimeMillis()
        val alertCooldown = 15_000L // 同类提醒冷却 15 秒

        if (result.postureScore < 50 && now - lastPostureAlertTime > alertCooldown) {
            overlayView.updateAlert("请注意坐姿！", OverlayView.AlertLevel.WARNING)
            postureAlertCount++
            alertCount++
            lastPostureAlertTime = now
        } else if (result.focusScore < 40 && now - lastFocusAlertTime > alertCooldown) {
            overlayView.updateAlert("请保持专注！", OverlayView.AlertLevel.DANGER)
            focusAlertCount++
            alertCount++
            lastFocusAlertTime = now
        } else if (result.postureScore >= 50 && result.focusScore >= 40) {
            overlayView.updateAlert(null, OverlayView.AlertLevel.NONE)
        }

        // 累计统计
        postureScoreSum += result.postureScore
        focusScoreSum += result.focusScore
        analyzeCount++

        // 定时快照
        if (now - lastSnapshotTime > SNAPSHOT_INTERVAL_MS) {
            lastSnapshotTime = now
            snapshots.add(
                StudySnapshot(
                    sessionId = sessionId,
                    timestamp = now,
                    postureScore = result.postureScore,
                    focusScore = result.focusScore,
                    isPersonDetected = result.hasPerson,
                    isFaceDetected = result.hasFace
                )
            )
        }
    }

    private fun getScoreColor(score: Int): Int {
        return when {
            score >= 80 -> Color.parseColor("#10B981")
            score >= 60 -> Color.parseColor("#F59E0B")
            else -> Color.parseColor("#EF4444")
        }
    }

    private fun stopStudySession() {
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        val avgPosture = if (analyzeCount > 0) postureScoreSum / analyzeCount else 0
        val avgFocus = if (analyzeCount > 0) focusScoreSum / analyzeCount else 0

        // 保存数据（Room 操作必须在 IO 线程）
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@StudySessionActivity)
            db.studySessionDao().updateSessionEnd(
                id = sessionId,
                endTime = endTime,
                duration = duration,
                avgPosture = avgPosture,
                avgFocus = avgFocus,
                alerts = alertCount,
                postureAlerts = postureAlertCount,
                focusAlerts = focusAlertCount
            )
            db.studySnapshotDao().insertAll(snapshots)

            withContext(Dispatchers.Main) {
                // 跳转到学习报告
                val intent = Intent(this@StudySessionActivity, StudyReportActivity::class.java).apply {
                    putExtra(EXTRA_SESSION_ID, sessionId)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onDestroy() {
        pipeline.release()
        analysisExecutor.shutdown()
        textRecognizer.close()
        if (isServiceBound) {
            unbindService(serviceConnection)
        }
        super.onDestroy()
    }

    // ====== 学习中 OCR 功能 ======

    /**
     * 选择 OCR 图片来源：拍照或相册
     */
    private fun showOcrSourceDialog() {
        val items = arrayOf("📷  拍照识别", "🖼️  相册选择")
        AlertDialog.Builder(this)
            .setTitle("文字识别")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> launchOcrCamera()
                    1 -> {
                        val intent = Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                        ocrPickImageLauncher.launch(intent)
                    }
                }
            }
            .show()
    }

    private fun launchOcrCamera() {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "STUDY_OCR_${timeStamp}.jpg"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val photoFile = File(storageDir, imageFileName)

            val uri = FileProvider.getUriForFile(
                this,
                "com.example.end_side.fileprovider",
                photoFile
            )
            ocrPhotoUri = uri
            ocrTakePictureLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "无法启动相机", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理选中的图片，使用 ML Kit 进行 OCR
     */
    private fun processOcrImage(imageUri: Uri) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_study_ocr, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_ocr_title)
        val tvOcrResult = dialogView.findViewById<TextView>(R.id.tv_dialog_ocr_result)
        val progressOcr = dialogView.findViewById<ProgressBar>(R.id.progress_dialog_ocr)
        val layoutActions = dialogView.findViewById<View>(R.id.layout_word_actions)
        val etWord = dialogView.findViewById<TextInputEditText>(R.id.et_dialog_word)
        val tvTranslation = dialogView.findViewById<TextView>(R.id.tv_dialog_translation)
        val btnLookup = dialogView.findViewById<MaterialButton>(R.id.btn_dialog_lookup)
        val btnAdd = dialogView.findViewById<MaterialButton>(R.id.btn_dialog_add_word)

        progressOcr.visibility = View.VISIBLE
        tvOcrResult.text = ""
        layoutActions.visibility = View.GONE

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.show()

        // 后台线程解码图片 + ML Kit OCR
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    withContext(Dispatchers.Main) {
                        textRecognizer.process(inputImage)
                            .addOnSuccessListener { visionText ->
                                progressOcr.visibility = View.GONE
                                val recognizedText = visionText.text
                                tvOcrResult.text = if (recognizedText.isNotBlank()) {
                                    layoutActions.visibility = View.VISIBLE
                                    recognizedText
                                } else {
                                    "未识别到文字"
                                }

                                // 查词功能
                                setupOcrDialogActions(
                                    btnLookup, btnAdd, etWord, tvTranslation,
                                    recognizedText, dialog
                                )
                            }
                            .addOnFailureListener { e ->
                                progressOcr.visibility = View.GONE
                                tvOcrResult.text = "识别失败：${e.message}"
                            }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressOcr.visibility = View.GONE
                    tvOcrResult.text = "处理失败：${e.message}"
                }
            }
        }
    }

    private fun setupOcrDialogActions(
        btnLookup: MaterialButton,
        btnAdd: MaterialButton,
        etWord: TextInputEditText,
        tvTranslation: TextView,
        sourceText: String,
        dialog: AlertDialog
    ) {
        btnLookup.setOnClickListener {
            val word = etWord.text?.toString()?.trim() ?: ""
            if (word.isBlank()) {
                Toast.makeText(this, "请输入要查询的单词", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btnLookup.isEnabled = false
            btnLookup.text = "查询中…"
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.dictionaryApi.lookupWord(word)
                    val translation = response.firstOrNull()?.meanings
                        ?.flatMap { it.definitions ?: emptyList() }
                        ?.take(3)
                        ?.mapIndexed { idx, def -> "${idx + 1}. ${def.definition}" }
                        ?.joinToString("\n")
                        ?: "未找到释义"
                    withContext(Dispatchers.Main) {
                        tvTranslation.text = translation
                        tvTranslation.visibility = View.VISIBLE
                        btnLookup.isEnabled = true
                        btnLookup.text = "查词"
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        tvTranslation.text = "查询失败：${e.message}"
                        tvTranslation.visibility = View.VISIBLE
                        btnLookup.isEnabled = true
                        btnLookup.text = "查词"
                    }
                }
            }
        }

        btnAdd.setOnClickListener {
            val word = etWord.text?.toString()?.trim() ?: ""
            if (word.isBlank()) {
                Toast.makeText(this, "请输入要添加的生词", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val translation = if (tvTranslation.visibility == View.VISIBLE)
                tvTranslation.text.toString() else ""
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val dao = AppDatabase.getInstance(this@StudySessionActivity).wordItemDao()
                    dao.insert(WordItem(word = word, translation = translation, sourceText = sourceText.take(100)))
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@StudySessionActivity, "已添加到生词本", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
