package com.example.end_side.engine.analyzer

import android.graphics.PointF
import com.example.end_side.util.AngleUtils

/**
 * 坐姿分析器
 * 基于 17 个 COCO 关键点计算坐姿评分
 *
 * 评分维度：
 * 1. 头部倾斜度 (双眼/双耳连线与水平方向的夹角)
 * 2. 肩膀倾斜度 (双肩连线与水平方向的夹角)
 * 3. 躯干前倾角 (肩中点-髋中点连线与垂直方向的夹角)
 * 4. 左右对称性 (左右肩到髋部距离的比值)
 */
class PostureAnalyzer {

    companion object {
        private const val NUM_KEYPOINTS = 17

        // 关键点索引 (COCO 17 格式)
        private const val NOSE = 0
        private const val LEFT_EYE = 1
        private const val RIGHT_EYE = 2
        private const val LEFT_EAR = 3
        private const val RIGHT_EAR = 4
        private const val LEFT_SHOULDER = 5
        private const val RIGHT_SHOULDER = 6
        private const val LEFT_HIP = 11
        private const val RIGHT_HIP = 12

        // 置信度阈值
        private const val MIN_CONFIDENCE = 0.3f

        // 坐姿评分阈值
        private const val HEAD_TILT_GOOD = 8f     // 头部倾斜 < 8° 为好
        private const val HEAD_TILT_BAD = 20f      // 头部倾斜 > 20° 为差
        private const val SHOULDER_TILT_GOOD = 5f
        private const val SHOULDER_TILT_BAD = 15f
        private const val TRUNK_LEAN_GOOD = 10f    // 躯干前倾 < 10° 为好
        private const val TRUNK_LEAN_BAD = 30f
    }

    /**
     * 分析坐姿，返回综合评分和各维度详情
     * @param keypoints COCO 17 格式关键点坐标
     * @param confidences 每个关键点的置信度
     */
    fun analyze(keypoints: List<PointF>, confidences: List<Float>): PostureResult {
        if (keypoints.size != NUM_KEYPOINTS) {
            return PostureResult(score = 0, isValid = false)
        }

        val kps = keypoints
        val confs = confidences

        val scores = mutableListOf<Float>()
        val issues = mutableListOf<String>()

        // 1. 头部倾斜度分析
        if (confs[LEFT_EYE] > MIN_CONFIDENCE && confs[RIGHT_EYE] > MIN_CONFIDENCE) {
            val headTilt = AngleUtils.calculateTiltAngle(kps[LEFT_EYE], kps[RIGHT_EYE])
            val headScore = calculateDimensionScore(headTilt, HEAD_TILT_GOOD, HEAD_TILT_BAD)
            scores.add(headScore)
            if (headScore < 60f) issues.add("头部倾斜过大")
        }

        // 2. 肩膀倾斜度分析
        if (confs[LEFT_SHOULDER] > MIN_CONFIDENCE && confs[RIGHT_SHOULDER] > MIN_CONFIDENCE) {
            val shoulderTilt = AngleUtils.calculateTiltAngle(kps[LEFT_SHOULDER], kps[RIGHT_SHOULDER])
            val shoulderScore = calculateDimensionScore(shoulderTilt, SHOULDER_TILT_GOOD, SHOULDER_TILT_BAD)
            scores.add(shoulderScore)
            if (shoulderScore < 60f) issues.add("肩膀高低不平")
        }

        // 3. 躯干前倾角分析
        if (confs[LEFT_SHOULDER] > MIN_CONFIDENCE && confs[RIGHT_SHOULDER] > MIN_CONFIDENCE
            && confs[LEFT_HIP] > MIN_CONFIDENCE && confs[RIGHT_HIP] > MIN_CONFIDENCE
        ) {
            val shoulderMid = midPoint(kps[LEFT_SHOULDER], kps[RIGHT_SHOULDER])
            val hipMid = midPoint(kps[LEFT_HIP], kps[RIGHT_HIP])
            // 用垂线偏移计算前倾: 理想状态肩膀正上方，dx 应接近 0
            val dx = kotlin.math.abs(shoulderMid.x - hipMid.x)
            val dy = kotlin.math.abs(shoulderMid.y - hipMid.y).coerceAtLeast(1f)
            val leanAngle = kotlin.math.atan2(dx.toDouble(), dy.toDouble())
                .let { it * 180.0 / kotlin.math.PI }.toFloat()
            val trunkScore = calculateDimensionScore(leanAngle, TRUNK_LEAN_GOOD, TRUNK_LEAN_BAD)
            scores.add(trunkScore)
            if (trunkScore < 60f) issues.add("身体前倾过多")
        }

        // 综合评分
        val finalScore = if (scores.isNotEmpty()) {
            scores.average().toInt().coerceIn(0, 100)
        } else {
            0
        }

        return PostureResult(
            score = finalScore,
            isValid = scores.isNotEmpty(),
            issues = issues
        )
    }

    /**
     * 将角度映射到分数 [0, 100]
     */
    private fun calculateDimensionScore(angle: Float, goodThreshold: Float, badThreshold: Float): Float {
        return when {
            angle <= goodThreshold -> 100f
            angle >= badThreshold -> 20f
            else -> {
                val ratio = (angle - goodThreshold) / (badThreshold - goodThreshold)
                100f - ratio * 80f
            }
        }
    }

    private fun midPoint(a: PointF, b: PointF): PointF {
        return PointF((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    }

    data class PostureResult(
        val score: Int,
        val isValid: Boolean,
        val issues: List<String> = emptyList()
    )
}
