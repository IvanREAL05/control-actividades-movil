package com.example.control_actividades

data class AlumnoResponse(
    val id_estudiante: Int,
    val matricula: String,
    val nombre: String,
    val apellido: String,
    val no_lista: Int,
    val estado: String
)