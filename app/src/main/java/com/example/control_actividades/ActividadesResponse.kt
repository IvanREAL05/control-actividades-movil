package com.example.control_actividades

data class ActividadesResponse(
    val actividades: List<Actividad>?,
    val mensaje: String,
    val id_actividad: Int
)
