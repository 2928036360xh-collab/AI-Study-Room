package com.example.end_side.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * 自定义折线图控件
 * 用于学习报告中展示专注力/坐姿评分随时间的变化趋势
 */
class ChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var dataPoints: List<Float> = emptyList()
    private var chartTitle: String = ""
    private var lineColor: Int = Color.parseColor("#2196F3")

    private val linePaint = Paint().apply {
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val dotPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        strokeWidth = 1f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val titlePaint = Paint().apply {
        color = Color.parseColor("#424242")
        textSize = 32f
        isAntiAlias = true
    }

    private val axisLabelPaint = Paint().apply {
        color = Color.parseColor("#9E9E9E")
        textSize = 22f
        isAntiAlias = true
    }

    private val linePath = Path()
    private val fillPath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val paddingLeft = 60f
        val paddingRight = 20f
        val paddingTop = 50f
        val paddingBottom = 40f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        // 标题
        canvas.drawText(chartTitle, paddingLeft, 36f, titlePaint)

        // 网格线
        for (i in 0..4) {
            val y = paddingTop + chartHeight * i / 4f
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)
            val label = (100 - 25 * i).toString()
            canvas.drawText(label, 8f, y + 8f, axisLabelPaint)
        }

        // 折线
        val maxVal = 100f
        val minVal = 0f
        val range = maxVal - minVal

        linePaint.color = lineColor
        dotPaint.color = lineColor
        fillPaint.color = Color.argb(40, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor))

        linePath.reset()
        fillPath.reset()

        val stepX = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)

        for (i in dataPoints.indices) {
            val x = paddingLeft + stepX * i
            val normalizedY = (dataPoints[i] - minVal) / range
            val y = paddingTop + chartHeight * (1f - normalizedY)

            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, paddingTop + chartHeight)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            // 数据点圆点
            canvas.drawCircle(x, y, 6f, dotPaint)
        }

        // 填充区域封口
        val lastX = paddingLeft + stepX * (dataPoints.size - 1)
        fillPath.lineTo(lastX, paddingTop + chartHeight)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)
    }

    fun setData(points: List<Float>, title: String = "", color: Int = Color.parseColor("#2196F3")) {
        this.dataPoints = points
        this.chartTitle = title
        this.lineColor = color
        invalidate()
    }
}
