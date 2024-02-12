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
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.test_ocr_reader.scanner.AmericanVisaExtract
import com.example.test_ocr_reader.scanner.BitmapUtils
import com.example.test_ocr_reader.scanner.CreditCardScanner
import com.example.test_ocr_reader.scanner.PassportScanner
import com.example.test_ocr_reader.scanner.ResidentCard
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions
import com.google.firebase.ml.vision.text.FirebaseVisionText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class PassportScannerActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SCAN_OCR"
        private const val REQUEST_IMAGE_CAPTURE = 100
        private const val PICK_IMAGE_REQUEST = 1
    }

    enum class SCAN_TYPES(val scanType: Int) {
        PASSPORT(0),
        VISA_ICAO(1),
        VISA_ANOTHER_PATTERN(2),
        CREDIT_CARD(3),
        RESIDENT_CARD(4),
    }

    private lateinit var scanPassportButton: Button
    private lateinit var uploadImageButton: Button
    private lateinit var imageView: ImageView
    private lateinit var scannedTextView: TextView
    private lateinit var imageContainer: RelativeLayout

    private var selectedTravelDocument: Int? = null
    private var selectedImageConversion: Int? = null
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>

    lateinit var currentPhotoPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.initialize(this)

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
        imageContainer = findViewById(R.id.imageContainer)

        setupDocumentSpinner()
        setUpImageConversionSpinner()
        // Initialize ActivityResultLauncher
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                lifecycleScope.launch {
                    try {
                        val file = File(currentPhotoPath)
                        val bitmap = getBitmapFromUri(Uri.fromFile(file))
                        processImage(bitmap)
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString())
                    }
                }
            }
        }
    }

    private fun setUpImageConversionSpinner() {
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
        val scannerOptions = arrayOf(
            "Passport",
            "VISA ICAO closer pattern",
            "VISA another Pattern",
            "Credit Card",
            "Resident card",
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, scannerOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedTravelDocument = position
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
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        application,
                        "${application.packageName}.provider",
                        it,
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePictureLauncher.launch(takePictureIntent)
                }
            }
        }
    }

    fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            application.cacheDir, /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
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
        hideResult()
        return withContext(Dispatchers.IO) {
            FirebaseVisionCloudTextRecognizerOptions.Builder()
                .setLanguageHints(listOf("es"))
                .build()

            val cloudTextRecognizer = FirebaseVision.getInstance().cloudTextRecognizer
            val result = cloudTextRecognizer.processImage(firebaseVisionImage).await()
            result
        }
    }

    private fun updatePassportTextView(documentData: String) {
        scannedTextView.text = documentData.toString()
        imageContainer.visibility = View.VISIBLE
        val animation = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up)
        imageContainer.startAnimation(animation)
    }

    private fun hideResult() {
        scannedTextView.text = ""
        imageContainer.visibility = View.GONE
    }

    private suspend fun processImage(bitmap: Bitmap) {
        val step = selectedImageConversion ?: 0
        val optimizedGrayScale = BitmapUtils.optimizeBitImage(step, bitmap)

        imageView.setImageBitmap(optimizedGrayScale)
        val firebaseVisionImage = FirebaseVisionImage.fromBitmap(optimizedGrayScale)
        val visionText = performTextRecognition(firebaseVisionImage)

        Log.d(TAG, "scanning $selectedTravelDocument")

        val result = when (selectedTravelDocument) {
            SCAN_TYPES.PASSPORT.scanType -> PassportScanner().processMrz(
                visionText,
                PassportScanner.TravelDocumentType.PASSPORT_TD_3,
            )
            SCAN_TYPES.CREDIT_CARD.scanType -> CreditCardScanner().processCreditCards(visionText)
            SCAN_TYPES.VISA_ANOTHER_PATTERN.scanType -> PassportScanner().processMrz(
                visionText,
                PassportScanner.TravelDocumentType.VISA_ANOTHER_PATTERN,
            )
            SCAN_TYPES.VISA_ICAO.scanType -> AmericanVisaExtract().processMrz(visionText)
            SCAN_TYPES.RESIDENT_CARD.scanType -> ResidentCard().processMrz(visionText)
            else -> "x and y are incomparable"
        }

        updatePassportTextView(result.toString())
    }
}
