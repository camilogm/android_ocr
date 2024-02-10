package com.example.test_ocr_reader


import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions
import com.google.firebase.ml.vision.text.FirebaseVisionText
import android.widget.ImageView
import android.widget.Spinner
import androidx.lifecycle.lifecycleScope
import com.example.test_ocr_reader.scanner.BitmapUtils
import com.example.test_ocr_reader.scanner.CreditCardScanner
import com.example.test_ocr_reader.scanner.PassportScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception

class PassportScannerActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SCAN_OCR"
        private const val REQUEST_IMAGE_CAPTURE = 100
        private const val PICK_IMAGE_REQUEST = 1
    }

    enum class SCAN_TYPES(val scanType: String) {
        PASSPORT("Passport"),
        CREDIT_CARD("Credit Card")

    }

    private lateinit var scanPassportButton: Button
    private lateinit var imageView: ImageView


    private lateinit var scannedTextView: TextView
    private lateinit var uploadImageButton: Button
    private var selectedSpinnerItem: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.initialize( this)

        setContentView(R.layout.activity_passport_scanner)

        scanPassportButton = findViewById(R.id.scanPassportButton)
        scanPassportButton.setOnClickListener {
            dispatchTakePictureIntent()
        }

        uploadImageButton = findViewById(R.id.uploadImageButton)
        uploadImageButton.setOnClickListener {
            openGallery()
        }

        scannedTextView = findViewById(R.id.scannedTextView)
        imageView = findViewById(R.id.imageView)


        setupSpinner()
    }

    private fun setupSpinner() {
        val spinner = findViewById<Spinner>(R.id.scanOptions)
        val options = resources.getStringArray(R.array.scanner_options)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSpinnerItem = options[position]
                scannedTextView.text = ""
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            lifecycleScope.launch {
                try {
                    processImage(imageBitmap)
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
            }
        }

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val selectedImageUri = data.data
            imageView.setImageURI(selectedImageUri)
            imageView.drawable?.let { drawable ->
                if (drawable is BitmapDrawable) {
                    val bitmap = drawable.bitmap
                    lifecycleScope.launch{
                        try {
                            processImage(bitmap)
                        } catch (e: Exception) {
                            Log.e(TAG, e.toString())
                        }
                    }
                }
            }
        }
    }

    private suspend fun performTextRecognition(firebaseVisionImage: FirebaseVisionImage): FirebaseVisionText {
        return withContext(Dispatchers.IO) {
            FirebaseVisionCloudTextRecognizerOptions.Builder()
                .setLanguageHints(listOf("es"))
                .build()

            val cloudTextRecognizer = FirebaseVision.getInstance().cloudTextRecognizer
            val result = cloudTextRecognizer.processImage(firebaseVisionImage).await()
            result
        }
    }
    private fun updatePassportTextView(passport: String) {
        scannedTextView.text =  passport.toString();
    }

    private suspend fun processImage(bitmap: Bitmap) {
        val optimizedGrayScale = BitmapUtils.optimizeBitImage(1 , bitmap)

        val firebaseVisionImage = FirebaseVisionImage.fromBitmap(optimizedGrayScale)
        val visionText = performTextRecognition(firebaseVisionImage)

        Log.d(TAG, "scanning $selectedSpinnerItem")

        val result = when  {
            selectedSpinnerItem == SCAN_TYPES.PASSPORT.scanType -> PassportScanner().processMrz(visionText)
            selectedSpinnerItem == SCAN_TYPES.CREDIT_CARD.scanType -> CreditCardScanner().processCreditCards(visionText)
            else -> "x and y are incomparable"
        }

        updatePassportTextView(result.toString())

    }

}
