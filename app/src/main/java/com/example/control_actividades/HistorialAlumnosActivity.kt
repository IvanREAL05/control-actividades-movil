package com.example.control_actividades

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class HistorialAlumnosActivity : AppCompatActivity() {

    // Views basadas en el nuevo XML
    private lateinit var toolbar: Toolbar
    private lateinit var tvContadorAlumnos: TextView
    private lateinit var tvPromedioGeneral: TextView
    private lateinit var tvActividadesTotales: TextView
    private lateinit var tvTasaEntrega: TextView
    private lateinit var cardNoAlumnos: MaterialCardView
    private lateinit var recyclerViewHistorial: RecyclerView

    // Datos
    private var idClase: Int = -1
    private var historialResponse: HistorialResponse = HistorialResponse(
        total_actividades = 0,
        total_ponderacion = 0,
        historial = emptyList()
    )
    private lateinit var adapter: HistorialAlumnosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_alumnos)

        // Obtener ID de la clase
        idClase = intent.getIntExtra("id_clase", -1)
        Log.d("TRACE", "HistorialAlumnosActivity | idClase recibido = $idClase")
        if (idClase == -1) {
            Toast.makeText(this, "Error: ID de clase no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupToolbar()
        setupRecyclerView()
        cargarHistorial()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvContadorAlumnos = findViewById(R.id.tvContadorAlumnos)
        tvPromedioGeneral = findViewById(R.id.tvPromedioGeneral)
        tvActividadesTotales = findViewById(R.id.tvActividadesTotales)
        tvTasaEntrega = findViewById(R.id.tvTasaEntrega)
        cardNoAlumnos = findViewById(R.id.cardNoAlumnos)
        recyclerViewHistorial = findViewById(R.id.recyclerViewHistorial)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "" // Título vacío porque ya está en el header

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        recyclerViewHistorial.layoutManager = LinearLayoutManager(this)
        recyclerViewHistorial.setHasFixedSize(false)
    }

    private fun cargarHistorial() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // ⭐ PASO 1: Cargar actividades (endpoint viejo)
                val responseActividades = RetrofitClient.instance.getHistorialAlumnos(idClase)
                historialResponse = responseActividades

                // ⭐ PASO 2: Cargar calificaciones de parciales (endpoint nuevo)
                val calificacionesParciales = try {
                    RetrofitClient.instance.obtenerCalificacionesClase(idClase)
                } catch (e: Exception) {
                    Log.w("HistorialAlumnos", "No se pudieron cargar calificaciones: ${e.message}")
                    emptyList() // Si falla, continuar sin calificaciones
                }

                // ⭐ PASO 3: Combinar datos
                val alumnosConCalificaciones = combinarDatos(
                    responseActividades.historial,
                    calificacionesParciales
                )

                // ⭐ PASO 4: Actualizar la respuesta con datos combinados
                historialResponse = HistorialResponse(
                    total_actividades = responseActividades.total_actividades,
                    total_ponderacion = responseActividades.total_ponderacion,
                    historial = alumnosConCalificaciones
                )

                // Mostrar en UI
                mostrarHistorial()

            } catch (e: Exception) {
                Log.e("HistorialAlumnos", "Error al cargar historial", e)
                mostrarError("Error al cargar el historial: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    // ⭐ NUEVA FUNCIÓN: Combinar actividades con calificaciones
    private fun combinarDatos(
        alumnos: List<AlumnoHistorialCompleto>,
        calificaciones: List<CalificacionesEstudianteResponse>
    ): List<AlumnoHistorialCompleto> {

        // Crear mapa de calificaciones por id_estudiante para búsqueda rápida
        val calificacionesMap = calificaciones.associateBy { it.id_estudiante }

        // Combinar datos
        return alumnos.map { alumno ->
            val calif = calificacionesMap[alumno.id_estudiante]

            // Crear nuevo objeto con calificaciones agregadas
            alumno.copy(
                parcial_1 = calif?.parcial_1,
                parcial_2 = calif?.parcial_2,
                ordinario = calif?.ordinario,
                promedio_parciales = calif?.promedio_parciales,
                estado_parcial_1 = calif?.estado_parcial_1 ?: "pendiente",
                estado_parcial_2 = calif?.estado_parcial_2 ?: "pendiente",
                fecha_parcial_1 = calif?.fecha_parcial_1,
                fecha_parcial_2 = calif?.fecha_parcial_2
            )
        }
    }

    private fun mostrarHistorial() {
        val alumnos = historialResponse.historial

        if (alumnos.isEmpty()) {
            // No hay alumnos
            cardNoAlumnos.visibility = View.VISIBLE
            recyclerViewHistorial.visibility = View.GONE
            actualizarEstadisticasVacias()
        } else {
            // Hay alumnos
            cardNoAlumnos.visibility = View.GONE
            recyclerViewHistorial.visibility = View.VISIBLE

            // Configurar adapter
            adapter = HistorialAlumnosAdapter(
                alumnos = alumnos,
                totalActividades = historialResponse.total_actividades,
                totalPonderacion = historialResponse.total_ponderacion,
                onVerDetallesClick = { alumno ->
                    verDetallesAlumno(alumno)
                }
            )
            recyclerViewHistorial.adapter = adapter

            // Actualizar estadísticas
            actualizarEstadisticas(alumnos)
        }
    }

    private fun actualizarEstadisticas(alumnos: List<AlumnoHistorialCompleto>) {
        // Contador de alumnos
        tvContadorAlumnos.text = alumnos.size.toString()

        // Total de actividades
        tvActividadesTotales.text = historialResponse.total_actividades.toString()

        // Promedio general (escala 0-100)
        val promedioGeneral = if (alumnos.isNotEmpty()) {
            val sumaPuntosObtenidos = alumnos.sumOf { it.puntosObtenidos }
            val sumaPuntosTotales = alumnos.sumOf { it.puntosTotales }
            if (sumaPuntosTotales > 0) {
                (sumaPuntosObtenidos * 100) / sumaPuntosTotales
            } else 0
        } else 0

        tvPromedioGeneral.text = promedioGeneral.toString()

        // Tasa de entrega
        val totalEntregas = alumnos.sumOf { it.actividadesEntregadas }
        val totalPosibles = alumnos.sumOf { it.totalActividades }
        val tasaEntrega = if (totalPosibles > 0) {
            (totalEntregas * 100) / totalPosibles
        } else 0

        tvTasaEntrega.text = "$tasaEntrega%"
    }

    private fun actualizarEstadisticasVacias() {
        tvContadorAlumnos.text = "0"
        tvPromedioGeneral.text = "0"
        tvActividadesTotales.text = historialResponse.total_actividades.toString()
        tvTasaEntrega.text = "0%"
    }

    private fun verDetallesAlumno(alumno: AlumnoHistorialCompleto) {
        Log.d("TRACE", "HistorialAlumnosActivity -> DetalleAlumnoActivity | idClase = $idClase, alumnoId = ${alumno.id_estudiante}")

        val intent = Intent(this, DetalleAlumnoActivity::class.java).apply {
            putExtra("ALUMNO_ID", alumno.id_estudiante)
            putExtra("ALUMNO_NOMBRE", "${alumno.nombre} ${alumno.apellido}")
            putExtra("CLASE_ID", idClase)
            putExtra("MATRICULA", alumno.matricula)
            putExtra("GRUPO", alumno.grupo)
        }
        startActivity(intent)
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            recyclerViewHistorial.visibility = View.GONE
            cardNoAlumnos.visibility = View.GONE
            Toast.makeText(this, "Cargando historial...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
        cardNoAlumnos.visibility = View.VISIBLE
        recyclerViewHistorial.visibility = View.GONE
        actualizarEstadisticasVacias()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}

// ⭐ AGREGAR: Función de extensión para copy() con valores por defecto
private fun AlumnoHistorialCompleto.copy(
    parcial_1: Int? = this.parcial_1,
    parcial_2: Int? = this.parcial_2,
    ordinario: Double? = this.ordinario,
    promedio_parciales: Double? = this.promedio_parciales,
    estado_parcial_1: String = this.estado_parcial_1,
    estado_parcial_2: String = this.estado_parcial_2,
    fecha_parcial_1: String? = this.fecha_parcial_1,
    fecha_parcial_2: String? = this.fecha_parcial_2
) = AlumnoHistorialCompleto(
    id_estudiante = this.id_estudiante,
    nombre = this.nombre,
    apellido = this.apellido,
    matricula = this.matricula,
    no_lista = this.no_lista,
    correo = this.correo,
    estado_actual = this.estado_actual,
    grupo = this.grupo,
    actividades = this.actividades,
    entregado = this.entregado,
    ponderacion = this.ponderacion,
    parcial_1 = parcial_1,
    parcial_2 = parcial_2,
    ordinario = ordinario,
    promedio_parciales = promedio_parciales,
    estado_parcial_1 = estado_parcial_1,
    estado_parcial_2 = estado_parcial_2,
    fecha_parcial_1 = fecha_parcial_1,
    fecha_parcial_2 = fecha_parcial_2
)