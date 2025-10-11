package com.example.control_actividades

import com.google.gson.annotations.SerializedName

data class AsistenciaResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("mensaje")
    val mensaje: String? = null,

    @SerializedName("nuevo")
    val nuevo: Boolean? = null,

    @SerializedName("duplicado")
    val duplicado: Boolean? = null,

    @SerializedName("actualizado")
    val actualizado: Boolean? = null
)