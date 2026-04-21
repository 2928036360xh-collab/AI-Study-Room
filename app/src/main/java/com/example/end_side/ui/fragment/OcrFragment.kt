package com.example.end_side.ui.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.end_side.R
import com.example.end_side.ui.ocr.OcrResultActivity
import com.google.android.material.button.MaterialButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OcrFragment : Fragment() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }

    private lateinit var btnCamera: MaterialButton
    private lateinit var btnGallery: MaterialButton
    private var currentPhotoUri: Uri? = null

    // Activity Result API: 拍照回调
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            navigateToOcrResult(currentPhotoUri!!)
        }
    }

    // Activity Result API: 选择图片回调
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                navigateToOcrResult(uri)
            }
        }
    }

    // 相机权限请求
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), R.string.permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_ocr, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnCamera = view.findViewById(R.id.btn_ocr_camera)
        btnGallery = view.findViewById(R.id.btn_ocr_gallery)

        val cardWordBook = view.findViewById<View>(R.id.card_word_book)

        btnCamera.setOnClickListener {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }

        btnGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        cardWordBook.setOnClickListener {
            val intent = Intent(requireContext(), com.example.end_side.ui.ocr.WordBookActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * 使用 FileProvider 创建照片文件 URI 并启动相机
     */
    private fun launchCamera() {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "OCR_${timeStamp}.jpg"
            val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val photoFile = File(storageDir, imageFileName)

            currentPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "com.example.end_side.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(currentPhotoUri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "无法启动相机", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToOcrResult(imageUri: Uri) {
        val intent = Intent(requireContext(), OcrResultActivity::class.java).apply {
            putExtra(EXTRA_IMAGE_URI, imageUri.toString())
        }
        startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
    }
}
