package com.example.control_actividades

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Actividad(
    val id_actividad: Int,
    val id_clase: Int,
    val titulo: String,
    val descripcion: String?,
    val fecha_entrega: String,
    val hora_entrega: String?,
    val fecha_creacion: String,
    val estado: String?,
    val fecha_entrega_real: String?,
    val vigencia: String?,
    val valor_maximo: Int = 10,
    val tipo_actividad: String
) : Parcelable