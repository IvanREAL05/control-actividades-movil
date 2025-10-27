package com.example.control_actividades

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class EstadisticasGrupoActivity : AppCompatActivity() {

    // Views del layout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvFechaConsulta: TextView
    private lateinit var tvTotalActividades: TextView
    private lateinit var tvValorMaximo: TextView
    private lateinit var tvEntregado: TextView
    private lateinit var tvPendiente: TextView
    private lateinit var tvNoEntregado: TextView
    private lateinit var tvPromedioGeneral: TextView
    private lateinit var tvRango0_5: TextView
    private lateinit var tvRango6_7: TextView
    private lateinit var tvRango8_10: TextView
    private lateinit var tvMasEntregada: TextView
    private lateinit var tvMenosEntregada: TextView
    private lateinit var tvMayorPromedio: TextView
    private lateinit var tvMenorPromedio: TextView
    private lateinit var btnDescargarLista: Button
    private lateinit var btnVerAsistenciasPorAlumno: Button

    // Variables para manejar datos
    private var idClase: Int = -1
    private var nombreClase: String = ""
    private var idProfesor: Int = 0
    private lateinit var apiService: ApiService
    private var listaEstudiantes: List<Estudiante> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estadisticas_grupo)

        // Obtener el ID de la clase del intent
        idClase = intent.getIntExtra("id_clase", -1)
        idProfesor = intent.getIntExtra("id_profesor", 0)
        nombreClase = intent.getStringExtra("nombre_clase") ?: "Clase"

        if (idClase == -1) {
            Toast.makeText(this, "Error: ID de clase no válido", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Inicializar API service
        apiService = RetrofitClient.instance

        // Configurar views
        setupViews()
        setupToolbar()
        setupListeners()

        // Cargar datos
        cargarEstadisticas()
        cargarEstudiantes()
    }

    private fun setupViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar)

        // Header
        tvFechaConsulta = findViewById(R.id.tvFechaConsulta)

        // Totales
        tvTotalActividades = findViewById(R.id.tvTotalActividades)
        tvValorMaximo = findViewById(R.id.tvValorMaximo)

        // Estado entregas
        tvEntregado = findViewById(R.id.tvEntregado)
        tvPendiente = findViewById(R.id.tvPendiente)
        tvNoEntregado = findViewById(R.id.tvNoEntregado)

        // Calificaciones
        tvPromedioGeneral = findViewById(R.id.tvPromedioGeneral)
        tvRango0_5 = findViewById(R.id.tvRango0_5)
        tvRango6_7 = findViewById(R.id.tvRango6_7)
        tvRango8_10 = findViewById(R.id.tvRango8_10)

        // Rankings
        tvMasEntregada = findViewById(R.id.tvMasEntregada)
        tvMenosEntregada = findViewById(R.id.tvMenosEntregada)
        tvMayorPromedio = findViewById(R.id.tvMayorPromedio)
        tvMenorPromedio = findViewById(R.id.tvMenorPromedio)

        // Botones
        btnDescargarLista = findViewById(R.id.btnDescargarLista)
        btnVerAsistenciasPorAlumno = findViewById(R.id.btnVerAsistenciasPorAlumno)
    }

    private fun setupListeners() {
        // Botón descargar lista
        btnDescargarLista.setOnClickListener {
            Log.d(TAG, "=== BOTÓN DESCARGA PRESIONADO ===")
            Log.d(TAG, "idProfesor a enviar: $idProfesor")

            if (idProfesor != 0) {
                Log.d(TAG, "✅ ID profesor válido, iniciando descarga...")
                descargarLista(idProfesor)
            } else {
                Log.e(TAG, "❌ ERROR: idProfesor es 0 o inválido")
                Toast.makeText(this, "Error: No se encontró ID de profesor válido", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón ver asistencias por alumno
        btnVerAsistenciasPorAlumno.setOnClickListener {
            mostrarBottomSheetAlumnos()
        }
    }

    /**
     * Carga la lista de estudiantes de la clase
     */
    private fun cargarEstudiantes() {
        lifecycleScope.launch {
            try {
                val response = apiService.getEstudiantesPorClase(idClase)
                if (response.isSuccessful && response.body() != null) {
                    val alumnosResponse = response.body()!!
                    listaEstudiantes = alumnosResponse.alumnos // ← Aquí extraes la lista
                    Log.d(TAG, "✅ Estudiantes cargados: ${listaEstudiantes.size}")
                } else {
                    Log.e(TAG, "❌ Error al cargar estudiantes: ${response.code()}")
                    Toast.makeText(
                        this@EstadisticasGrupoActivity,
                        "Error al cargar lista de alumnos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción al cargar estudiantes", e)
                Toast.makeText(
                    this@EstadisticasGrupoActivity,
                    "Error de conexión al cargar alumnos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Muestra el BottomSheet con la lista de alumnos
     */
    private fun mostrarBottomSheetAlumnos() {
        if (listaEstudiantes.isEmpty()) {
            Toast.makeText(this, "No hay alumnos disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        val bottomSheet = AlumnosBottomSheet.newInstance(
            nombreClase = nombreClase,
            listaAlumnos = listaEstudiantes,
            onAlumnoSeleccionado = { estudiante ->
                // Cuando se selecciona un alumno, mostrar el DateRangePicker
                Log.d(TAG, "📌 Alumno seleccionado: ${estudiante.nombre} (ID: ${estudiante.id})")
                mostrarDateRangePicker(estudiante)
            }
        )

        bottomSheet.show(supportFragmentManager, AlumnosBottomSheet.TAG)
    }

    /**
     * Muestra el DateRangePicker para seleccionar rango de fechas
     */
    private fun mostrarDateRangePicker(estudiante: Estudiante) {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Selecciona el período")
            .setSelection(
                androidx.core.util.Pair(
                    MaterialDatePicker.thisMonthInUtcMilliseconds(),
                    MaterialDatePicker.todayInUtcMilliseconds()
                )
            )
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val fechaInicio = selection.first
            val fechaFin = selection.second

            // Convertir timestamps a formato yyyy-MM-dd
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val fechaInicioStr = sdf.format(Date(fechaInicio))
            val fechaFinStr = sdf.format(Date(fechaFin))

            Log.d(TAG, "📅 Rango seleccionado: $fechaInicioStr a $fechaFinStr")

            // Consultar asistencias
            consultarAsistencias(estudiante, fechaInicioStr, fechaFinStr)
        }

        dateRangePicker.addOnNegativeButtonClickListener {
            Log.d(TAG, "❌ DateRangePicker cancelado")
        }

        dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }

    /**
     * Consulta las asistencias del alumno en el rango de fechas
     */
    private fun consultarAsistencias(
        estudiante: Estudiante,
        fechaInicio: String,
        fechaFin: String
    ) {
        lifecycleScope.launch {
            try {
                // Mostrar loading
                Toast.makeText(
                    this@EstadisticasGrupoActivity,
                    "Consultando asistencias...",
                    Toast.LENGTH_SHORT
                ).show()

                // 🔥 NUEVO: Llamar al endpoint actualizado (sin Response wrapper)
                val resultado = apiService.obtenerAsistenciasRango(
                    idEstudiante = estudiante.id,
                    idClase = idClase,
                    fechaInicio = fechaInicio,
                    fechaFin = fechaFin
                )

                // 🔥 NUEVO: Ya no necesitamos verificar isSuccessful, solo usamos el objeto
                Log.d(TAG, "✅ Asistencias obtenidas exitosamente")
                Log.d(TAG, "  - Total asistencias: ${resultado.totalAsistencias}")
                Log.d(TAG, "  - Total faltas: ${resultado.totalFaltas}")
                Log.d(TAG, "  - Total justificantes: ${resultado.totalJustificantes}")
                Log.d(TAG, "  - Tasa asistencia: ${resultado.tasaAsistencia}%")

                mostrarDialogResultado(resultado)

            } catch (e: HttpException) {
                // Error HTTP
                val mensaje = when (e.code()) {
                    404 -> "Estudiante o clase no encontrados"
                    400 -> "Rango de fechas inválido"
                    500 -> "Error del servidor"
                    else -> "Error al consultar asistencias (${e.code()})"
                }
                Toast.makeText(this@EstadisticasGrupoActivity, mensaje, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "❌ Error HTTP: ${e.code()} - ${e.message()}")

            } catch (e: IOException) {
                // Error de conexión
                Toast.makeText(
                    this@EstadisticasGrupoActivity,
                    "Error de conexión. Verifica tu internet.",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "❌ Error de conexión", e)

            } catch (e: Exception) {
                // Otros errores
                Toast.makeText(
                    this@EstadisticasGrupoActivity,
                    "Error inesperado: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "❌ Excepción al consultar asistencias", e)
            }
        }
    }



    /**
     * Muestra el dialog con los resultados de asistencias
     */
    private fun mostrarDialogResultado(data: AsistenciasRangoResponse) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_resultado_asistencia)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Configurar views del dialog
        val tvNombreAlumnoDialog = dialog.findViewById<TextView>(R.id.tvNombreAlumnoDialog)
        val tvNombreClaseDialog = dialog.findViewById<TextView>(R.id.tvNombreClaseDialog)
        val tvFechaInicio = dialog.findViewById<TextView>(R.id.tvFechaInicio)
        val tvFechaFin = dialog.findViewById<TextView>(R.id.tvFechaFin)
        val tvAsistencias = dialog.findViewById<TextView>(R.id.tvAsistencias)
        val tvFaltas = dialog.findViewById<TextView>(R.id.tvFaltas)
        val tvJustificantes = dialog.findViewById<TextView>(R.id.tvJustificantes)
        val tvPorcentajeAsistencia = dialog.findViewById<TextView>(R.id.tvPorcentajeAsistencia)
        val btnCerrarDialog = dialog.findViewById<Button>(R.id.btnCerrarDialog)
        val btnNuevaConsulta = dialog.findViewById<Button>(R.id.btnNuevaConsulta)

        // 🔥 NUEVO: Llenar datos con el nuevo modelo
        tvNombreAlumnoDialog.text = data.nombreCompleto
        tvNombreClaseDialog.text = data.nombreClase
        tvFechaInicio.text = formatearFecha(data.fechaInicio)
        tvFechaFin.text = formatearFecha(data.fechaFin)
        tvAsistencias.text = data.totalAsistencias.toString()
        tvFaltas.text = data.totalFaltas.toString()
        tvJustificantes.text = data.totalJustificantes.toString()

        tvPorcentajeAsistencia.text = data.getPorcentajeFormateado()



        // 🔥 NUEVO: Cambiar color del porcentaje según el valor
        tvPorcentajeAsistencia.setTextColor(data.getColorPorcentaje())

        // Configurar botones
        btnCerrarDialog.setOnClickListener {
            dialog.dismiss()
        }

        btnNuevaConsulta.setOnClickListener {
            dialog.dismiss()
            mostrarBottomSheetAlumnos()
        }

        dialog.show()
    }

    /**
     * Formatea una fecha de yyyy-MM-dd a dd/MM/yyyy
     */
    private fun formatearFecha(fecha: String): String {
        return try {
            val parts = fecha.split("-")
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } catch (e: Exception) {
            fecha
        }
    }

    private fun descargarLista(idProfesor: Int) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val response = apiService.descargarReporteProfesor(idProfesor)
                    if (response.isSuccessful && response.body() != null) {
                        val inputStream = response.body()!!.byteStream()
                        val fileName = "Reporte_Profesor_$idProfesor.xlsx"

                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        if (!downloadsDir.exists()) downloadsDir.mkdirs()

                        val file = File(downloadsDir, fileName)
                        FileOutputStream(file).use { output ->
                            inputStream.copyTo(output)
                        }

                        // Notificar al Media Scanner
                        val uri = android.net.Uri.fromFile(file)
                        val scanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        scanIntent.data = uri
                        sendBroadcast(scanIntent)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@EstadisticasGrupoActivity,
                                "Reporte guardado en Descargas: $fileName",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@EstadisticasGrupoActivity,
                                "Error al descargar el reporte",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e(TAG, "Error: ${response.code()} - ${response.message()}")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@EstadisticasGrupoActivity,
                            "Ocurrió un error al descargar el archivo",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(TAG, "Error descargando Excel", e)
                    }
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun cargarEstadisticas() {
        lifecycleScope.launch {
            try {
                mostrarEstadoCarga()
                val resumen = apiService.getResumenClase(idClase)
                mostrarDatos(resumen)
            } catch (e: HttpException) {
                when (e.code()) {
                    404 -> mostrarError("Clase no encontrada")
                    500 -> mostrarError("Error interno del servidor")
                    else -> mostrarError("Error HTTP: ${e.code()}")
                }
            } catch (e: IOException) {
                mostrarError("Error de conexión. Verifica tu internet.")
            } catch (e: Exception) {
                mostrarError("Error inesperado: ${e.localizedMessage}")
            }
        }
    }

    private fun mostrarEstadoCarga() {
        tvFechaConsulta.text = "Cargando datos..."
        tvTotalActividades.text = "..."
        tvValorMaximo.text = "..."
        tvEntregado.text = "..."
        tvPendiente.text = "..."
        tvNoEntregado.text = "..."
        tvPromedioGeneral.text = "..."
        tvRango0_5.text = "..."
        tvRango6_7.text = "..."
        tvRango8_10.text = "..."
        tvMasEntregada.text = "Cargando..."
        tvMenosEntregada.text = "Cargando..."
        tvMayorPromedio.text = "Cargando..."
        tvMenorPromedio.text = "Cargando..."
    }

    private fun mostrarDatos(resumen: ResumenClase) {
        tvFechaConsulta.text = "Consultado el: ${resumen.fechaConsulta}"
        tvTotalActividades.text = resumen.totales.actividades.toString()
        tvValorMaximo.text = resumen.totales.valorMaximoPromedio?.toString() ?: "N/A"

        with(resumen.estadoEntregas) {
            tvEntregado.text = entregado.toString()
            tvPendiente.text = pendiente.toString()
            tvNoEntregado.text = noEntregado.toString()
        }

        with(resumen.calificaciones) {
            tvPromedioGeneral.text = String.format("%.1f", promedioGeneral)
            with(distribucion) {
                tvRango0_5.text = rango0a5.toString()
                tvRango6_7.text = rango6a7.toString()
                tvRango8_10.text = rango8a10.toString()
            }
        }

        with(resumen.mejoresPeores) {
            tvMasEntregada.text = masEntregada ?: "Sin datos"
            tvMenosEntregada.text = menosEntregada ?: "Sin datos"
            tvMayorPromedio.text = mayorPromedio ?: "Sin datos"
            tvMenorPromedio.text = menorPromedio ?: "Sin datos"
        }

        mostrarEstadisticasAdicionales(resumen)
    }

    private fun mostrarEstadisticasAdicionales(resumen: ResumenClase) {
        val porcentajeEntregado = resumen.estadoEntregas.getPorcentajeEntregado()
        val totalCalificaciones = resumen.calificaciones.distribucion.getTotalCalificaciones()

        if (porcentajeEntregado > 0) {
            val mensaje = "Porcentaje de entregas: ${String.format("%.1f", porcentajeEntregado)}%"
            if (totalCalificaciones > 0) {
                val (p1, p2, p3) = resumen.calificaciones.distribucion.getPorcentajes()
                val distribucionMsg = "\nDistribución: ${String.format("%.0f", p1)}% (0-5), ${String.format("%.0f", p2)}% (6-7), ${String.format("%.0f", p3)}% (8-10)"
                Toast.makeText(this, mensaje + distribucionMsg, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        tvFechaConsulta.text = "Error al cargar datos"
        tvTotalActividades.text = "0"
        tvValorMaximo.text = "0"
        tvEntregado.text = "0"
        tvPendiente.text = "0"
        tvNoEntregado.text = "0"
        tvPromedioGeneral.text = "0.0"
        tvRango0_5.text = "0"
        tvRango6_7.text = "0"
        tvRango8_10.text = "0"
        tvMasEntregada.text = "Sin datos"
        tvMenosEntregada.text = "Sin datos"
        tvMayorPromedio.text = "Sin datos"
        tvMenorPromedio.text = "Sin datos"

        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    companion object {
        const val EXTRA_ID_CLASE = "id_clase"
        const val TAG = "EstadisticasGrupoActivity"
    }
}