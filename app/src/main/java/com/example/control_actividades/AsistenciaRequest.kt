package com.example.control_actividades

data class AsistenciaRequest(
    val qr: String,
    val estado: String,
    val id_clase: Int
)