package com.example.control_actividades

data class ActividadRequest(
    val titulo: String,
    val descripcion: String,
    val fecha_entrega: String,
    val hora_entrega: String,
    val id_clase: Int,
    val valor_maximo: Int,
    val tipo_actividad: String
)
