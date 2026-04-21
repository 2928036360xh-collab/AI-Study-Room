package com.example.end_side.util

import android.graphics.PointF
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

/**
 * 角度计算工具，用于坐姿分析中关键点之间的角度计算
 */
object AngleUtils {

    /**
     * 计算三点形成的角度（以中间点 b 为顶点）
     * @return 角度值，范围 [0, 180]
     */
    fun calculateAngle(a: PointF, b: PointF, c: PointF): Float {
        val radians = atan2(
            (c.y - b.y).toDouble(),
            (c.x - b.x).toDouble()
        ) - atan2(
            (a.y - b.y).toDouble(),
            (a.x - b.x).toDouble()
        )
        var angle = abs(radians * 180.0 / PI).toFloat()
        if (angle > 180f) {
            angle = 360f - angle
        }
        return angle
    }

    /**
     * 计算两点连线与水平线的倾斜角度
     * @return 角度值，范围 [0, 90]
     */
    fun calculateTiltAngle(a: PointF, b: PointF): Float {
        val deltaY = abs(a.y - b.y)
        val deltaX = abs(a.x - b.x)
        if (deltaX < 1e-6f) return 90f
        val radians = atan2(deltaY.toDouble(), deltaX.toDouble())
        return (radians * 180.0 / PI).toFloat()
    }

    /**
     * 计算两点之间的距离
     */
    fun distance(a: PointF, b: PointF): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}
