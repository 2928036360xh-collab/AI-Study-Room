package com.example.end_side.ui.ocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.end_side.R
import com.example.end_side.data.AppDatabase
import com.example.end_side.data.entity.WordItem
import com.example.end_side.data.network.RetrofitClient
import com.example.end_side.ui.fragment.OcrFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * OCR 识别结果页面
 * 使用 ML Kit 进行文字识别，支持中英文
 * 支持复制文本、查词、添加生词
 */
class OcrResultActivity : AppCompatActivity() {

    private lateinit var ivPreview: ImageView
    private lateinit var tvResult: TextView
    private lateinit var progressBar: ProgressBar
    private var recognizedText = ""

    // ML Kit 文字识别器（中文模型，同时支持英文/拉丁文）
    private val textRecognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocr_result)

        initViews()
        processImage()
    }

    private fun initViews() {
        ivPreview = findViewById(R.id.iv_ocr_preview)
        tvResult = findViewById(R.id.tv_ocr_result)
        progressBar = findViewById(R.id.progress_ocr)

        findViewById<ImageView>(R.id.btn_ocr_back).setOnClickListener { finish() }

        findViewById<ImageView>(R.id.btn_copy_text).setOnClickListener {
            if (recognizedText.isNotBlank()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("OCR Result", recognizedText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<MaterialButton>(R.id.btn_add_to_wordbook).setOnClickListener {
            showAddWordDialog()
        }

        findViewById<MaterialButton>(R.id.btn_goto_wordbook).setOnClickListener {
            startActivity(Intent(this, WordBookActivity::class.java))
        }
    }

    private fun processImage() {
        val imageUriStr = intent.getStringExtra(OcrFragment.EXTRA_IMAGE_URI) ?: return

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(imageUriStr)
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    // 预览图片
                    withContext(Dispatchers.Main) {
                        ivPreview.setImageBitmap(bitmap)
                    }

                    // ML Kit 文字识别
                    val inputImage = InputImage.fromBitmap(bitmap, 0)

                    withContext(Dispatchers.Main) {
                        textRecognizer.process(inputImage)
                            .addOnSuccessListener { visionText ->
                                progressBar.visibility = View.GONE
                                recognizedText = visionText.text
                                tvResult.text = if (recognizedText.isNotBlank()) {
                                    recognizedText
                                } else {
                                    "未识别到文字，请尝试调整图片角度或清晰度"
                                }
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = View.GONE
                                tvResult.text = "识别失败：${e.message}"
                            }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    tvResult.text = "识别失败：${e.message}"
                }
            }
        }
    }

    /**
     * 弹出添加生词对话框，支持查词+添加
     */
    private fun showAddWordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_study_ocr, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_ocr_title)
        val tvOcrResult = dialogView.findViewById<TextView>(R.id.tv_dialog_ocr_result)
        val progressOcr = dialogView.findViewById<ProgressBar>(R.id.progress_dialog_ocr)
        val layoutActions = dialogView.findViewById<View>(R.id.layout_word_actions)
        val etWord = dialogView.findViewById<TextInputEditText>(R.id.et_dialog_word)
        val tvTranslation = dialogView.findViewById<TextView>(R.id.tv_dialog_translation)
        val btnLookup = dialogView.findViewById<MaterialButton>(R.id.btn_dialog_lookup)
        val btnAdd = dialogView.findViewById<MaterialButton>(R.id.btn_dialog_add_word)

        tvTitle.text = "添加生词"
        progressOcr.visibility = View.GONE
        tvOcrResult.text = if (recognizedText.isNotBlank()) recognizedText else "（暂无识别文本）"
        layoutActions.visibility = View.VISIBLE

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // 查词
        btnLookup.setOnClickListener {
            val word = etWord.text?.toString()?.trim() ?: ""
            if (word.isBlank()) {
                Toast.makeText(this, "请输入要查询的单词", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btnLookup.isEnabled = false
            btnLookup.text = "查询中…"
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.dictionaryApi.lookupWord(word)
                    val translation = response.firstOrNull()?.meanings
                        ?.flatMap { it.definitions ?: emptyList() }
                        ?.take(3)
                        ?.mapIndexed { index, def -> "${index + 1}. ${def.definition}" }
                        ?.joinToString("\n")
                        ?: "未找到释义"
                    withContext(Dispatchers.Main) {
                        tvTranslation.text = translation
                        tvTranslation.visibility = View.VISIBLE
                        btnLookup.isEnabled = true
                        btnLookup.text = "查词"
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        tvTranslation.text = "查询失败：${e.message}"
                        tvTranslation.visibility = View.VISIBLE
                        btnLookup.isEnabled = true
                        btnLookup.text = "查词"
                    }
                }
            }
        }

        // 添加生词
        btnAdd.setOnClickListener {
            val word = etWord.text?.toString()?.trim() ?: ""
            if (word.isBlank()) {
                Toast.makeText(this, "请输入要添加的生词", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val translation = if (tvTranslation.visibility == View.VISIBLE)
                tvTranslation.text.toString() else ""
            saveWord(word, translation)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveWord(word: String, translation: String = "") {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getInstance(this@OcrResultActivity).wordItemDao()
                dao.insert(
                    WordItem(
                        word = word,
                        translation = translation,
                        sourceText = recognizedText.take(100)
                    )
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OcrResultActivity, "已添加到生词本", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        textRecognizer.close()
        super.onDestroy()
    }
}
