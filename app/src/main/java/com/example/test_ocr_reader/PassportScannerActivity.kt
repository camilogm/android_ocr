package com.example.test_ocr_reader


import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var uploadImageButton: Button
    private lateinit var imageView: ImageView
    private lateinit var scannedTextView: TextView

    private var selectedSpinnerItem: String? = null
    private var selectedImageConversion: Int? = null
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>


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


        setupDocumentSpinner()
        setUpImageConversionSpinner()
        // Initialize ActivityResultLauncher
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.extras?.get("data")?.let { image ->
                    if (image is Bitmap) {
                        lifecycleScope.launch {
                            try {
                                processImage(image)
                            } catch (e: Exception) {
                                Log.e(TAG, e.toString())
                            }
                        }
                    }
                }
            } else {
            }
        }
    }

    private fun setUpImageConversionSpinner(){
        val stepSpinner: Spinner = findViewById(R.id.stepSpinner)
        val options = listOf("No Conversion", "Grayscale", "Histogram Equalization")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        stepSpinner.adapter = adapter

        stepSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedImageConversion = position
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
    }

    private fun setupDocumentSpinner() {
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

    private suspend fun getBitmapFromUri(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        // Use content resolver to load bitmap from URI
        val inputStream = contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    }
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.let {
            takePictureLauncher.launch(takePictureIntent)
        }
    }


    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedImageUri ->
            imageView.setImageURI(selectedImageUri)
            // Get the bitmap from the URI asynchronously
            lifecycleScope.launch {
                try {
                    val bitmap = getBitmapFromUri(selectedImageUri)
                    processImage(bitmap)
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
            }
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
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
        val step = selectedImageConversion ?: 0
        val optimizedGrayScale = BitmapUtils.optimizeBitImage(step, bitmap)

        imageView.setImageBitmap(optimizedGrayScale)
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
