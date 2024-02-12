package com.example.test_ocr_reader.scanner

import android.util.Log
import com.example.test_ocr_reader.model.Passport
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionText.TextBlock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Pattern

class PassportScanner {

    enum class TravelDocumentType(
        val line1Regex: String,
        val line2Regex: String
    ) {
        PASSPORT_TD_3(
            "(P[A-Z0-9<]{1})([A-Z]{3})([A-Z0-9<]{39})",
            "([A-Z0-9<]{9})([0-9]{1})([A-Z]{3})([0-9]{6})([0-9]{1})([M|F|X|<]{1})([0-9]{6})([0-9]{1})([A-Z0-9<]{14})([0-9<]{1})([0-9]{1})"
        ),
        VISA_ANOTHER_PATTERN(
            "(V[A-Z0-9<]{1})([A-Z]{3})([A-Z0-9<]{31,})",
            "([A-Z0-9<]{9})([0-9]{1})([A-Z]{3})([0-9]{6})([0-9]{1})([M|F|X|<]{1})([0-9]{6})([0-9]{1})(<+)"
        ),
        VISA_ICAO_PATTERN(
            "VN([A-Z]{3})([A-Z]+)<<([A-Z]+)<<*",
            "^([A-Z0-9<]{31})\$"
        )
    }
    companion object {
        private const val TAG = "SCAN_OCR"
    }

    private fun processMrzLineOne(travelDocumentLine: String, passport: Passport, pattern:String): Passport {

        val patternPassportTD3Line1: Pattern = Pattern.compile(pattern)
        val matcherPassportTD3Line1 = patternPassportTD3Line1.matcher(travelDocumentLine)

        val isLineOneProcessable = matcherPassportTD3Line1.find()
        Log.d(TAG, "text $travelDocumentLine length: ${travelDocumentLine.length}")
        Log.d(TAG, "this line matches with first line: $isLineOneProcessable")

        return if (isLineOneProcessable) {

            val line1 = matcherPassportTD3Line1.group(0)
            val expeditionCountry: String = line1.substring(2, 5)

            val groupValues =  Regex(pattern).find(travelDocumentLine)?.groupValues
            val surname = groupValues?.getOrNull(2)
            val givenName = groupValues?.getOrNull(3)

            passport.copy(expeditionCountry = expeditionCountry, surName = surname ?: "", givenName = givenName ?: "")
        } else {
            Log.d(TAG, "the document was labeled as empty")
            passport
        }
    }

    private fun processMrzLineTwo(travelDocumentLine: String, passport: Passport, pattern: String): Passport {
        val dateFormat = SimpleDateFormat("yyMMdd")


        val patternPassportTD3Line2 = Pattern.compile(pattern)
        val matcherPassportTD3Line2 = patternPassportTD3Line2.matcher(travelDocumentLine)

        val isLineTwoProcessable = matcherPassportTD3Line2.find()
        Log.d(TAG, "text $travelDocumentLine length: ${travelDocumentLine.length}")
        Log.d(TAG, "this line matches with second line: $isLineTwoProcessable")

        return if (isLineTwoProcessable) {

            val line2 = matcherPassportTD3Line2.group(0)
            val documentNumber = line2.substring(0, 9).replace("O", "0")
            val dateOfBirthDay = line2.substring(13, 19)
            val expiryDate = line2.substring(21, 27)

            Log.d(
                TAG,
                "Scanned Text Buffer Passport ->>>> Doc Number: $documentNumber DateOfBirth: $dateOfBirthDay ExpiryDate: $expiryDate"
            )
            passport.copy(
                documentNumber = documentNumber,
                dateOfBirthDay = dateFormat.parse(dateOfBirthDay),
                expiryDate = dateFormat.parse(expiryDate)
            )
        } else {
            Log.d(TAG, "the document was labeled as empty")
            passport
        }
    }

    private fun processDocument(
        firebaseVisionText: TextBlock,
        passport: Passport,
        option: TravelDocumentType
    ): Passport {
        val line = firebaseVisionText.text.uppercase().trim().replace(" ", "").replace("\n", "")

        val passportLineOne = processMrzLineOne(line, passport, option.line1Regex)

        return processMrzLineTwo(line, passportLineOne, option.line2Regex)
    }

    fun processMrz(firebaseVisionText: FirebaseVisionText, option: TravelDocumentType ): Passport? {
        val passport = Passport("", Date(), Date(), "")

        return firebaseVisionText.textBlocks
            .fold(passport) { accPassport, block ->
                processDocument(block, accPassport, option) ?: accPassport
            }
    }

}