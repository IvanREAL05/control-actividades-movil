package com.example.control_actividades

data class Observacion(
    val id: Int,
    val estudiante_id: Int,
    val profesor_id: Int,
    val estado: Int?,
    val fecha: String,
    val nombre_estudiante: String? = null,
    val apellido_estudiante: String? = null,
    val nombre_profesor: String? = null,
    val apellido_profesor: String? = null
)

data class ObservacionResponse(
    val success: Boolean,
    val mensaje: String? = null,
    val observacion: Observacion? = null,
    val observaciones: List<Observacion>? = null,
    val total_observaciones: Int
)

data class CrearActualizarObservacionRequest(
    val estudiante_id: Int,
    val profesor_id: Int,
    val estado: Int? = null  // Puede ser null si es "sin observación"
)
