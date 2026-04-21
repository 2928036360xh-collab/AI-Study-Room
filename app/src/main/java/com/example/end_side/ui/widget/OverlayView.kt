package com.example.end_side.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * 通用叠加层 View：在摄像头预览上绘制检测框、骨架、人脸关键点等
 * 使用 Canvas 进行自定义绘制
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 检测框列表
    private var detectionBoxes: List<DetectionBox> = emptyList()

    // 骨架关键点
    private var poseKeypoints: List<PointF> = emptyList()
    private var poseConfidences: List<Float> = emptyList()

    // 人脸框
    private var faceBoxes: List<RectF> = emptyList()

    // 提示文字
    private var alertText: String? = null
    private var alertLevel: AlertLevel = AlertLevel.NONE

    // 实时评分
    private var postureScore: Int = -1
    private var focusScore: Int = -1

    // 画笔 — 赛博朋克/HUD 风格
    private val boxPaint = Paint().apply {
        color = Color.parseColor("#00C8FF")   // 电光青
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val labelPaint = Paint().apply {
        color = Color.parseColor("#00C8FF")
        textSize = 32f
        isAntiAlias = true
        isFakeBoldText = true
    }

    private val labelBgPaint = Paint().apply {
        color = Color.parseColor("#CC050D1A")  // 深黑半透明
        style = Paint.Style.FILL
    }

    private val keypointPaint = Paint().apply {
        color = Color.parseColor("#00FF9D")   // 霓虹绿
        strokeWidth = 12f
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val bonePaint = Paint().apply {
        color = Color.parseColor("#00C8FF")   // 电光青骨骼线
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val faceBoxPaint = Paint().apply {
        color = Color.parseColor("#E040FB")   // 霓虹紫
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val alertPaint = Paint().apply {
        textSize = 42f
        isAntiAlias = true
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val alertBgPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val scorePaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        isAntiAlias = true
    }

    // COCO 17 关键点骨架连接
    private val boneConnections = listOf(
        0 to 1, 0 to 2, 1 to 3, 2 to 4,       // 头部
        5 to 6,                                   // 肩膀
        5 to 7, 7 to 9, 6 to 8, 8 to 10,         // 手臂
        5 to 11, 6 to 12,                         // 躯干
        11 to 12,                                  // 髋部
        11 to 13, 13 to 15, 12 to 14, 14 to 16   // 腿部
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawDetectionBoxes(canvas)
        drawPoseSkeleton(canvas)
        drawFaceBoxes(canvas)
        drawAlert(canvas)
    }

    private fun drawDetectionBoxes(canvas: Canvas) {
        for (box in detectionBoxes) {
            // 绘制检测框
            canvas.drawRect(box.rect, boxPaint)

            // 绘制标签背景
            val label = "${box.label} ${(box.confidence * 100).toInt()}%"
            val textWidth = labelPaint.measureText(label)
            val textBgRect = RectF(
                box.rect.left, box.rect.top - 44f,
                box.rect.left + textWidth + 16f, box.rect.top
            )
            canvas.drawRect(textBgRect, labelBgPaint)
            canvas.drawText(label, box.rect.left + 8f, box.rect.top - 10f, labelPaint)
        }
    }

    private fun drawPoseSkeleton(canvas: Canvas) {
        if (poseKeypoints.isEmpty()) return
        val threshold = 0.3f

        // 绘制骨骼连线
        for ((startIdx, endIdx) in boneConnections) {
            if (startIdx >= poseKeypoints.size || endIdx >= poseKeypoints.size) continue
            val startConf = poseConfidences.getOrElse(startIdx) { 0f }
            val endConf = poseConfidences.getOrElse(endIdx) { 0f }
            if (startConf < threshold || endConf < threshold) continue

            val start = poseKeypoints[startIdx]
            val end = poseKeypoints[endIdx]
            canvas.drawLine(start.x, start.y, end.x, end.y, bonePaint)
        }

        // 绘制关键点
        for (i in poseKeypoints.indices) {
            val conf = poseConfidences.getOrElse(i) { 0f }
            if (conf < threshold) continue
            val point = poseKeypoints[i]
            canvas.drawCircle(point.x, point.y, 8f, keypointPaint)
        }
    }

    private fun drawFaceBoxes(canvas: Canvas) {
        for (faceRect in faceBoxes) {
            canvas.drawRect(faceRect, faceBoxPaint)
        }
    }

    private fun drawAlert(canvas: Canvas) {
        val text = alertText ?: return
        if (alertLevel == AlertLevel.NONE) return

        val bgColor = when (alertLevel) {
            AlertLevel.WARNING -> Color.parseColor("#DD1A1200")   // 深琥珀半透明
            AlertLevel.DANGER  -> Color.parseColor("#DD1A0010")   // 深红半透明
            else -> return
        }
        val glowColor = when (alertLevel) {
            AlertLevel.WARNING -> Color.parseColor("#FFB800")
            AlertLevel.DANGER  -> Color.parseColor("#FF3060")
            else -> return
        }
        alertBgPaint.color = bgColor
        alertPaint.color = glowColor

        val bgRect = RectF(
            width * 0.05f, height * 0.85f,
            width * 0.95f, height * 0.85f + 70f
        )
        canvas.drawRoundRect(bgRect, 12f, 12f, alertBgPaint)

        // 发光边框
        val borderPaint = Paint().apply {
            color = glowColor
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
            alpha = 180
        }
        canvas.drawRoundRect(bgRect, 12f, 12f, borderPaint)
        canvas.drawText(text, width / 2f, height * 0.85f + 48f, alertPaint)
    }

    // --- 公开接口: 更新绘制数据 ---

    fun updateDetectionBoxes(boxes: List<DetectionBox>) {
        this.detectionBoxes = boxes
        invalidate()
    }

    fun updatePoseKeypoints(keypoints: List<PointF>, confidences: List<Float>) {
        this.poseKeypoints = keypoints
        this.poseConfidences = confidences
        invalidate()
    }

    fun updateFaceBoxes(boxes: List<RectF>) {
        this.faceBoxes = boxes
        invalidate()
    }

    fun updateAlert(text: String?, level: AlertLevel) {
        this.alertText = text
        this.alertLevel = level
        invalidate()
    }

    fun updateScores(posture: Int, focus: Int) {
        this.postureScore = posture
        this.focusScore = focus
        invalidate()
    }

    fun clearAll() {
        detectionBoxes = emptyList()
        poseKeypoints = emptyList()
        poseConfidences = emptyList()
        faceBoxes = emptyList()
        alertText = null
        alertLevel = AlertLevel.NONE
        invalidate()
    }

    // --- 数据类 ---

    data class DetectionBox(
        val rect: RectF,
        val label: String,
        val confidence: Float
    )

    enum class AlertLevel {
        NONE, WARNING, DANGER
    }
}
