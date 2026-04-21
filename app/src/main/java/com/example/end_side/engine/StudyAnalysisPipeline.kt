package com.example.end_side.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import com.example.end_side.engine.analyzer.FocusAnalyzer
import com.example.end_side.engine.analyzer.PostureAnalyzer
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

/**
 * 学习分析流水线 —— 基于 ML Kit 的统一调度器
 *
 * 处理流程:
 * 1. ML Kit Pose Detection → 检测人体存在 + 提取 17 个 COCO 关键点
 * 2. ML Kit Face Detection → 检测人脸边界框
 * 3. PostureAnalyzer → 坐姿评分
 * 4. FocusAnalyzer → 专注力评分
 */
class StudyAnalysisPipeline {

    companion object {
        private const val TAG = "StudyPipeline"

        // ML Kit PoseLandmark type → COCO 17 关键点索引映射
        private val ML_KIT_TO_COCO = intArrayOf(
            PoseLandmark.NOSE,           // 0: 鼻子
            PoseLandmark.LEFT_EYE,       // 1: 左眼
            PoseLandmark.RIGHT_EYE,      // 2: 右眼
            PoseLandmark.LEFT_EAR,       // 3: 左耳
            PoseLandmark.RIGHT_EAR,      // 4: 右耳
            PoseLandmark.LEFT_SHOULDER,  // 5: 左肩
            PoseLandmark.RIGHT_SHOULDER, // 6: 右肩
            PoseLandmark.LEFT_ELBOW,     // 7: 左肘
            PoseLandmark.RIGHT_ELBOW,    // 8: 右肘
            PoseLandmark.LEFT_WRIST,     // 9: 左腕
            PoseLandmark.RIGHT_WRIST,    // 10: 右腕
            PoseLandmark.LEFT_HIP,       // 11: 左髋
            PoseLandmark.RIGHT_HIP,      // 12: 右髋
            PoseLandmark.LEFT_KNEE,      // 13: 左膝
            PoseLandmark.RIGHT_KNEE,     // 14: 右膝
            PoseLandmark.LEFT_ANKLE,     // 15: 左踝
            PoseLandmark.RIGHT_ANKLE     // 16: 右踝
        )
    }

    private var mlPoseDetector: com.google.mlkit.vision.pose.PoseDetector? = null
    private var mlFaceDetector: com.google.mlkit.vision.face.FaceDetector? = null
    private val postureAnalyzer = PostureAnalyzer()
    private val focusAnalyzer = FocusAnalyzer()
    private var isInitialized = false

    /**
     * 初始化 ML Kit 检测器
     * 可在任意线程调用，ML Kit 自身线程安全
     */
    fun init(context: Context) {
        try {
            val poseOptions = PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()
            mlPoseDetector = PoseDetection.getClient(poseOptions)

            val faceOptions = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
            mlFaceDetector = FaceDetection.getClient(faceOptions)

            isInitialized = true
            android.util.Log.i(TAG, "ML Kit 分析流水线初始化完成")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "分析流水线初始化失败", e)
            isInitialized = false
        }
    }

    /**
     * 执行完整分析流水线
     * 必须在后台线程调用（内部使用 Tasks.await 同步阻塞）
     */
    fun analyze(bitmap: Bitmap): AnalysisResult {
        if (!isInitialized) return AnalysisResult.EMPTY

        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val imgW = bitmap.width.toFloat()
            val imgH = bitmap.height.toFloat()

            // ---- Step 1: ML Kit 姿态检测 ----
            val pose = try {
                Tasks.await(mlPoseDetector!!.process(inputImage))
            } catch (e: Exception) {
                android.util.Log.w(TAG, "姿态检测失败", e)
                null
            }

            val keypoints = mutableListOf<PointF>()
            val confidences = mutableListOf<Float>()
            val personBoxes = mutableListOf<RectF>()

            if (pose != null && pose.allPoseLandmarks.isNotEmpty()) {
                for (mlType in ML_KIT_TO_COCO) {
                    val lm = pose.getPoseLandmark(mlType)
                    if (lm != null) {
                        keypoints.add(PointF(lm.position.x, lm.position.y))
                        confidences.add(lm.inFrameLikelihood)
                    } else {
                        keypoints.add(PointF(0f, 0f))
                        confidences.add(0f)
                    }
                }

                // 从有效关键点推算人体边界框
                val valid = keypoints.zip(confidences).filter { it.second > 0.3f }.map { it.first }
                if (valid.isNotEmpty()) {
                    val pad = 20f
                    personBoxes.add(
                        RectF(
                            (valid.minOf { it.x } - pad).coerceAtLeast(0f),
                            (valid.minOf { it.y } - pad).coerceAtLeast(0f),
                            (valid.maxOf { it.x } + pad).coerceAtMost(imgW),
                            (valid.maxOf { it.y } + pad).coerceAtMost(imgH)
                        )
                    )
                }
            }

            // ---- Step 2: ML Kit 人脸检测 ----
            val faces = try {
                Tasks.await(mlFaceDetector!!.process(inputImage))
            } catch (e: Exception) {
                android.util.Log.w(TAG, "人脸检测失败", e)
                emptyList()
            }
            val faceBoxes = faces.map { RectF(it.boundingBox) }

            // ---- Step 3: 坐姿分析 ----
            val postureResult = postureAnalyzer.analyze(keypoints, confidences)

            // ---- Step 4: 专注力分析 ----
            val focusResult = focusAnalyzer.analyze(personBoxes, faceBoxes)

            return AnalysisResult(
                postureScore = postureResult.score,
                focusScore = focusResult.score,
                postureIssues = postureResult.issues,
                focusIssues = focusResult.issues,
                focusStatus = focusResult.status,
                personDetections = personBoxes,
                faceBoxes = faceBoxes,
                keypoints = keypoints,
                keypointConfidences = confidences,
                hasPerson = focusResult.hasPerson,
                hasFace = focusResult.hasFace
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "分析异常", e)
            return AnalysisResult.EMPTY
        }
    }

    fun release() {
        mlPoseDetector?.close()
        mlFaceDetector?.close()
        focusAnalyzer.reset()
        isInitialized = false
    }

    data class AnalysisResult(
        val postureScore: Int = 0,
        val focusScore: Int = 0,
        val postureIssues: List<String> = emptyList(),
        val focusIssues: List<String> = emptyList(),
        val focusStatus: FocusAnalyzer.FocusStatus = FocusAnalyzer.FocusStatus.UNKNOWN,
        val personDetections: List<RectF> = emptyList(),
        val faceBoxes: List<RectF> = emptyList(),
        val keypoints: List<PointF> = emptyList(),
        val keypointConfidences: List<Float> = emptyList(),
        val hasPerson: Boolean = false,
        val hasFace: Boolean = false
    ) {
        companion object {
            val EMPTY = AnalysisResult()
        }
    }
}
