package com.example.control_actividades

data class ActualizarEstadoRequest(
    val id_estudiante: Int,
    val id_clase: Int,
    val estado: String
)