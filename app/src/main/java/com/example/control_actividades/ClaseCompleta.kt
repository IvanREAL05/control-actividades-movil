package com.example.control_actividades

data class ClaseCompleta(
    val id_clase: Int,
    val nombre_clase: String,
    val nrc: String,
    val aula: String,
    val materia: String,
    val materia_clave: String,
    val grupo: String,
    val turno: String,
    val nivel: String,
    val hora_inicio: String,
    val hora_fin: String,
    val dia: String,
    val nombre_profesor: String
)

data class ClasesPorDiaResponse(
    val success: Boolean,
    val data: ClasesPorDiaData
)

data class ClasesPorDiaData(
    val profesor: String,
    val clases_por_dia: Map<String, List<ClaseCompleta>>,
    val total_clases: Int
)
