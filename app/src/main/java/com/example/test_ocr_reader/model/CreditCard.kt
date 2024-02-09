package com.example.test_ocr_reader.model

class CreditCard(
    var creditCardNumber: String = "",
    var expirationDate: String = "",
    val cardholderName: String = ""
) {
    override fun toString(): String {
        return "credit card: $creditCardNumber  expirationDate $expirationDate cardholdername $cardholderName"
    }
}