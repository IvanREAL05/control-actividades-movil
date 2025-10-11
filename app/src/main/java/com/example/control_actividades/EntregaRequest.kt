package com.example.control_actividades

data class EntregaRequest(
    val qr: String,
    val id_actividad: Int,
    val calificacion: Int? = null // <- opcional, solo se envía si es tardía
)
