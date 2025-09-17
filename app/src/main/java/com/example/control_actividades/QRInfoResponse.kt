package com.example.control_actividades

data class QRInfoResponse(
    val success: Boolean,
    val data: QRData? = null
)

data class QRData(
    val qr_valido: Boolean,
    val datos_qr: DatosQR,
    val datos_bd: DatosBD,
    val validacion: Validacion
)

data class DatosQR(
    val nombre: String,
    val matricula: String,
    val grupo: String,
    val clave: String
)

data class DatosBD(
    val nombre: String,
    val matricula: String,
    val grupo: String,
    val turno: String,
    val nivel: String,
    val estado: String,
    val correo: String,
    val no_lista: Int
)

data class Validacion(
    val grupo_coincide: Boolean,
    val estudiante_activo: Boolean
)