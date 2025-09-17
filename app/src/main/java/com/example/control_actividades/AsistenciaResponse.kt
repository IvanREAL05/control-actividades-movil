package com.example.control_actividades

data class AsistenciaResponse(
    val success: Boolean,
    val mensaje: String?,
    val duplicado: Boolean? = null,
    val actualizado: Boolean? = null,
    val nuevo: Boolean? = null
)
