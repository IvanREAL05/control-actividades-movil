package com.example.control_actividades
import com.google.gson.annotations.SerializedName

// Data classes que coinciden con tu endpoint
data class HistorialResponse(
    val total_actividades: Int,
    val total_ponderacion: Int,
    val historial: List<AlumnoHistorialCompleto>
)

data class AlumnoHistorialCompleto(
    @SerializedName("id_estudiante") val id_estudiante: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellido") val apellido: String,
    @SerializedName("matricula") val matricula: String?,
    @SerializedName("no_lista") val no_lista: Int,
    @SerializedName("correo") val correo: String?,
    @SerializedName("estado_actual") val estado_actual: String?,
    @SerializedName("grupo") val grupo: String?,
    @SerializedName("actividades") val actividades: List<ActividadDetalle>,
    @SerializedName("entregado") val entregado: Int,
    @SerializedName("ponderacion") val ponderacion: Int,

    // ⭐ NUEVOS CAMPOS - Calificaciones de parciales
    @SerializedName("parcial_1") val parcial_1: Int? = null,
    @SerializedName("parcial_2") val parcial_2: Int? = null,
    @SerializedName("ordinario") val ordinario: Double? = null,
    @SerializedName("promedio_parciales") val promedio_parciales: Double? = null,
    @SerializedName("estado_parcial_1") val estado_parcial_1: String = "pendiente",
    @SerializedName("estado_parcial_2") val estado_parcial_2: String = "pendiente",
    @SerializedName("fecha_parcial_1") val fecha_parcial_1: String? = null,
    @SerializedName("fecha_parcial_2") val fecha_parcial_2: String? = null
) {
    // ✅ VERSIÓN VIEJA - Sin cambios
    val actividadesEntregadas: Int
        get() = actividades.count { it.estado == "entregado" }

    val totalActividades: Int
        get() = actividades.size

    val puntosObtenidos: Int
        get() = actividades.sumOf { it.calificacion ?: 0 }

    val puntosTotales: Int
        get() = actividades.sumOf { it.valor_maximo }
}


data class ActividadEntregada(
    val id_actividad: Int,
    val titulo: String,
    val estado: String, // "entregado", "pendiente", etc.
    val valor_maximo: Int
)
