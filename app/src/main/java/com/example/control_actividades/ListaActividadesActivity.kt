package com.example.control_actividades

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.os.Build
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore


class ListaActividadesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AlumnoActividadAdapter
    private lateinit var apiService: ApiService
    private lateinit var toolbar: Toolbar
    private lateinit var btnDescargarExcel: Button
    private lateinit var progressBarDescarga: ProgressBar
    private lateinit var actividad: Actividad

    private var actividadId: Int = -1
    private var actividadTitulo: String = ""
    private var alumnosCompletos: MutableList<AlumnoActividad> = mutableListOf()
    private var tipoDescarga: String? = null

    companion object {
        const val EXTRA_ACTIVIDAD_ID = "actividad_id"
        const val EXTRA_ACTIVIDAD_TITULO = "actividad_titulo"
        const val EXTRA_TIPO_ACTIVIDAD = "tipo_actividad"
        const val EXTRA_VALOR_MAXIMO = "valor_maximo"
    }

    private val requestStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            when (tipoDescarga) {
                "CSV" -> iniciarDescargaCSV()
            }
        } else {
            Toast.makeText(this, "❌ Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_actividades)

        // Obtener datos del intent
        actividadId = intent.getIntExtra(EXTRA_ACTIVIDAD_ID, -1)
        actividadTitulo = intent.getStringExtra(EXTRA_ACTIVIDAD_TITULO) ?: "Lista de Alumnos"

        if (actividadId == -1) {
            Toast.makeText(this, "Error: ID de actividad no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 🔹 PRIMERO: Obtener la actividad completa desde el backend
        configurarToolbar(actividadTitulo)
        inicializarVistas()
        configurarRetrofit()

        // Cargar datos de la actividad antes de cargar alumnos
        cargarDatosActividad()
    }

    private fun configurarToolbar(titulo: String) {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = titulo
    }

    private fun inicializarVistas() {
        recyclerView = findViewById(R.id.recyclerViewAlumnos)
        recyclerView.layoutManager = LinearLayoutManager(this)

        btnDescargarExcel = findViewById(R.id.btnDescargarExcel)
        progressBarDescarga = findViewById(R.id.progressBarDescarga)

        btnDescargarExcel.setOnClickListener {
            tipoDescarga = "CSV"
            verificarPermisosYDescargar()
        }
    }

    private fun configurarRetrofit() {
        apiService = RetrofitClient.instance
    }

    // 🔹 NUEVO: Método para cargar datos de la actividad
    private fun cargarDatosActividad() {
        Log.d("ListaActividades", "📥 Cargando datos de actividad ID: $actividadId")

        lifecycleScope.launch {
            try {
                // 🔹 Asume que tienes un endpoint para obtener una actividad por ID
                // Si no lo tienes, debes crearlo o pasar los datos desde el Intent
                val actividadResponse = apiService.getActividadPorId(actividadId)

                actividad = actividadResponse

                Log.d("ListaActividades", "✅ Actividad cargada:")
                Log.d("ListaActividades", "   Tipo: ${actividad.tipo_actividad}")
                Log.d("ListaActividades", "   Valor máximo: ${actividad.valor_maximo}")

                // Ahora sí, cargar la lista de alumnos
                cargarListaAlumnos(actividadId)

            } catch (e: Exception) {
                Log.e("ListaActividades", "❌ Error al cargar datos de actividad", e)

                // 🔹 ALTERNATIVA: Si el endpoint no existe, obtener del Intent
                val tipoActividad = intent.getStringExtra(EXTRA_TIPO_ACTIVIDAD) ?: "tarea"
                val valorMaximo = intent.getIntExtra(EXTRA_VALOR_MAXIMO, 10)

                actividad = Actividad(
                    id_actividad = actividadId,
                    id_clase = 0,
                    titulo = actividadTitulo,
                    descripcion = null,
                    fecha_entrega = "",
                    hora_entrega = null,
                    fecha_creacion = "",
                    estado = null,
                    fecha_entrega_real = null,
                    vigencia = null,
                    valor_maximo = valorMaximo,
                    tipo_actividad = tipoActividad
                )

                Log.d("ListaActividades", "⚠️ Usando datos del Intent:")
                Log.d("ListaActividades", "   Tipo: ${actividad.tipo_actividad}")
                Log.d("ListaActividades", "   Valor máximo: ${actividad.valor_maximo}")

                cargarListaAlumnos(actividadId)
            }
        }
    }

    private fun verificarPermisosYDescargar() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                iniciarDescargaCSV()
            }
            else -> {
                requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun iniciarDescargaCSV() {
        if (alumnosCompletos.isEmpty()) {
            Toast.makeText(this, "⚠️ No hay datos para exportar", Toast.LENGTH_SHORT).show()
            return
        }

        btnDescargarExcel.isEnabled = false
        btnDescargarExcel.text = "📊 Generando CSV..."
        progressBarDescarga.visibility = ProgressBar.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val archivoUri = generarArchivoCSV()

                withContext(Dispatchers.Main) {
                    ocultarProgreso()
                    if (archivoUri != null) {
                        Toast.makeText(
                            this@ListaActividadesActivity,
                            "✅ CSV guardado en Descargas",
                            Toast.LENGTH_LONG
                        ).show()

                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(archivoUri, "text/csv")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@ListaActividadesActivity,
                                "ℹ️ Archivo guardado, pero no se encontró app para abrir CSV",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@ListaActividadesActivity,
                            "❌ Error al generar el archivo CSV",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CSVDownload", "Error generando CSV", e)
                withContext(Dispatchers.Main) {
                    ocultarProgreso()
                    Toast.makeText(
                        this@ListaActividadesActivity,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun generarArchivoCSV(): Uri? {
        return try {
            val csvContent = StringBuilder()
            csvContent.append("No.,Nombre,Apellido,Matrícula,Estado,Calificación,Fecha\n")

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            alumnosCompletos.forEachIndexed { index, alumno ->
                csvContent.append("${index + 1},")
                csvContent.append("\"${alumno.nombre ?: ""}\",")
                csvContent.append("\"${alumno.apellido ?: ""}\",")
                csvContent.append("\"${alumno.matricula ?: "N/A"}\",")
                csvContent.append("\"${alumno.estado}\",")
                csvContent.append("\"${alumno.calificacion ?: "N/A"}\",")
                csvContent.append("\"${alumno.fechaEntregaReal ?: dateFormat.format(Date())}\"\n")
            }

            val resolver = applicationContext.contentResolver
            val fileName = "Lista_${actividadTitulo.replace(" ", "_")}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"

            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                Uri.fromFile(file)
            }

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(csvContent.toString().toByteArray(Charsets.UTF_8))
                }
            }

            Log.d("CSVDownload", "Archivo CSV creado: $fileName")
            uri
        } catch (e: Exception) {
            Log.e("CSVDownload", "Error creando CSV con MediaStore", e)
            null
        }
    }

    private fun ocultarProgreso() {
        btnDescargarExcel.isEnabled = true
        btnDescargarExcel.text = "📊 Descargar Lista en Excel"
        progressBarDescarga.visibility = ProgressBar.GONE
    }

    private fun cargarListaAlumnos(actividadId: Int) {
        Log.d("ListaActividades", "Cargando alumnos para actividad ID: $actividadId")

        lifecycleScope.launch {
            try {
                val alumnos = apiService.getEstudiantes(actividadId)

                if (alumnos.isEmpty()) {
                    Toast.makeText(
                        this@ListaActividadesActivity,
                        "No se encontraron alumnos para esta actividad",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    mostrarAlumnos(alumnos.toMutableList())
                }

            } catch (e: Exception) {
                Log.e("ListaActividades", "Error al cargar la lista de alumnos", e)
                Toast.makeText(
                    this@ListaActividadesActivity,
                    "Error de conexión: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun mostrarAlumnos(alumnos: MutableList<AlumnoActividad>) {
        alumnosCompletos = alumnos.toMutableList()
        Log.d("ListaActividades", "Lista completa guardada con ${alumnosCompletos.size} elementos")

        // 🔹 Verificar que actividad esté inicializada
        if (!::actividad.isInitialized) {
            Log.e("ListaActividades", "❌ Actividad no inicializada, usando valores por defecto")
            actividad = Actividad(
                id_actividad = actividadId,
                id_clase = 0,
                titulo = actividadTitulo,
                descripcion = null,
                fecha_entrega = "",
                hora_entrega = null,
                fecha_creacion = "",
                estado = null,
                fecha_entrega_real = null,
                vigencia = null,
                valor_maximo = 10,
                tipo_actividad = "tarea"
            )
        }

        Log.d("ListaActividades", "🎯 Creando adapter con:")
        Log.d("ListaActividades", "   Tipo: ${actividad.tipo_actividad}")
        Log.d("ListaActividades", "   Valor máximo: ${actividad.valor_maximo}")

        adapter = AlumnoActividadAdapter(
            alumnos = alumnos.toMutableList(),
            actividadId = actividadId,
            tipoActividad = actividad.tipo_actividad,
            calificacionMaxima = actividad.valor_maximo,
            apiService = apiService,
            onEstadoChanged = { alumno, nuevoEstado ->
                actualizarEstadoEnListaPrincipal(alumno, nuevoEstado)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun actualizarEstadoEnListaPrincipal(alumno: AlumnoActividad, nuevoEstado: String) {
        Log.d("ListaActividades", "🔄 Actualizando estado local: ${alumno.nombre} -> $nuevoEstado")

        val index = alumnosCompletos.indexOfFirst { it.idEstudiante == alumno.idEstudiante }
        if (index != -1) {
            alumnosCompletos[index] = alumnosCompletos[index].copy(estado = nuevoEstado)
            Log.d("ListaActividades", "✅ Estado actualizado en lista principal en posición $index")
        } else {
            Log.e("ListaActividades", "❌ No se encontró el alumno con ID ${alumno.idEstudiante}")
        }
    }

    private fun recargarListaAlumnos() {
        Log.d("ListaActividades", "Recargando lista de alumnos...")
        cargarListaAlumnos(actividadId)
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_filtro_estados, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.filtro_todos -> {
                filtrarPorEstado(null)
                true
            }
            R.id.filtro_entregado -> {
                filtrarPorEstado("entregado")
                true
            }
            R.id.filtro_pendiente -> {
                filtrarPorEstado("pendiente")
                true
            }
            R.id.filtro_no_entregado -> {
                filtrarPorEstado("no entregado")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun filtrarPorEstado(estado: String?) {
        if (alumnosCompletos.isEmpty()) {
            Log.w("ListaActividades", "La lista completa está vacía, recargando datos...")
            recargarListaAlumnos()
            return
        }

        Log.d("ListaActividades", "🔍 Filtrando por estado: '$estado'")

        val alumnosFiltrados = if (estado == null) {
            alumnosCompletos.toList()
        } else {
            alumnosCompletos.filter { it.estado == estado }
        }

        Log.d("ListaActividades", "📋 Resultado del filtro: ${alumnosFiltrados.size} elementos")

        if (::adapter.isInitialized) {
            adapter.updateAlumnos(alumnosFiltrados)
        } else {
            Log.e("ListaActividades", "❌ Adapter no está inicializado")
            return
        }

        supportActionBar?.title = when (estado) {
            null -> actividadTitulo
            "entregado" -> "Entregados (${alumnosFiltrados.size})"
            "pendiente" -> "Pendientes (${alumnosFiltrados.size})"
            "no entregado" -> "No Entregados (${alumnosFiltrados.size})"
            else -> "Lista de Alumnos"
        }
    }
}