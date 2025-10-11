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
import kotlin.math.roundToInt
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class HistorialAlumnosActivity : AppCompatActivity() {

    // Views basadas en tu XML real
    private lateinit var toolbar: Toolbar
    private lateinit var tvContadorAlumnos: TextView
    private lateinit var tvPromedioGeneral: TextView
    private lateinit var tvActividadesTotales: TextView
    private lateinit var tvTasaEntrega: TextView
    private lateinit var cardNoAlumnos: MaterialCardView
    private lateinit var recyclerViewHistorial: RecyclerView
    private lateinit var fabExportar: FloatingActionButton

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
        setupFAB()
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
        fabExportar = findViewById(R.id.fabExportar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        recyclerViewHistorial.layoutManager = LinearLayoutManager(this)
        recyclerViewHistorial.setHasFixedSize(true)
    }

    private fun setupFAB() {
        fabExportar.setOnClickListener {
            if (historialResponse.historial.isNotEmpty()) {
                exportarHistorial()
            } else {
                Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarHistorial() {
        // Mostrar loading
        showLoading(true)

        lifecycleScope.launch {
            try {
                // 🔹 Llamada suspend (ya no usamos enqueue)
                val response = RetrofitClient.instance.getHistorialAlumnos(idClase)

                // Guardamos la respuesta
                historialResponse = response

                // Mostramos el historial en UI
                mostrarHistorial()

            } catch (e: Exception) {
                mostrarError("Error al cargar el historial: ${e.message}")
            } finally {
                // Ocultar loading al terminar
                showLoading(false)
            }
        }
    }

    private fun mostrarHistorial() {

        val alumnos = historialResponse.historial

        if (alumnos.isEmpty()) {
            // No hay alumnos
            cardNoAlumnos.visibility = View.VISIBLE
            recyclerViewHistorial.visibility = View.GONE
            fabExportar.visibility = View.GONE

            // Actualizar estadísticas con valores por defecto
            actualizarEstadisticasVacias()
        } else {
            // Hay alumnos
            cardNoAlumnos.visibility = View.GONE
            recyclerViewHistorial.visibility = View.VISIBLE
            fabExportar.visibility = View.VISIBLE

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
        tvContadorAlumnos.text = "${alumnos.size} alumno${if (alumnos.size != 1) "s" else ""}"

        // Total de actividades
        tvActividadesTotales.text = historialResponse.total_actividades.toString()

        val promedioGeneral = if (alumnos.isNotEmpty()) {
            val sumaPuntosObtenidos = alumnos.sumOf { it.puntosObtenidos.toDouble() } // asegurar Double
            val sumaPuntosTotales = alumnos.sumOf { it.puntosTotales.toDouble() }
            if (sumaPuntosTotales > 0) {
                // Escala 0-10
                (sumaPuntosObtenidos / sumaPuntosTotales * 10 * 10).roundToInt() / 10.0
            } else 0.0
        } else 0.0

        tvPromedioGeneral.text = promedioGeneral.toString()

        // Tasa de entrega (actividades entregadas)
        val totalEntregas = alumnos.sumOf { it.actividadesEntregadas } // Usa la propiedad del modelo
        val totalPosibles = alumnos.sumOf { it.totalActividades } // Usa la propiedad del modelo
        val tasaEntrega = if (totalPosibles > 0) {
            (totalEntregas.toFloat() / totalPosibles * 100).roundToInt()
        } else 0
        tvTasaEntrega.text = "$tasaEntrega%"
    }

    private fun actualizarEstadisticasVacias() {
        tvContadorAlumnos.text = "0 alumnos"
        tvPromedioGeneral.text = "0.0"
        tvActividadesTotales.text = historialResponse.total_actividades.toString()
        tvTasaEntrega.text = "0%"
    }

    private fun verDetallesAlumno(alumno: AlumnoHistorialCompleto) {
        Log.d("TRACE", "HistorialAlumnosActivity -> DetalleAlumnoActivity | idClase = $idClase, alumnoId = ${alumno.id_estudiante}")

        // Intent para ver detalles específicos del alumno
        val intent = Intent(this, DetalleAlumnoActivity::class.java).apply {
            putExtra("ALUMNO_ID", alumno.id_estudiante)
            putExtra("ALUMNO_NOMBRE", "${alumno.nombre} ${alumno.apellido}")
            putExtra("CLASE_ID", idClase)
            // Pasar más datos si es necesario
            putExtra("MATRICULA", alumno.matricula)
            putExtra("GRUPO", alumno.grupo)
        }
        startActivity(intent)
    }

    private fun exportarHistorial() {
        // Implementar exportación a Excel
        try {
            // Aquí puedes implementar tu lógica de exportación
            // Por ejemplo, usando Apache POI o similar

            val fileName = "historial_clase_${idClase}_${System.currentTimeMillis()}.xlsx"

            // Simular exportación exitosa
            Toast.makeText(this, "Funcionalidad de exportar en desarrollo", Toast.LENGTH_LONG).show()

            // TODO: Implementar exportación real
            /*
            val excelExporter = ExcelExporter()
            val success = excelExporter.exportarHistorialAlumnos(
                context = this,
                historial = historialResponse,
                fileName = fileName
            )

            if (success) {
                Toast.makeText(this, "Historial exportado: $fileName", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Error al exportar el historial", Toast.LENGTH_SHORT).show()
            }
            */
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            recyclerViewHistorial.visibility = View.GONE
            cardNoAlumnos.visibility = View.GONE
            fabExportar.visibility = View.GONE
            // Aquí podrías mostrar un ProgressBar si lo tienes en el XML
            // progressBar.visibility = View.VISIBLE
        }
        // El contenido se mostrará en mostrarHistorial()
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()

        // Mostrar estado de error
        cardNoAlumnos.visibility = View.VISIBLE
        recyclerViewHistorial.visibility = View.GONE
        fabExportar.visibility = View.GONE

        // Actualizar el mensaje de error en la card
        // Nota: Tendrías que tener un TextView específico para errores
        // o modificar el texto existente
        actualizarEstadisticasVacias()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}