package com.example.end_side.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * OCR 生词本实体 - 存储用户收藏的生词
 */
@Entity(tableName = "word_book")
data class WordItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,                // 单词
    val translation: String = "",    // 翻译/释义
    val sourceText: String = "",     // OCR 识别的原文片段
    val addTime: Long = System.currentTimeMillis(), // 添加时间
    val isFavorite: Boolean = false  // 是否星标
)
