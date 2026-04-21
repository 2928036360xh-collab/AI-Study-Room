package com.example.end_side.engine.analyzer

import android.graphics.RectF

/**
 * 专注力分析器
 * 综合人体检测和人脸检测结果判断专注程度
 *
 * 评分依据：
 * 1. 是否检测到人体 -> 人不在座位
 * 2. 是否检测到人脸 -> 是否面向屏幕
 * 3. 人脸大小变化 -> 是否离屏幕过远（打瞌睡/离开）
 * 4. 连续检测缓冲 -> 避免瞬时波动
 */
class FocusAnalyzer {

    companion object {
        private const val BUFFER_SIZE = 10  // 滑动窗口大小
    }

    // 历史检测缓冲区
    private val personDetectedBuffer = ArrayDeque<Boolean>(BUFFER_SIZE)
    private val faceDetectedBuffer = ArrayDeque<Boolean>(BUFFER_SIZE)
    private val faceAreaBuffer = ArrayDeque<Float>(BUFFER_SIZE)
    private var baselineFaceArea: Float = -1f  // 基准人脸面积

    /**
     * 分析专注力
     * @param personBoxes 人体检测框列表
     * @param faceBoxes 人脸检测框列表
     * @return 专注力评估结果
     */
    fun analyze(
        personBoxes: List<RectF>,
        faceBoxes: List<RectF>
    ): FocusResult {
        val hasPerson = personBoxes.isNotEmpty()
        val hasFace = faceBoxes.isNotEmpty()
        val faceArea = if (hasFace) {
            faceBoxes[0].width() * faceBoxes[0].height()
        } else {
            0f
        }

        // 更新缓冲区
        addToBuffer(personDetectedBuffer, hasPerson)
        addToBuffer(faceDetectedBuffer, hasFace)
        addToBuffer(faceAreaBuffer, faceArea)

        // 更新基准人脸面积（取前几帧的平均值）
        if (baselineFaceArea < 0 && faceAreaBuffer.size >= 3) {
            val validAreas = faceAreaBuffer.filter { it > 0 }
            if (validAreas.isNotEmpty()) {
                baselineFaceArea = validAreas.average().toFloat()
            }
        }

        // 评分计算
        val personRate = personDetectedBuffer.count { it }.toFloat() / personDetectedBuffer.size.coerceAtLeast(1)
        val faceRate = faceDetectedBuffer.count { it }.toFloat() / faceDetectedBuffer.size.coerceAtLeast(1)

        var score = 0f
        var status = FocusStatus.UNKNOWN
        val issues = mutableListOf<String>()

        when {
            personRate < 0.3f -> {
                // 大部分时间没检测到人
                score = 10f
                status = FocusStatus.ABSENT
                issues.add("未检测到人，可能已离开座位")
            }
            faceRate < 0.3f -> {
                // 人在但没看屏幕
                score = 30f
                status = FocusStatus.DISTRACTED
                issues.add("未检测到人脸，可能未面向屏幕")
            }
            else -> {
                // 基础分数
                score = personRate * 40f + faceRate * 40f

                // 人脸大小变化分析
                if (baselineFaceArea > 0 && faceArea > 0) {
                    val areaRatio = faceArea / baselineFaceArea
                    when {
                        areaRatio < 0.5f -> {
                            score -= 15f
                            issues.add("人脸过小，可能离屏幕过远")
                        }
                        areaRatio > 1.5f -> {
                            score -= 10f
                            issues.add("人脸过近，注意坐姿距离")
                        }
                        else -> score += 20f // 面积正常加分
                    }
                } else {
                    score += 10f
                }

                status = if (score >= 70) FocusStatus.FOCUSED else FocusStatus.DISTRACTED
            }
        }

        return FocusResult(
            score = score.toInt().coerceIn(0, 100),
            status = status,
            hasPerson = hasPerson,
            hasFace = hasFace,
            issues = issues
        )
    }

    fun reset() {
        personDetectedBuffer.clear()
        faceDetectedBuffer.clear()
        faceAreaBuffer.clear()
        baselineFaceArea = -1f
    }

    private fun <T> addToBuffer(buffer: ArrayDeque<T>, value: T) {
        if (buffer.size >= BUFFER_SIZE) {
            buffer.removeFirst()
        }
        buffer.addLast(value)
    }

    data class FocusResult(
        val score: Int,
        val status: FocusStatus,
        val hasPerson: Boolean,
        val hasFace: Boolean,
        val issues: List<String> = emptyList()
    )

    enum class FocusStatus {
        FOCUSED,    // 专注
        DISTRACTED, // 分心
        ABSENT,     // 离席
        UNKNOWN     // 初始化中
    }
}
