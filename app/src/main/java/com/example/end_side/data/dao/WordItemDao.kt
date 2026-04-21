package com.example.end_side.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.end_side.data.entity.WordItem

@Dao
interface WordItemDao {

    @Insert
    suspend fun insert(word: WordItem): Long

    @Update
    suspend fun update(word: WordItem)

    @Query("SELECT * FROM word_book ORDER BY addTime DESC")
    suspend fun getAll(): List<WordItem>

    @Query("SELECT * FROM word_book WHERE isFavorite = 1 ORDER BY addTime DESC")
    suspend fun getFavorites(): List<WordItem>

    @Query("SELECT * FROM word_book WHERE word LIKE '%' || :keyword || '%' OR translation LIKE '%' || :keyword || '%' ORDER BY addTime DESC")
    suspend fun search(keyword: String): List<WordItem>

    @Query("DELETE FROM word_book WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM word_book")
    suspend fun getCount(): Int
}
