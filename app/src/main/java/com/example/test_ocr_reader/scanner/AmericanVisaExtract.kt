package com.example.test_ocr_reader.scanner

import android.util.Log
import com.example.test_ocr_reader.model.Passport
import com.google.firebase.ml.vision.text.FirebaseVisionText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Pattern

/**
 * COPY PASTE OF PASSPORT SCANNER BECAUSE OF TIME TO CREATE THIS PROCESS IN A GENERIC WAY
 */
class AmericanVisaExtract {

    companion object {
        private const val TAG = "SCAN_OCR"
    }

    private fun processMrzLineOne(travelDocumentLine: String, passport: Passport): Passport {
        val pattern= PassportScanner.TravelDocumentType.VISA_ICAO_PATTERN.line1Regex
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

            val newDocument = passport.copy(expeditionCountry = expeditionCountry, surName = surname ?: "", givenName = givenName ?: "")

            return processMrzLineTwo(travelDocumentLine, newDocument)
        } else {
            Log.d(TAG, "the document was labeled as empty")
            passport
        }
    }

    private fun processMrzLineTwo(travelDocumentLine: String, passport: Passport): Passport {
        val dateFormat = SimpleDateFormat("yyMMdd")

        val regexVisa = Regex("([A-Z0-9<]{10})([A-Z]{3})(\\d{2})(\\d{2})(\\d{2})(\\d{1})([MF])(\\d{2})(\\d{2})(\\d{2})")
        val matchResult = regexVisa.find(travelDocumentLine)
        Log.d(TAG, "evaluate in second: $travelDocumentLine , result: ${matchResult != null}")

        if (matchResult != null) {
            val (passportNumber, nationality, year, month, day, _no, gender, expYear, expMonth, expDay) = matchResult.destructured
            Log.d(TAG, "Passport Number: $passportNumber")
            Log.d(TAG,"Nationality: $nationality")
            Log.d(TAG,"Date of Birth: $year/$month/$day")

            Log.d(TAG,"Gender: ${if (gender == "M") "Male" else "Female"}")
            Log.d(TAG,"Expiration Date: $expYear/$expMonth/$expDay")

            val dateOfBirth = "$year$month$day"
            val expirationDate = "$expYear$expMonth$expDay"
            return passport.copy( documentNumber = passportNumber,
                dateOfBirthDay = dateFormat.parse(dateOfBirth),
                expiryDate = dateFormat.parse(expirationDate)
            )
        }

        return passport
    }


    private fun processDocument(
        firebaseVisionText: FirebaseVisionText.TextBlock,
        passport: Passport
    ): Passport {
        val line = firebaseVisionText.text.uppercase().trim().replace(" ", "").replace("\n", "")

        val partialDocument =  processMrzLineOne(line, passport)

        return processMrzLineTwo(line, partialDocument)
    }

    fun processMrz(firebaseVisionText: FirebaseVisionText): Passport? {
        val passport = Passport("", Date(), Date(), "")

        return firebaseVisionText.textBlocks
            .fold(passport) { accPassport, block ->
                processDocument(block, accPassport) ?: accPassport
            }
    }
}