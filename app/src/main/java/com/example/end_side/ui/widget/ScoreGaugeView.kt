package com.example.end_side.ui.widget

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.end_side.R

/**
 * 科技感发光圆弧仪表盘控件
 * 参考风格：深海底+霓虹发光弧线，仿HUD效果
 */
class ScoreGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var score: Int = 0
    private var maxScore: Int = 100
    private var label: String = ""

    // 弧轨道：深色半透明
    private val bgArcPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.gauge_track)
        strokeWidth = 14f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    // 外层发光（最宽最透明）
    private val glowOuterPaint = Paint().apply {
        strokeWidth = 36f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
        maskFilter = BlurMaskFilter(22f, BlurMaskFilter.Blur.NORMAL)
    }

    // 中层发光
    private val glowInnerPaint = Paint().apply {
        strokeWidth = 22f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
        maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
    }

    // 主弧线（清晰锐利）
    private val fgArcPaint = Paint().apply {
        strokeWidth = 14f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    // 分数数字
    private val scorePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.gauge_text)
        textSize = 58f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }

    // 分数单位
    private val unitPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.gauge_label)
        textSize = 22f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // 标签
    private val labelPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.gauge_label)
        textSize = 26f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // 状态文字（良好/注意/危险）
    private val statusPaint = Paint().apply {
        textSize = 22f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val arcRect = RectF()
    private val glowRect = RectF()

    init {
        // 软件层渲染：BlurMaskFilter 需要关闭硬件加速
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val size = minOf(w, h)
        val cx = w / 2f
        val cy = h / 2f - 12f

        val pad = size * 0.14f
        val arcSize = size - pad * 2

        arcRect.set(cx - arcSize / 2, cy - arcSize / 2, cx + arcSize / 2, cy + arcSize / 2)

        // --- 背景弧（轨道）---
        canvas.drawArc(arcRect, 135f, 270f, false, bgArcPaint)

        if (score > 0) {
            val sweepAngle = 270f * score.coerceIn(0, maxScore) / maxScore
            val arcColor = getScoreColor(score)

            // --- 外层大光晕 ---
            glowOuterPaint.color = arcColor
            glowOuterPaint.alpha = 45
            canvas.drawArc(arcRect, 135f, sweepAngle, false, glowOuterPaint)

            // --- 中层光晕 ---
            glowInnerPaint.color = arcColor
            glowInnerPaint.alpha = 90
            canvas.drawArc(arcRect, 135f, sweepAngle, false, glowInnerPaint)

            // --- 主弧线（清晰）---
            fgArcPaint.color = arcColor
            canvas.drawArc(arcRect, 135f, sweepAngle, false, fgArcPaint)
        }

        // --- 分数文字 ---
        canvas.drawText(score.toString(), cx, cy + 20f, scorePaint)

        // --- 标签 ---
        canvas.drawText(label, cx, cy + 50f, labelPaint)

        // --- 状态文字 ---
        val statusText = when {
            score >= 80 -> "良好"
            score >= 60 -> "注意"
            score > 0   -> "危险"
            else        -> "--"
        }
        statusPaint.color = if (score > 0) getScoreColor(score) else ContextCompat.getColor(context, R.color.gauge_status_empty)
        canvas.drawText(statusText, cx, cy - arcSize / 2 - 6f, statusPaint)
    }

    fun setScore(value: Int) {
        score = value.coerceIn(0, maxScore)
        invalidate()
    }

    fun setLabel(text: String) {
        label = text
        invalidate()
    }

    private fun getScoreColor(score: Int): Int = when {
        score >= 80 -> ContextCompat.getColor(context, R.color.score_good)   // 良好
        score >= 60 -> ContextCompat.getColor(context, R.color.score_warning) // 注意
        else        -> ContextCompat.getColor(context, R.color.score_danger)  // 危险
    }
}

