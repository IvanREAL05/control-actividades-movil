package com.example.control_actividades
import com.google.gson.annotations.SerializedName


data class DetalleAlumnoResponse(
    val alumno: AlumnoInfo,
    val clase: ClaseDetalle,
    val actividades: List<ActividadDetalle>
)

data class AlumnoInfo(
    @SerializedName("id_estudiante") val id_estudiante: Int,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("apellido") val apellido: String?,
    val matricula: String?,
    val correo: String?,
    val grupo: String?,
    val turno: String?
)

data class ClaseDetalle(
    @SerializedName("id_clase") val id_clase: Int,
    @SerializedName("nombre_clase") val nombre_clase: String?,
    val materia: String?,
    val profesor: String?,
    @SerializedName("hora_inicio") val hora_inicio: String?,
    @SerializedName("hora_fin") val hora_fin: String?,
    @SerializedName("dias_semana") val dias_semana: String?
)

data class ActividadDetalle(
    @SerializedName("id_actividad") val id_actividad: Int,
    val titulo: String,
    val descripcion: String?,
    @SerializedName("fecha_entrega") val fecha_entrega: String?,
    @SerializedName("valor_maximo") val valor_maximo: Int,
    val estado: String?,
    @SerializedName("fecha_entrega_real") val fecha_entrega_real: String?,
    val calificacion: Double? // <-- CORREGIDO
)
