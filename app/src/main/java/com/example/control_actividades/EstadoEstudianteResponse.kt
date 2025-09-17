package com.example.control_actividades

data class EstadoEstudianteResponse(
    val success: Boolean,
    val message: String,
    val data: DataEstudiante? = null
)

data class DataEstudiante(
    val id_actividad_estudiante: Int?,
    val estado: String?,
    val fecha_entrega_real: String?,
    val fecha_registro: String?,
    val estudiante: EstudianteInfo?
)

data class EstudianteInfo(
    val id: Int,
    val nombre: String,
    val apellido: String
)
