package com.example.control_actividades

data class ClaseProfesorItem(
    val id_clase: Int,
    val materia: String,
    val grupo: String,
    val nrc: String? = null,
    val aula: String? = null
)

data class ClasesProfesorResponse(
    val clases: List<ClaseProfesorItem>
)


