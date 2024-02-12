package com.example.test_ocr_reader.model

import java.text.SimpleDateFormat
import java.util.Date

data class Passport(
    val documentNumber: String,
    val dateOfBirthDay: Date,
    val expiryDate: Date,
    val expeditionCountry: String,
    val givenName: String = "",
    val surName: String = ""
){

    override fun toString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        return "TravelDocument(" +
                "documentNumber='$documentNumber', " +
                "dateOfBirthDay=${dateFormat.format(dateOfBirthDay)}, " +
                "expiryDate=${dateFormat.format(expiryDate)}, " +
                "expeditionCountry='$expeditionCountry', " +
                "givenName='$givenName', " +
                "surName='$surName')"
    }
}