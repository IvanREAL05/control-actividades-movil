package com.example.control_actividades
import com.google.gson.annotations.SerializedName

/**
 * Data class principal para el resumen de actividades de una clase
 */
data class ResumenClase(
    @SerializedName("fecha_consulta")
    val fechaConsulta: String,

    @SerializedName("totales")
    val totales: Totales,

    @SerializedName("estado_entregas")
    val estadoEntregas: EstadoEntregas,

    @SerializedName("calificaciones")
    val calificaciones: Calificaciones,

    @SerializedName("mejores_peores")
    val mejoresPeores: MejoresPeores
)

/**
 * Información de totales de actividades
 */
data class Totales(
    @SerializedName("actividades")
    val actividades: Int,

    @SerializedName("valor_maximo_promedio")
    val valorMaximoPromedio: Int?
)

/**
 * Estado de entregas de actividades
 */
data class EstadoEntregas(
    @SerializedName("pendiente")
    val pendiente: Int,

    @SerializedName("entregado")
    val entregado: Int,

    @SerializedName("no_entregado")
    val noEntregado: Int
) {
    /**
     * Calcula el total de entregas
     */
    fun getTotalEntregas(): Int = pendiente + entregado + noEntregado

    /**
     * Calcula el porcentaje de entregas completadas
     */
    fun getPorcentajeEntregado(): Float {
        val total = getTotalEntregas()
        return if (total > 0) (entregado.toFloat() / total) * 100 else 0f
    }
}

/**
 * Información de calificaciones
 */
data class Calificaciones(
    @SerializedName("promedio_general")
    val promedioGeneral: Float,

    @SerializedName("distribucion")
    val distribucion: DistribucionCalificaciones
)

/**
 * Distribución de calificaciones por rangos
 */
data class DistribucionCalificaciones(
    @SerializedName("0-5")
    val rango0a5: Int,

    @SerializedName("6-7")
    val rango6a7: Int,

    @SerializedName("8-10")
    val rango8a10: Int
) {
    /**
     * Calcula el total de calificaciones
     */
    fun getTotalCalificaciones(): Int = rango0a5 + rango6a7 + rango8a10

    /**
     * Calcula el porcentaje de cada rango
     */
    fun getPorcentajes(): Triple<Float, Float, Float> {
        val total = getTotalCalificaciones()
        return if (total > 0) {
            Triple(
                (rango0a5.toFloat() / total) * 100,
                (rango6a7.toFloat() / total) * 100,
                (rango8a10.toFloat() / total) * 100
            )
        } else {
            Triple(0f, 0f, 0f)
        }
    }
}

/**
 * Información de mejores y peores actividades
 */
data class MejoresPeores(
    @SerializedName("mas_entregada")
    val masEntregada: String?,

    @SerializedName("menos_entregada")
    val menosEntregada: String?,

    @SerializedName("mayor_promedio")
    val mayorPromedio: String?,

    @SerializedName("menor_promedio")
    val menorPromedio: String?
)