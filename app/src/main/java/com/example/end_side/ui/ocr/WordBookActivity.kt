package com.example.end_side.ui.ocr

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.end_side.R
import com.example.end_side.data.AppDatabase
import com.example.end_side.data.entity.WordItem
import com.example.end_side.data.network.RetrofitClient
import com.example.end_side.ui.adapter.WordListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 生词本页面
 * 展示所有收藏的生词，支持搜索、删除、在线查询释义
 */
class WordBookActivity : AppCompatActivity() {

    private lateinit var rvWords: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvTotal: TextView
    private lateinit var etSearch: EditText

    private val adapter = WordListAdapter(
        onDeleteClick = { word -> confirmDelete(word) },
        onItemClick = { word -> lookupWord(word) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_book)

        initViews()
        loadWords()
    }

    private fun initViews() {
        rvWords = findViewById(R.id.rv_words)
        tvEmpty = findViewById(R.id.tv_wordbook_empty)
        tvTotal = findViewById(R.id.tv_word_total)
        etSearch = findViewById(R.id.et_search_word)

        rvWords.layoutManager = LinearLayoutManager(this)
        rvWords.adapter = adapter

        findViewById<ImageView>(R.id.btn_wordbook_back).setOnClickListener { finish() }

        // 搜索过滤
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val keyword = s?.toString()?.trim() ?: ""
                searchWords(keyword)
            }
        })
    }

    private fun loadWords() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getInstance(this@WordBookActivity).wordItemDao()
                val words = dao.getAll()
                withContext(Dispatchers.Main) {
                    updateList(words)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun searchWords(keyword: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getInstance(this@WordBookActivity).wordItemDao()
                val words = if (keyword.isBlank()) dao.getAll() else dao.search(keyword)
                withContext(Dispatchers.Main) {
                    updateList(words)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateList(words: List<WordItem>) {
        adapter.submitList(words)
        tvTotal.text = "${words.size} 个"
        tvEmpty.visibility = if (words.isEmpty()) View.VISIBLE else View.GONE
        rvWords.visibility = if (words.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun confirmDelete(word: WordItem) {
        AlertDialog.Builder(this)
            .setTitle("删除确认")
            .setMessage("确定删除「${word.word}」吗？")
            .setPositiveButton(R.string.delete) { _, _ -> deleteWord(word) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteWord(word: WordItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                AppDatabase.getInstance(this@WordBookActivity).wordItemDao().deleteById(word.id)
                loadWords() // 刷新列表
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 使用 Retrofit 查询单词释义
     * 网络请求使用协程的 suspend 函数，结果切回主线程更新 UI
     */
    private fun lookupWord(word: WordItem) {
        if (word.translation.isNotBlank()) {
            // 已有释义，直接显示
            showTranslationDialog(word.word, word.translation)
            return
        }

        Toast.makeText(this, "正在查询释义...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.dictionaryApi.lookupWord(word.word)
                val translation = response.firstOrNull()?.meanings
                    ?.flatMap { it.definitions ?: emptyList() }
                    ?.take(3)
                    ?.mapIndexed { index, def -> "${index + 1}. ${def.definition}" }
                    ?.joinToString("\n")
                    ?: "未找到释义"

                // 更新数据库
                val dao = AppDatabase.getInstance(this@WordBookActivity).wordItemDao()
                dao.update(word.copy(translation = translation))

                // 切回主线程更新 UI
                withContext(Dispatchers.Main) {
                    showTranslationDialog(word.word, translation)
                    loadWords()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@WordBookActivity,
                        "查询失败：${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showTranslationDialog(word: String, translation: String) {
        AlertDialog.Builder(this)
            .setTitle(word)
            .setMessage(translation)
            .setPositiveButton(R.string.confirm, null)
            .show()
    }
}
