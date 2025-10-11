package com.example.control_actividades

data class ValidarEntregaResponse(
    val success: Boolean,
    val mensaje: String,
    val tarde: Boolean = false,
    val id_estudiante: Int? = null,
    val nombre: String? = null,
    val calificacion: Int? = null,
    val tipo_actividad: String? = null
)
