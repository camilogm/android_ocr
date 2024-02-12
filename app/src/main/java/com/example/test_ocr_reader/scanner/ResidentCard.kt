package com.example.test_ocr_reader.scanner

import android.util.Log
import com.example.test_ocr_reader.model.Passport
import com.google.firebase.ml.vision.text.FirebaseVisionText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Pattern

class ResidentCard {
    companion object {
        private const val TAG = "SCAN_OCR"
    }

    private fun processMrzLineOne(travelDocumentLine: String, passport: Passport): Passport {
        val regexResidentCardLine1 = Regex(PassportScanner.TravelDocumentType.RESIDENT_CARD.line1Regex)
        val matchResult = regexResidentCardLine1.find(travelDocumentLine)

        Log.d(TAG, "text $travelDocumentLine length: ${travelDocumentLine.length}")
        Log.d(TAG, "this line matches with first line: ${matchResult != null}")

        if (matchResult != null) {

            val (_typeDocument, country, documentNumber) = matchResult.destructured

            return passport.copy(documentNumber = documentNumber, expeditionCountry =  country)
        }

        return passport
    }

    private fun processMrzLineTwo(travelDocumentLine: String, passport: Passport): Passport {
        val dateFormat = SimpleDateFormat("yyMMdd")
        val regexResidentCardLine2 = Regex(PassportScanner.TravelDocumentType.RESIDENT_CARD.line2Regex)
        val matchResult = regexResidentCardLine2.find(travelDocumentLine)

        Log.d(TAG, "text $travelDocumentLine length: ${travelDocumentLine.length}")
        Log.d(TAG, "this line matches with second line: ${matchResult != null}")


        if (matchResult != null) {
            val (_separate, yearOfBirth, monthOfBirth, dayOfBirth) = matchResult.destructured
            val dateOfBirth = "$yearOfBirth$monthOfBirth$dayOfBirth"


            return passport.copy(dateOfBirthDay =  dateFormat.parse(dateOfBirth))
        }

        return passport
    }

    private fun processMrzLineThree(travelDocumentLine: String, passport: Passport): Passport {
        val regexResidentCardLine3 = Regex(PassportScanner.TravelDocumentType.RESIDENT_CARD.line3Regex)
        val matchResult = regexResidentCardLine3 .find(travelDocumentLine)

        Log.d(TAG, "text $travelDocumentLine length: ${travelDocumentLine.length}")
        Log.d(TAG, "this line matches with third line: ${matchResult != null}")

        if (matchResult != null) {
            val (_separate, givenName, _separate2, surName) = matchResult.destructured
            return passport.copy(givenName = givenName, surName = surName)
        }

        return passport
    }



    private fun processDocument(
        firebaseVisionText: FirebaseVisionText.TextBlock,
        passport: Passport
    ): Passport {
        val line = firebaseVisionText.text.uppercase().trim().replace(" ", "").replace("\n", "")

        // TODO: function composition
        val partialDocumentLine1 =  processMrzLineOne(line, passport)
        val partialDocumentLine2 = processMrzLineTwo(line, partialDocumentLine1)

        return processMrzLineThree(line, partialDocumentLine2)
    }

    fun processMrz(firebaseVisionText: FirebaseVisionText): Passport? {
        val passport = Passport("", Date(), Date(), "")

        return firebaseVisionText.textBlocks
            .fold(passport) { accPassport, block ->
                processDocument(block, accPassport) ?: accPassport
            }
    }
}