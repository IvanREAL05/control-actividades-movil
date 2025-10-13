package com.example.control_actividades

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Environment
import android.view.View
import android.widget.ProgressBar
import android.view.Menu
import android.view.MenuItem
import android.util.Log
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.ContentValues
import android.provider.MediaStore
import android.net.Uri



class ListaGrupoActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnDescargar: Button
    private lateinit var btnObservaciones: Button
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var alumnoAdapter: AlumnoAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var btnAsistenciaManual: MaterialButton

    // NUEVOS TextViews para estadísticas
    private lateinit var tvPresentes: TextView
    private lateinit var tvAusentes: TextView
    private lateinit var tvJustificantes: TextView

    private var idClase: Int = -1
    private var idProfesor: Int = -1

    // Variables para filtrado
    private var alumnosCompletos = listOf<AlumnoResponse>()
    private var tituloOriginal = "Lista del grupo"
    private var filtroActual: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_grupo)

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupListeners()

        idClase = intent.getIntExtra("id_clase", -1)
        idProfesor = intent.getIntExtra("id_profesor", -1)
        Log.d("ID_DEBUG", "🔸 Recibido en ListaGrupo: idClase=$idClase, idProfesor=$idProfesor")
        if (idClase == -1) {
            Toast.makeText(this, "ID de clase no recibido", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (idProfesor == -1) {
            Toast.makeText(this, "ID de profesor no recibido", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        cargarListaAlumnos(idClase)
    }

    private fun initializeViews() {
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.recyclerAlumnos)
        btnDescargar = findViewById(R.id.btnDescargarLista)
        btnObservaciones = findViewById(R.id.btnObservaciones)

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        btnAsistenciaManual = findViewById(R.id.btnAsistenciaManual)

        // Inicializar TextViews de estadísticas
        tvPresentes = findViewById(R.id.tvPresentes)
        tvAusentes = findViewById(R.id.tvAusentes)
        tvJustificantes = findViewById(R.id.tvJustificantes)
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = tituloOriginal
        }

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        btnDescargar.setOnClickListener {
            descargarListaClase(idClase)
        }

        btnObservaciones.setOnClickListener {
            Log.d("OBS_DEBUG", "🔹 En ListaGrupoActivity: idClase=$idClase, idProfesor=$idProfesor")
            val intent = Intent(this, ObservacionesActivity::class.java)
            intent.putExtra("id_clase", idClase)
            intent.putExtra("nombre_grupo", "Grupo $idClase")
            intent.putExtra("id_profesor", idProfesor)  // ✅ ENVIAR ID PROFESOR A OBSERVACIONES
            startActivity(intent)
        }

        swipeRefreshLayout.setOnRefreshListener {
            refreshLista()
        }

        // Colores del SwipeRefresh
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        btnAsistenciaManual.setOnClickListener {
            val intent = Intent(this, AsistenciaManualActivity::class.java)
            intent.putExtra("idClase", idClase)
            intent.putExtra("nombreGrupo", "Grupo $idClase")
            startActivity(intent)
        }
    }

    private fun refreshLista() {
        cargarListaAlumnos(idClase, esRefresh = true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_filtros_asistencia, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.filtro_todos -> {
                filtroActual = null
                filtrarPorEstado(null)
                true
            }
            R.id.filtro_presente -> {
                filtroActual = "presente"
                filtrarPorEstado("presente")
                true
            }
            R.id.filtro_ausente -> {
                filtroActual = "ausente"
                filtrarPorEstado("ausente")
                true
            }
            R.id.filtro_justificante -> {
                filtroActual = "justificante"
                filtrarPorEstado("justificante")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun filtrarPorEstado(estado: String?) {
        if (alumnosCompletos.isEmpty()) return

        val alumnosFiltrados = if (estado == null) {
            alumnosCompletos
        } else {
            alumnosCompletos.filter { it.estado.equals(estado, ignoreCase = true) }
        }

        if (::alumnoAdapter.isInitialized) {
            alumnoAdapter.updateAlumnos(alumnosFiltrados)
        }

        // Actualizar estadísticas con la lista filtrada
        actualizarEstadisticas(alumnosFiltrados)

        supportActionBar?.title = when (estado) {
            null -> "$tituloOriginal (${alumnosFiltrados.size})"
            "presente" -> "Presentes (${alumnosFiltrados.size})"
            "ausente" -> "Ausentes (${alumnosFiltrados.size})"
            "justificante" -> "Justificantes (${alumnosFiltrados.size})"
            else -> tituloOriginal
        }
    }

    private fun actualizarEstadisticas(alumnos: List<AlumnoResponse>) {
        val presentes = alumnos.count { it.estado.equals("presente", ignoreCase = true) }
        val ausentes = alumnos.count { it.estado.equals("ausente", ignoreCase = true) }
        val justificantes = alumnos.count { it.estado.equals("justificante", ignoreCase = true) }

        // Actualizar los TextViews
        tvPresentes.text = presentes.toString()
        tvAusentes.text = ausentes.toString()
        tvJustificantes.text = justificantes.toString()

        Log.d("ListaGrupoActivity", "📊 Estadísticas actualizadas:")
        Log.d("ListaGrupoActivity", "   Presentes: $presentes")
        Log.d("ListaGrupoActivity", "   Ausentes: $ausentes")
        Log.d("ListaGrupoActivity", "   Justificantes: $justificantes")
    }

    private fun cargarListaAlumnos(idClase: Int, esRefresh: Boolean = false) {
        if (!esRefresh) {
            progressBar.visibility = View.VISIBLE
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getListaAlumnos(idClase)

                progressBar.visibility = View.GONE

                if (esRefresh) {
                    swipeRefreshLayout.isRefreshing = false
                }

                if (response.isSuccessful) {
                    val listaAlumnos = response.body() ?: emptyList()

                    // Guardar lista completa para filtrado
                    alumnosCompletos = listaAlumnos

                    if (::alumnoAdapter.isInitialized) {
                        alumnoAdapter.updateAlumnos(listaAlumnos)

                        // Reaplicar filtro actual si existe
                        if (filtroActual != null) {
                            filtrarPorEstado(filtroActual)
                        } else {
                            // Actualizar estadísticas con lista completa
                            actualizarEstadisticas(listaAlumnos)
                        }
                    } else {
                        alumnoAdapter = AlumnoAdapter(
                            this@ListaGrupoActivity,
                            listaAlumnos,
                            idClase
                        ) {
                            cargarListaAlumnos(idClase)
                        }
                        recyclerView.adapter = alumnoAdapter

                        // Actualizar estadísticas iniciales
                        actualizarEstadisticas(listaAlumnos)
                    }

                    // Actualizar título con total
                    if (filtroActual == null) {
                        supportActionBar?.title = "$tituloOriginal (${listaAlumnos.size})"
                    }

                    if (esRefresh) {
                        Toast.makeText(this@ListaGrupoActivity, "Lista actualizada", Toast.LENGTH_SHORT).show()
                    }

                    Log.d("ListaGrupoActivity", "📚 Lista cargada: ${listaAlumnos.size} alumnos")
                    Log.d("ListaGrupoActivity", "Estados encontrados: ${listaAlumnos.map { it.estado }.distinct()}")
                } else {
                    Toast.makeText(this@ListaGrupoActivity, "Error al cargar lista: ${response.code()}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                progressBar.visibility = View.GONE

                if (esRefresh) {
                    swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(this@ListaGrupoActivity, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ListaGrupoActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun descargarListaClase(idClase: Int) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.instance.descargarReporteClase(idClase)
                    if (response.isSuccessful && response.body() != null) {
                        val inputStream = response.body()!!.byteStream()
                        val fileName = "Reporte_Clase_$idClase.xlsx"

                        // Detectar versión de Android
                        val isAndroid10OrAbove = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

                        if (isAndroid10OrAbove) {
                            // 🟢 Android 10+
                            val resolver = contentResolver
                            val contentValues = ContentValues().apply {
                                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                put(MediaStore.Downloads.IS_PENDING, 1)
                            }

                            val uri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                            uri?.let {
                                resolver.openOutputStream(it)?.use { output ->
                                    inputStream.copyTo(output)
                                }
                                contentValues.clear()
                                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                                resolver.update(it, contentValues, null, null)
                            }
                        } else {
                            // 🔵 Android 9 o inferior
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            if (!downloadsDir.exists()) downloadsDir.mkdirs()
                            val file = java.io.File(downloadsDir, fileName)
                            java.io.FileOutputStream(file).use { output ->
                                inputStream.copyTo(output)
                            }

                            // Notificar al sistema para que aparezca en "Descargas"
                            val uri = android.net.Uri.fromFile(file)
                            val scanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                            scanIntent.data = uri
                            sendBroadcast(scanIntent)
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ListaGrupoActivity,
                                "Reporte guardado en Descargas: $fileName",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ListaGrupoActivity,
                                "Error al descargar el reporte",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ListaGrupoActivity,
                            "Ocurrió un error al descargar el archivo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    Log.e("ListaGrupoActivity", "Error descargando Excel", e)
                }
            }
        }
    }

    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        finish()
    }

}

fun AlumnoAdapter.hasUpdateMethod(): Boolean {
    return try {
        this.javaClass.getMethod("updateAlumnos", List::class.java)
        true
    } catch (e: NoSuchMethodException) {
        false
    }
}