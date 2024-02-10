package com.example.test_ocr_reader.model

class CreditCard(
    var creditCardNumber: String = "",
    var expirationDate: String = "",
    val cardholderName: String = "",
    var cvc: String = ""
) {
    override fun toString(): String {
        return "credit card: $creditCardNumber  \n expirationDate $expirationDate \n cardholdername $cardholderName \n CVC $cvc"
    }
}