package com.example.control_actividades
import com.google.gson.annotations.SerializedName

data class ActualizacionEstado(
    @SerializedName("estudiante_id") val estudianteId: Int,
    @SerializedName("actividad_id") val actividadId: Int,
    @SerializedName("nuevo_estado") val nuevoEstado: String
)