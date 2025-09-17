package com.example.control_actividades

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch



class AsistenciaManualActivity : AppCompatActivity() { // ✅ NOMBRE CORREGIDO

    private lateinit var recyclerAlumnos: RecyclerView
    private lateinit var alumnoManualAdapter: AlumnoManualAdapter
    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnGuardarCambios: MaterialButton


    private var idClase: Int = 0
    private var nombreGrupo: String = ""
    private var cambiosRealizados = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asistencia_manual)

        // Obtener datos del Intent
        idClase = intent.getIntExtra("idClase", 0)
        nombreGrupo = intent.getStringExtra("nombreGrupo") ?: "Grupo"

        initializeViews()
        setupToolbar()
        setupBotonGuardar()
        cargarAlumnos()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerAlumnos = findViewById(R.id.recyclerAlumnosManual)
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios)

        recyclerAlumnos.layoutManager = LinearLayoutManager(this)
    }

    private fun setupToolbar() {
        toolbar.title = "Asistencia Manual - $nombreGrupo"
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupBotonGuardar() {
        // Inicialmente deshabilitado y transparente
        btnGuardarCambios.alpha = 0.5f
        btnGuardarCambios.text = "Sin cambios por guardar"
        btnGuardarCambios.isEnabled = false

        btnGuardarCambios.setOnClickListener {
            // Animación de presión
            btnGuardarCambios.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    btnGuardarCambios.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                }

            // Cambiar texto y deshabilitar
            btnGuardarCambios.text = "⏳ Guardando $cambiosRealizados cambios..."
            btnGuardarCambios.isEnabled = false

            // Simular proceso de guardado
            Handler(Looper.getMainLooper()).postDelayed({
                btnGuardarCambios.text = "✅ ¡$cambiosRealizados cambios guardados!"

                // Esperar un poco y mostrar mensaje final
                Handler(Looper.getMainLooper()).postDelayed({
                    Toast.makeText(this, "🎉 Asistencia actualizada correctamente", Toast.LENGTH_SHORT).show()

                    // Pequeño delay adicional antes de cerrar
                    Handler(Looper.getMainLooper()).postDelayed({
                        finish() // Regresa a ListaGrupoActivity
                    }, 800)
                }, 1000)
            }, 1500)
        }
    }

    private fun onCambioRealizado() {
        cambiosRealizados++

        // Activar botón si hay cambios y no está ya procesando
        if (cambiosRealizados > 0 && !btnGuardarCambios.isEnabled) {
            btnGuardarCambios.isEnabled = true
            btnGuardarCambios.alpha = 1f
            btnGuardarCambios.text = "Guardar cambios ($cambiosRealizados)"

            // Animación de "despertar" del botón
            btnGuardarCambios.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(200)
                .withEndAction {
                    btnGuardarCambios.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                }
        } else if (btnGuardarCambios.isEnabled) {
            // Solo actualizar el contador si el botón ya está activo
            btnGuardarCambios.text = "Guardar cambios ($cambiosRealizados)"
        }
    }

    private fun cargarAlumnos() {
        if (idClase == 0) {
            Toast.makeText(this, "Error: ID de clase no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Usamos lifecycleScope para llamar a la función suspend
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getListaAlumnos(idClase)

                if (response.isSuccessful && response.body() != null) {
                    val alumnos: List<AlumnoResponse> = response.body()!!
                    setupAdapter(alumnos)
                } else {
                    Toast.makeText(
                        this@AsistenciaManualActivity,
                        "Error al cargar alumnos: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AsistenciaManualActivity,
                    "Error de conexión: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun recargarLista() {
        cargarAlumnos() // o como llames a la función que obtiene la lista desde el servidor
    }

    private fun setupAdapter(alumnos: List<AlumnoResponse>) {
        // Convertir List a MutableList
        val alumnosMutable = alumnos.toMutableList()

        alumnoManualAdapter = AlumnoManualAdapter(
            context = this,
            alumnos = alumnosMutable,
            idClase = idClase,
            onEstadoActualizado = {
                recargarLista()
                onCambioRealizado()
            },
            lifecycleOwner = this
        )
        recyclerAlumnos.adapter = alumnoManualAdapter
    }
}