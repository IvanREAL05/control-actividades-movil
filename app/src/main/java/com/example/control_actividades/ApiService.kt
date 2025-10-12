package com.example.control_actividades

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.DELETE
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Streaming


interface ApiService {


    // NUEVA: Función con paginación
    @GET("api/avisos")
    fun obtenerAvisosPaginados(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Call<AvisoResponse>


    // NUEVO: Login
    @POST("api/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("api/profesor/clases/profesor/{id_profesor}/hoy")
    suspend fun getClasesHoy(
        @Path("id_profesor") idProfesor: Int
    ): Response<ClasesHoyResponse>

    @GET("api/profesor/clase-actual")
    suspend fun getClaseActual(
        @Query("id_profesor") idProfesor: Int
    ): Response<ClaseActualResponse>


    @GET("api/actividades/clase/{id_clase}")
    suspend fun getActividadesPorClase(
        @Path("id_clase") idClase: Int
    ): ActividadesResponse

    // Eliminar actividad por ID
    @DELETE("api/actividades/{id_actividad}")
    suspend fun eliminarActividad(
        @Path("id_actividad") idActividad: Int
    ): Response<ResponseBody>

    @POST("api/actividades/entrega")
    suspend fun registrarEntrega(
        @Body request: EntregaRequest
    ): EntregaResponse

    @POST("api/actividades/")
    suspend fun crearActividad(
        @Body actividad: ActividadRequest
    ): ActividadesResponse

    @PUT("api/actividades/{id}")
    suspend fun editarActividad(
        @Path("id") idActividad: Int,
        @Body actividad: ActividadRequest
    ): ActividadesResponse

    @GET("api/actividades/estudiantes/{id_actividad}")
    suspend fun getEstudiantes(
        @Path("id_actividad") idActividad: Int
    ): List<AlumnoActividad>

    @GET("api/reportes/excel/clase/{id_clase}")
    @Streaming
    suspend fun descargarReporteActividades(
        @Path("id_clase") idClase: Int
    ): Response<ResponseBody>


    @POST("api/actividades/actualizar-estado-estudiante")
    suspend fun actualizarEstadoEstudiante(
        @Body payload: ActualizacionEstado
    ): EstadoEstudianteResponse

    @POST("api/qr/info")
    suspend fun obtenerInfoQR(
        @Body request: QRInfoRequest
    ): QRInfoResponse

    @GET("api/actividades/resumen-clase/{id_clase}")
    suspend fun obtenerResumenClase(
        @Path("id_clase") idClase: Int
    ): Response<ResumenClaseResponse>

    @GET("api/asistencias/clase/{id_clase}")
    suspend fun getListaAlumnos(
        @Path("id_clase") idClase: Int
    ): Response<List<AlumnoResponse>>

    @PUT("api/asistencias/actualizar-estado")
    suspend fun actualizarEstado(
        @Body request: ActualizarEstadoRequest
    ): Response<ActualizarEstadoResponse>

    @POST("api/asistencias/")
    fun registrarAsistencia(
        @Body request: AsistenciaRequest
    ): Call<AsistenciaResponse>



    @Multipart
    @POST("api/justificantes")
    suspend fun enviarJustificante(
        @Part("fecha_expedicion") fechaExpedicion: RequestBody,
        @Part("matricula") matricula: RequestBody,
        @Part("nombre_estudiante") nombreEstudiante: RequestBody,
        @Part("fecha_inicio") fechaInicio: RequestBody,
        @Part("fecha_fin") fechaFin: RequestBody,
        @Part("gestor") gestor: RequestBody,
        @Part("numero_gestor") numeroGestor: RequestBody,
        @Part("situacion") situacion: RequestBody,
        @Part("folio_aprobacion") folioAprobacion: RequestBody,
        @Part("ejecutivo") ejecutivo: RequestBody,
        @Part documentoPdf: MultipartBody.Part,
        @Part documentoIne: MultipartBody.Part
    )


    @GET("api/actividades/{id_clase}/actividades-recientes")
    suspend fun obtenerActividadesRecientes(
        @Path("id_clase") idClase: Int
    ): Response<List<Actividad>>

    @GET("api/profesor/clases/profesor/{id_profesor}")
    suspend fun getTodasClasesProfesor(
        @Path("id_profesor") idProfesor: Int
    ): Response<ClasesPorDiaResponse>

    @GET("api/estadisticas/clases-actividades/{id_clase}/resumen")
    suspend fun getResumenClase(
        @Path("id_clase") idClase: Int
    ): ResumenClase

    @GET("api/actividades/historial/{id_clase}")
    suspend fun getHistorialAlumnos(
        @Path("id_clase") idClase: Int
    ): HistorialResponse

    @GET("api/actividades/alumno/{id_estudiante}/clase/{id_clase}")
    suspend fun getDetalleAlumno(
        @Path("id_estudiante") idEstudiante: Int,
        @Path("id_clase") idClase: Int
    ): DetalleAlumnoResponse

    // Crear observación
    @POST("api/observaciones")
    suspend fun crearObservacion(
        @Body body: CrearActualizarObservacionRequest
    ): ObservacionResponse

    // Actualizar observación
    @PUT("api/observaciones/{id}")
    suspend fun actualizarObservacion(
        @Path("id") id: Int,
        @Body body: CrearActualizarObservacionRequest
    ): ObservacionResponse

    // Obtener todas las observaciones
    @GET("api/observaciones")
    suspend fun obtenerObservaciones(): ObservacionResponse

    // Obtener por estudiante
    @GET("api/observaciones/estudiante/{estudiante_id}")
    suspend fun obtenerObservacionesPorEstudiante(
        @Path("estudiante_id") estudianteId: Int
    ): ObservacionResponse

    // Eliminar observación
    @DELETE("api/observaciones/{id}")
    suspend fun eliminarObservacion(
        @Path("id") id: Int
    ): ObservacionResponse

    @POST("api/actividades/validar-entrega")
    suspend fun validarEntrega(
        @Body request: EntregaRequest
    ): ValidarEntregaResponse

    @GET("api/reportes/excel/clase/completo/{idClase}")
    @Streaming
    suspend fun descargarReporteClaseCompleto(
        @Path("idClase") idClase: Int
    ): Response<ResponseBody>

    @GET("api/actividades/actividades/{id}")
    suspend fun getActividadPorId(@Path("id") actividadId: Int): Actividad


    @GET("api/observaciones/grupo/{idGrupo}")
    suspend fun obtenerObservacionesPorGrupo(
        @Path("idGrupo") idGrupo: Int
    ): Response<ObservacionResponse>


}