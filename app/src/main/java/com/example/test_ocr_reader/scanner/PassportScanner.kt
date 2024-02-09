package com.example.test_ocr_reader.scanner

import android.util.Log
import com.example.test_ocr_reader.model.Passport
import com.google.firebase.ml.vision.text.FirebaseVisionText
import java.text.SimpleDateFormat
import java.util.regex.Pattern

class PassportScanner {




    companion object {
        private const val TAG = "SCAN_OCR"
        private const val PASSPORT_TD_3_LINE_2_REGEX =
            "([A-Z0-9<]{9})([0-9]{1})([A-Z]{3})([0-9]{6})([0-9]{1})([M|F|X|<]{1})([0-9]{6})([0-9]{1})([A-Z0-9<]{14})([0-9<]{1})([0-9]{1})"
        private const val PASSPORT_TD_3_LINE_1_REGEX = "(P[A-Z0-9<]{1})([A-Z]{3})([A-Z0-9<]{39})"

    }

    private fun processMRZDataBlock(mrzData: FirebaseVisionText.TextBlock): Passport? {
        val dateFormat = SimpleDateFormat("yyMMdd")

        Log.d(TAG, mrzData.text)

        val patternPassportTD3Line1: Pattern = Pattern.compile(PASSPORT_TD_3_LINE_1_REGEX)
        val matcherPassportTD3Line1 = patternPassportTD3Line1.matcher(mrzData.text)
        val patternPassportTD3Line2 = Pattern.compile(PASSPORT_TD_3_LINE_2_REGEX)
        val matcherPassportTD3Line2 = patternPassportTD3Line2.matcher(mrzData.text)

        return if (matcherPassportTD3Line1.find() && matcherPassportTD3Line2.find()) {
            Log.d(TAG, "processing passport")
            val line1 = matcherPassportTD3Line1.group(0)
            val line2 = matcherPassportTD3Line2.group(0)
            val documentNumber = line2.substring(0, 9).replace("O", "0")
            val dateOfBirthDay = line2.substring(13, 19)
            val expiryDate = line2.substring(21, 27)
            val expiditionCountry = line1.substring(2, 5)

            Log.d(
                TAG,
                "Scanned Text Buffer Passport ->>>> Doc Number: $documentNumber DateOfBirth: $dateOfBirthDay ExpiryDate: $expiryDate"
            )
            Passport(documentNumber, dateFormat.parse(dateOfBirthDay), dateFormat.parse(expiryDate), expiditionCountry)
        } else {
            Log.d(TAG, "empty")
            null
        }
    }

    fun processMrz(firebaseVisionText: FirebaseVisionText): Passport? {
        return firebaseVisionText.textBlocks
            .mapNotNull { block ->
                PassportScanner().processMRZDataBlock(block)
            }
            .firstOrNull()
    }

}