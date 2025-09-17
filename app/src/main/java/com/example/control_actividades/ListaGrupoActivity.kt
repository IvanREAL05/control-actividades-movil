package com.example.control_actividades

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.DownloadManager
import android.content.Context
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

class ListaGrupoActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnDescargar: Button
    private lateinit var btnVolver: Button
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var alumnoAdapter: AlumnoAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var btnAsistenciaManual: MaterialButton

    // NUEVOS TextViews para estadísticas
    private lateinit var tvPresentes: TextView
    private lateinit var tvAusentes: TextView
    private lateinit var tvJustificantes: TextView

    private var idClase: Int = -1

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
        if (idClase == -1) {
            Toast.makeText(this, "ID de clase no recibido", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        cargarListaAlumnos(idClase)
    }

    private fun initializeViews() {
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.recyclerAlumnos)
        btnDescargar = findViewById(R.id.btnDescargarLista)
        btnVolver = findViewById(R.id.btnVolver)
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
            mostrarDialogoSalida()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        btnDescargar.setOnClickListener {
            descargarLista()
        }

        btnVolver.setOnClickListener {
            finish()
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

    private fun descargarLista() {
        val url = "https://control-asistenciav1.onrender.com/api/asistencia/alumnos/clase/$idClase/excel"

        try {
            val request = DownloadManager.Request(Uri.parse(url))
            request.setTitle("Lista de Asistencia")
            request.setDescription("Descargando archivo Excel...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "lista_asistencia_clase_$idClase.xlsx")

            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)

            Toast.makeText(this, "Descarga iniciada...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al descargar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        mostrarDialogoSalida()
    }

    private fun mostrarDialogoSalida() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Confirmar salida")
        builder.setMessage("¿Realmente quieres salir? Se cerrará la sesión del docente.")
        builder.setPositiveButton("Sí") { _, _ ->
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
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