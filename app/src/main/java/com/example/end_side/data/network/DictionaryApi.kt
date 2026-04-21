package com.example.end_side.data.network

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * 免费词典 API 接口 (https://api.dictionaryapi.dev)
 * 用于查询英文单词释义
 */
interface DictionaryApi {

    @GET("api/v2/entries/en/{word}")
    suspend fun lookupWord(@Path("word") word: String): List<DictionaryResponse>
}

data class DictionaryResponse(
    val word: String?,
    val meanings: List<Meaning>?
)

data class Meaning(
    val partOfSpeech: String?,
    val definitions: List<Definition>?
)

data class Definition(
    val definition: String?
)
