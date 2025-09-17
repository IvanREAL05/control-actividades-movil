package com.example.control_actividades

data class ActividadReciente(
    val id_actividad: Int,
    val titulo: String,
    val descripcion: String?,
    val fecha_creacion: String,
    val fecha_entrega: String,
    val valor_maximo: Double
)

