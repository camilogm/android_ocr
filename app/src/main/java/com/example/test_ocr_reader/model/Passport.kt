package com.example.test_ocr_reader.model

import java.util.Date

data class Passport(
    val documentNumber: String,
    val dateOfBirthDay: Date,
    val expiryDate: Date,
    val expeditionCountry: String,
){
    override fun toString(): String {
        return "Passport: { Document Number: $documentNumber, Date of Birth: $dateOfBirthDay, Expiry Date: $expiryDate }, country ${expeditionCountry}"
    }
}