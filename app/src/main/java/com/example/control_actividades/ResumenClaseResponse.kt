package com.example.control_actividades

data class ResumenClaseResponse(
    val hoy: Hoy,
    val historial: Historial,
    val top: Top
)

data class Hoy(
    val presentes: Int,
    val ausentes: Int,
    val justificantes: Int
)

data class Historial(
    val presentes: Int,
    val ausentes: Int,
    val justificantes: Int,
    val porcentaje_asistencia: Int,
    val total_registros: Int,
    val total_estudiantes: Int
)

data class Top(
    val mas_asisten: List<RankingEstudiante>,
    val mas_faltan: List<RankingEstudiante>,
    val mas_justifican: List<RankingEstudiante>
)

data class RankingEstudiante(
    val id_estudiante: Int,
    val nombre: String,
    val apellido: String,
    val cantidad: Int
)