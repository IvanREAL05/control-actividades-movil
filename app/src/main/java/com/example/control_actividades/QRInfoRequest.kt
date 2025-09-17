package com.example.control_actividades

import com.google.gson.annotations.SerializedName

data class QRInfoRequest(
    @SerializedName("qrData") val qrData: String
)