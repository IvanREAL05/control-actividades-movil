package com.example.control_actividades
import com.google.gson.annotations.SerializedName

data class CalificacionesEstudianteResponse(
    @SerializedName("id_estudiante") val id_estudiante: Int,
    @SerializedName("matricula") val matricula: String,
    @SerializedName("nombre_completo") val nombre_completo: String,
    @SerializedName("id_clase") val id_clase: Int,
    @SerializedName("nombre_clase") val nombre_clase: String,
    @SerializedName("nrc") val nrc: String,

    // Calificaciones
    @SerializedName("parcial_1") val parcial_1: Int?,
    @SerializedName("parcial_2") val parcial_2: Int?,
    @SerializedName("ordinario") val ordinario: Double?,
    @SerializedName("promedio_parciales") val promedio_parciales: Double?,

    // Estados
    @SerializedName("estado_parcial_1") val estado_parcial_1: String,
    @SerializedName("estado_parcial_2") val estado_parcial_2: String,
    @SerializedName("fecha_parcial_1") val fecha_parcial_1: String?,
    @SerializedName("fecha_parcial_2") val fecha_parcial_2: String?
)
