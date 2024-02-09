package com.example.test_ocr_reader.scanner

import android.util.Log
import com.example.test_ocr_reader.model.CreditCard
import com.google.firebase.ml.vision.text.FirebaseVisionText

class CreditCardScanner {

    companion object {
        private const val TAG = "SCAN_OCR"
        private const val CREDIT_CARD_PATTERN = "^(?:4[0-9]{12}(?:[0-9]{3})?|[25][1-7][0-9]{14}|6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|(?:2131|1800|35\\d{3})\\d{11})\$"

    }

fun extractCardHolderName(visionText: FirebaseVisionText): String {
        var cardHolderName = ""
        for (block in visionText.textBlocks) {
            val blockText = block.text
            // Assuming the holder's name is in the first block
            if (blockText.isNotBlank()) {
                cardHolderName = blockText
                break
            }
        }
        return cardHolderName
    }


    fun processCreditCards(firebaseVisionText: FirebaseVisionText): CreditCard {
        Log.d(TAG, firebaseVisionText.text)
        val creditCard = CreditCard(cardholderName = extractCardHolderName(firebaseVisionText))

        val words = firebaseVisionText.text.split("\n")
        for (word in words) {
            Log.e("TAG", word)
            //REGEX for detecting a credit card
            if (word.replace(" ", "").matches(Regex(CREDIT_CARD_PATTERN))){
                creditCard.creditCardNumber = word
            }
            if (word.contains("/")) {
                for (year in word.split(" ")) {
                    if (year.contains("/"))
                        creditCard.expirationDate = word
                }
            }
        }

        return creditCard
    }
}
