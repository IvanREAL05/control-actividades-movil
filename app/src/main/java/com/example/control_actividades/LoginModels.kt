package com.example.control_actividades

import com.google.gson.annotations.SerializedName

/**
 * Request para confirmar sesión del dashboard
 */
data class ConfirmarSesionRequest(
    @SerializedName("session_id")
    val sessionId: String,  // ✅ camelCase en Kotlin

    @SerializedName("id_profesor")
    val idProfesor: Int,    // ✅ camelCase en Kotlin

    @SerializedName("id_clase")
    val idClase: Int        // ✅ camelCase en Kotlin
)

/**
 * Respuesta al confirmar sesión
 */
data class ConfirmarSesionResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("mensaje")
    val mensaje: String,

    @SerializedName("clase")
    val clase: ClaseDashboard? = null
)

/**
 * Información de la clase en el dashboard
 */
data class ClaseDashboard(
    @SerializedName("id_clase")
    val idClase: Int,

    @SerializedName("materia")
    val materia: String,

    @SerializedName("grupo")
    val grupo: String
)