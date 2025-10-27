package com.example.control_actividades

import com.google.gson.annotations.SerializedName

/**
 * Respuesta del endpoint GET /api/estadisticas/asistencias/alumno/{id_estudiante}/clase/{id_clase}
 */
data class AsistenciasRangoResponse(
    @SerializedName("id_estudiante")
    val idEstudiante: Int,

    @SerializedName("matricula")
    val matricula: String,

    @SerializedName("nombre_completo")
    val nombreCompleto: String,

    @SerializedName("id_clase")
    val idClase: Int,

    @SerializedName("nombre_clase")
    val nombreClase: String,

    @SerializedName("nrc")
    val nrc: String,

    @SerializedName("fecha_inicio")
    val fechaInicio: String,

    @SerializedName("fecha_fin")
    val fechaFin: String,

    @SerializedName("total_asistencias")
    val totalAsistencias: Int,

    @SerializedName("total_faltas")
    val totalFaltas: Int,

    @SerializedName("total_justificantes")
    val totalJustificantes: Int,

    @SerializedName("total_registros")
    val totalRegistros: Int,

    @SerializedName("tasa_asistencia")
    val tasaAsistencia: Double,

    @SerializedName("detalles")
    val detalles: List<DetalleAsistencia>
) {
    /**
     * Formatea el porcentaje de asistencia con 1 decimal
     */
    fun getPorcentajeFormateado(): String {
        return String.format("%.1f%%", tasaAsistencia)
    }

    /**
     * Determina el color del porcentaje según el valor
     * Verde: >= 80%
     * Naranja: 60% - 79%
     * Rojo: < 60%
     */
    fun getColorPorcentaje(): Int {
        return when {
            tasaAsistencia >= 80.0 -> android.graphics.Color.parseColor("#4CAF50") // Verde
            tasaAsistencia >= 60.0 -> android.graphics.Color.parseColor("#FF9800") // Naranja
            else -> android.graphics.Color.parseColor("#F44336") // Rojo
        }
    }

    /**
     * Obtiene un mensaje descriptivo del estado de asistencia
     */
    fun getMensajeEstado(): String {
        return when {
            tasaAsistencia >= 90.0 -> "¡Excelente asistencia!"
            tasaAsistencia >= 80.0 -> "Muy buena asistencia"
            tasaAsistencia >= 70.0 -> "Buena asistencia"
            tasaAsistencia >= 60.0 -> "Asistencia regular"
            tasaAsistencia > 0.0 -> "Asistencia baja"
            else -> "Sin asistencias en este periodo"
        }
    }

    /**
     * Formatea el rango de fechas en formato legible
     * De: 2025-10-20 A: 2025-10-24
     */
    fun getRangoFormateado(): String {
        return "Del $fechaInicio al $fechaFin"
    }
}

/**
 * Detalle de una asistencia específica
 */
data class DetalleAsistencia(
    @SerializedName("fecha")
    val fecha: String,

    @SerializedName("estado")
    val estado: String, // "presente", "ausente", "justificante"

    @SerializedName("hora_entrada")
    val horaEntrada: String?,

    @SerializedName("hora_salida")
    val horaSalida: String?
) {
    /**
     * Obtiene el emoji según el estado
     */
    fun getEmoji(): String {
        return when (estado.lowercase()) {
            "presente" -> "✅"
            "ausente" -> "❌"
            "justificante" -> "📋"
            else -> "❓"
        }
    }

    /**
     * Obtiene el color según el estado
     */
    fun getColor(): Int {
        return when (estado.lowercase()) {
            "presente" -> android.graphics.Color.parseColor("#4CAF50") // Verde
            "ausente" -> android.graphics.Color.parseColor("#F44336") // Rojo
            "justificante" -> android.graphics.Color.parseColor("#2196F3") // Azul
            else -> android.graphics.Color.parseColor("#9E9E9E") // Gris
        }
    }

    /**
     * Formatea la fecha en formato legible (DD/MM/YYYY)
     */
    fun getFechaFormateada(): String {
        return try {
            val parts = fecha.split("-")
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } catch (e: Exception) {
            fecha
        }
    }

    /**
     * Obtiene el horario formateado
     */
    fun getHorarioFormateado(): String {
        return when {
            horaEntrada != null && horaSalida != null -> "$horaEntrada - $horaSalida"
            horaEntrada != null -> "Entrada: $horaEntrada"
            horaSalida != null -> "Salida: $horaSalida"
            else -> "Sin horario registrado"
        }
    }
}