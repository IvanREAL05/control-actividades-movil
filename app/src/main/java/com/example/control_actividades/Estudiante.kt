package com.example.control_actividades

import com.google.gson.annotations.SerializedName

/**
 * Respuesta del endpoint /api/estadisticas/alumnos-clase/{id_clase}
 */
data class AlumnosClaseResponse(
    @SerializedName("id_clase")
    val idClase: Int,

    @SerializedName("total_alumnos")
    val totalAlumnos: Int,

    @SerializedName("alumnos")
    val alumnos: List<Estudiante>
)

/**
 * Data class para representar un Estudiante
 * Usada en el BottomSheet de selección de alumnos para asistencias
 */
data class Estudiante(
    @SerializedName("id_estudiante")
    val id: Int,

    @SerializedName("matricula")
    val matricula: String,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("apellido")
    val apellido: String,

    @SerializedName("nombre_completo")
    val nombreCompleto: String,

    @SerializedName("no_lista")
    val noLista: Int? = null
) {
    /**
     * Obtiene las iniciales del estudiante (máximo 2 letras)
     */
    fun getIniciales(): String {
        val nombreInicial = nombre.firstOrNull()?.uppercase() ?: ""
        val apellidoInicial = apellido.firstOrNull()?.uppercase() ?: ""
        return "$nombreInicial$apellidoInicial".take(2).ifEmpty { "?" }
    }
}