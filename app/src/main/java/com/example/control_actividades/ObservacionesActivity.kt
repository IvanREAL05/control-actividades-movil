package com.example.control_actividades

import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.appbar.MaterialToolbar
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class ObservacionesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: MaterialToolbar

    private lateinit var adapter: ObservacionesAdapter
    private var listaAlumnos: List<AlumnoResponse> = emptyList()
    private var observacionesExistentes: Map<Int, Observacion> = emptyMap()

    private var idClase: Int = -1
    private var idProfesor: Int = -1
    private var nombreGrupo: String = ""

    private val apiService = RetrofitClient.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_observaciones)

        inicializarVistas()
        obtenerDatosIntent()
        configurarToolbar()
        configurarAdapter()
        configurarRecyclerView()
        configurarListeners()

        cargarDatos()
    }

    private fun inicializarVistas() {
        recyclerView = findViewById(R.id.recyclerObservaciones)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        progressBar = findViewById(R.id.progressBar)
        // ✅ btnAgregar eliminado - ya no es necesario
        toolbar = findViewById(R.id.toolbar)
    }

    private fun obtenerDatosIntent() {
        idClase = intent.getIntExtra("id_clase", -1)
        idProfesor = intent.getIntExtra("id_profesor", -1)
        nombreGrupo = intent.getStringExtra("nombre_grupo") ?: "Grupo"

        Log.d("OBS_DEBUG", "🔸 Recibido en Observaciones: idClase=$idClase, idProfesor=$idProfesor, nombreGrupo=$nombreGrupo")

        if (idClase == -1) {
            Toast.makeText(this, "Error: No se recibió el ID de la clase", Toast.LENGTH_LONG).show()
            finish()
        }
        if (idProfesor == -1) {
            Toast.makeText(this, "ID de profesor no recibido", Toast.LENGTH_LONG).show()
            finish()
            return
        }
    }

    private fun configurarToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Observaciones - $nombreGrupo"
            setDisplayHomeAsUpEnabled(true)
        }

        toolbar.setNavigationOnClickListener {
            finish() // Ya no necesitamos verificar cambios pendientes
        }
    }

    private fun configurarAdapter() {
        adapter = ObservacionesAdapter(
            context = this,
            listaAlumnos = listaAlumnos,
            observacionesExistentes = observacionesExistentes,
            // ✅ CALLBACK PARA GUARDADO INMEDIATO
            onObservacionChanged = { estudianteId, nuevoEstado ->
                guardarObservacionInmediata(estudianteId, nuevoEstado)
            },
            // ✅ CALLBACK PARA ELIMINACIÓN INMEDIATA
            onEliminarObservacion = { estudianteId ->
                eliminarObservacionInmediata(estudianteId)
            }
        )
    }

    private fun configurarRecyclerView() {
        recyclerView.apply {
            adapter = this@ObservacionesActivity.adapter
            layoutManager = LinearLayoutManager(this@ObservacionesActivity)
            addItemDecoration(DividerItemDecoration(this@ObservacionesActivity, DividerItemDecoration.VERTICAL))
        }
    }

    private fun configurarListeners() {
        // SwipeRefresh
        swipeRefreshLayout.setOnRefreshListener {
            cargarDatos()
        }

        // ✅ BOTÓN AGREGAR REMOVIDO - Ya no es necesario
        // La funcionalidad masiva se elimina porque cada observación es individual
    }

    private fun cargarDatos() {
        mostrarCargando(true)
        cargarAlumnos()
    }

    private fun cargarAlumnos() {
        lifecycleScope.launch {
            mostrarCargando(true)
            try {
                val response = apiService.getListaAlumnos(idClase) // suspend function
                if (response.isSuccessful) {
                    val alumnos = response.body() ?: emptyList()
                    listaAlumnos = alumnos
                    adapter.actualizarAlumnos(alumnos)
                    cargarObservacionesExistentes()
                } else {
                    mostrarError("Error al cargar alumnos: ${response.code()}")
                }
            } catch (e: Exception) {
                mostrarError("Error al cargar alumnos: ${e.message}")
            } finally {
                mostrarCargando(false)
            }
        }
    }

    private fun cargarObservacionesExistentes() {
        lifecycleScope.launch {
            swipeRefreshLayout.isRefreshing = true
            try {

                val observacionResponse: ObservacionResponse = apiService.obtenerObservaciones()

                val observaciones = mutableMapOf<Int, Observacion>()
                observacionResponse.observaciones?.forEach { obs ->
                    observaciones[obs.estudiante_id] = obs
                }

                observacionesExistentes = observaciones
                adapter.actualizarObservaciones(observaciones)

            } catch (e: Exception) {
                mostrarError("Error al cargar observaciones: ${e.message}")
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }


    // ✅ GUARDADO INMEDIATO DE OBSERVACIÓN
    private fun guardarObservacionInmediata(estudianteId: Int, estado: Int) {
        Log.d("OBS_DEBUG", "💾 Guardando inmediatamente: estudianteId=$estudianteId, estado=$estado")

        // Mostrar indicador de carga sutil
        mostrarProgresoPequeno(true, estudianteId)

        val request = CrearActualizarObservacionRequest(
            estudiante_id = estudianteId,
            profesor_id = idProfesor,
            estado = estado
        )

        val observacionExistente = observacionesExistentes[estudianteId]

        when {
            observacionExistente != null -> {
                // Actualizar observación existente
                actualizarObservacion(observacionExistente.id, request) { success ->
                    manejarResultadoGuardado(estudianteId, estado, success)
                }
            }
            else -> {
                // Crear nueva observación
                crearObservacion(request) { success ->
                    manejarResultadoGuardado(estudianteId, estado, success)
                }
            }
        }
    }

    // ✅ ELIMINACIÓN INMEDIATA DE OBSERVACIÓN
    private fun eliminarObservacionInmediata(estudianteId: Int) {
        val observacionExistente = observacionesExistentes[estudianteId]

        if (observacionExistente == null) {
            Toast.makeText(this, "No hay observación que eliminar", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar diálogo de confirmación
        AlertDialog.Builder(this)
            .setTitle("Eliminar observación")
            .setMessage("¿Estás seguro de que deseas eliminar esta observación?")
            .setPositiveButton("Eliminar") { _, _ ->
                realizarEliminacion(estudianteId, observacionExistente.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun realizarEliminacion(estudianteId: Int, observacionId: Int) {
        lifecycleScope.launch {
            mostrarProgresoPequeno(true, estudianteId)
            try {
                val response = apiService.eliminarObservacion(observacionId) // suspend function
                if (response.success) {
                    val nuevasObservaciones = observacionesExistentes.toMutableMap()
                    nuevasObservaciones.remove(estudianteId)
                    observacionesExistentes = nuevasObservaciones
                    adapter.actualizarObservaciones(observacionesExistentes)
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this@ObservacionesActivity, "✅ Observación eliminada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ObservacionesActivity, "❌ Error al eliminar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ObservacionesActivity, "❌ Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                mostrarProgresoPequeno(false, estudianteId)
            }
        }
    }


    private fun manejarResultadoGuardado(estudianteId: Int, estado: Int, success: Boolean) {
        mostrarProgresoPequeno(false, estudianteId)

        if (success) {
            val nombreEstado = obtenerNombreEstado(estado)
            Toast.makeText(this, "✅ $nombreEstado guardado", Toast.LENGTH_SHORT).show()

            // ✅ OPCIÓN ALTERNATIVA: Recargar datos desde servidor
            // Esto es más confiable que crear manualmente la observación local
            cargarObservacionesExistentes()
        } else {
            Toast.makeText(this, "❌ Error al guardar", Toast.LENGTH_SHORT).show()
            // Revertir cambio en el adapter si falla
            adapter.notifyDataSetChanged()
        }
    }

    private fun mostrarProgresoPequeno(mostrar: Boolean, estudianteId: Int) {
        // Aquí podrías implementar un indicador de progreso por item
        // Por ahora solo usaremos el toast
    }

    // ✅ FUNCIONES DE OBSERVACIÓN MASIVA ELIMINADAS
    // Ya no son necesarias porque cada observación se maneja individualmente

    private fun crearObservacion(
        body: CrearActualizarObservacionRequest,
        callback: (Boolean) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val response = apiService.crearObservacion(body)
                callback(response.success)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }


    private fun actualizarObservacion(
        observacionId: Int,
        body: CrearActualizarObservacionRequest,
        callback: (Boolean) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val response = apiService.actualizarObservacion(observacionId, body)
                callback(response.success)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }


    private fun mostrarCargando(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        recyclerView.visibility = if (mostrar) View.GONE else View.VISIBLE
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, "❌ $mensaje", Toast.LENGTH_LONG).show()
    }



    private fun obtenerNombreEstado(estado: Int): String {
        return when (estado) {
            1 -> "🏆"
            2 -> "🔴"
            3 -> "🩺"
            4 -> "💛"
            5 -> "🟣"
            6 -> "👷‍♂️"
            else -> "Sin observación"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


}