package com.example.control_actividades

data class ClaseActualResponse(
    val success: Boolean,
    val data: ClaseActualData,
)

data class ClaseActualData(
    val nombre_profesor: String,
    val clase_actual: ClaseInfo?,   // puede ser null si no hay clase
    val mensaje: String
)
