package com.example.test_ocr_reader.scanner

import android.util.Log
import com.example.test_ocr_reader.model.CreditCard
import com.google.firebase.ml.vision.text.FirebaseVisionText
import java.text.SimpleDateFormat
class CreditCardScanner {

    companion object {
        private const val TAG = "SCAN_OCR"
        private val CREDIT_CARD_PATTERN = Regex("^(4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|(34|37)[0-9]{13}|3[0-9]{14}|636[0-9]{13})\$")
        private val CVC_PATTERN = Regex("^\\d{3,4}\$")
    }


    private fun extractMMYY(text: String): String? {
        val regex = """(?<!\d)(\d{1,2}/\d{2})(?!\d)""".toRegex()
        val matchResult = regex.find(text)
        return matchResult?.value
    }

    /**
     * useful when the card has expedition date and expiration date
     */
    private fun compareDates(date1: String, date2: String): String {
        val format = SimpleDateFormat("MM/yy")

        try {
            if (date1 == null || date1.isEmpty()){
                return date2
            }

            val parsedDate1 = format.parse(this.extractMMYY(date1))
            val parsedDate2 = format.parse(this.extractMMYY(date2))

            return if (parsedDate1.after(parsedDate2)) date1 else date2
        }catch (e: Exception) {
            Log.e(TAG, e.toString())
        }

        return this.extractMMYY(date2) ?: ""
    }

    private fun isValidCreditCard(creditCardNumber: String): Boolean {
        return CREDIT_CARD_PATTERN.matches(creditCardNumber.replace(" ", ""))
    }
    private fun isValidCVC(cvc: String, isAmex: Boolean = false): Boolean {


        val extractedCVC = if (!isAmex) {
            cvc.takeLast(3)
        } else {

            cvc.takeLast(4)
        }
        return CVC_PATTERN.matches(extractedCVC)
    }

    fun processCreditCards(firebaseVisionText: FirebaseVisionText): CreditCard {
        Log.d(TAG, firebaseVisionText.text)
        val creditCard = CreditCard()

        val words = firebaseVisionText.text.split("\n")
        for (word in words) {
            if (isValidCreditCard(word)){
                creditCard.creditCardNumber = word.replace(" ", "")
            }
            if (word.contains("/")) {
                for (year in word.split(" ")) {
                    if (year.contains("/")) {
                        creditCard.expirationDate = compareDates(creditCard.expirationDate, word)
                    }
                }
            }

            if (isValidCVC(word.takeLast(4)) && !creditCard.creditCardNumber.contains(word.takeLast(4))){
                creditCard.cvc = word.takeLast(4)
            }
        }

        return creditCard
    }
}
