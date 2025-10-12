package com.example.control_actividades

import com.google.gson.annotations.SerializedName

data class AlumnoActividad(
    @SerializedName("id_estudiante")
    val idEstudiante: Int,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("apellido")
    val apellido: String,

    @SerializedName("correo")
    val correo: String?,

    @SerializedName("matricula")
    val matricula: String?,

    @SerializedName("codigo_qr")
    val codigoQr: String?,

    @SerializedName("no_lista")
    val noLista: Int?,

    @SerializedName("id_grupo")
    val idGrupo: Int?,

    @SerializedName("estado")
    var estado: String = "pendiente", // Default value

    @SerializedName("fecha_entrega_real")
    val fechaEntregaReal: String?,

    @SerializedName("fecha_registro")
    val fechaRegistro: String?,

    @SerializedName("calificacion")
    var  calificacion: Int?,

    @SerializedName("id_actividad_estudiante")
    val idActividadEstudiante: Int?
)