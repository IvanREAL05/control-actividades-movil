package com.example.control_actividades

data class AsistenciaRequest(
    val qr: String,
    val estado: String = "presente", // valor por defecto
    val id_clase: Int
)
