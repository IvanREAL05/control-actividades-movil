package com.example.control_actividades

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DetalleAlumnoActivity : AppCompatActivity() {

    // Views
    private lateinit var toolbar: Toolbar
    private lateinit var tvNombreCompleto: TextView
    private lateinit var tvMatricula: TextView
    private lateinit var tvGrupo: TextView
    private lateinit var tvCorreo: TextView
    private lateinit var tvPromedioGeneral: TextView
    private lateinit var tvActividadesEntregadas: TextView
    private lateinit var tvPuntosTotales: TextView
    private lateinit var tvTasaEntrega: TextView
    private lateinit var recyclerActividades: RecyclerView
    private lateinit var cardResumen: MaterialCardView
    private lateinit var tvAvatar: TextView

    // Datos del intent
    private var alumnoId: Int = -1
    private var claseId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_alumno)

        obtenerDatosIntent()
        initViews()
        setupToolbar()
        setupRecyclerView()
        cargarDetalleAlumno()
    }

    private fun obtenerDatosIntent() {
        alumnoId = intent.getIntExtra("ALUMNO_ID", -1)
        claseId = intent.getIntExtra("CLASE_ID", -1)
        Log.d("TRACE", "DetalleAlumnoActivity | idClase = $claseId, alumnoId = $alumnoId")

        if (alumnoId == -1 || claseId == -1) {
            Toast.makeText(this, "Error: Datos del alumno no válidos", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvNombreCompleto = findViewById(R.id.tvNombreCompleto)
        tvMatricula = findViewById(R.id.tvMatricula)
        tvGrupo = findViewById(R.id.tvGrupo)
        tvCorreo = findViewById(R.id.tvCorreo)
        tvPromedioGeneral = findViewById(R.id.tvPromedioGeneral)
        tvActividadesEntregadas = findViewById(R.id.tvActividadesEntregadas)
        tvPuntosTotales = findViewById(R.id.tvPuntosTotales)
        tvTasaEntrega = findViewById(R.id.tvTasaEntrega)
        recyclerActividades = findViewById(R.id.recyclerActividades)
        cardResumen = findViewById(R.id.cardResumen)
        tvAvatar = findViewById(R.id.tvAvatar)


    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Detalle del Alumno"

        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        recyclerActividades.layoutManager = LinearLayoutManager(this)
        recyclerActividades.setHasFixedSize(true)
    }

    private fun cargarDetalleAlumno() {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.instance
                val detalle = api.getDetalleAlumno(alumnoId, claseId)

                Log.d("DetalleAlumno", "Alumno recibido: $detalle")
                mostrarInformacionBasica(detalle)
                mostrarActividades(detalle.actividades)

            } catch (e: Exception) {
                Log.e("DetalleAlumno", "Error al cargar datos", e)
                Toast.makeText(this@DetalleAlumnoActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarInformacionBasica(detalle: DetalleAlumnoResponse) {
        val alumno = detalle.alumno
        val clase = detalle.clase
        val actividades = detalle.actividades
        val alumnoLetras = detalle.alumno

        tvNombreCompleto.text = "${alumno.nombre ?: ""} ${alumno.apellido ?: ""}"
        tvMatricula.text = "Matrícula: ${alumno.matricula ?: "N/A"}"
        tvGrupo.text = "Grupo: ${alumno.grupo ?: "N/A"}"
        tvCorreo.text = alumno.correo ?: "Correo no disponible"

        // Resumen de actividades
        val iniciales = (alumno.nombre?.firstOrNull()?.uppercase() ?: "") +
                (alumno.apellido?.firstOrNull()?.uppercase() ?: "")
        tvAvatar.text = iniciales
        val entregadas = actividades.count { it.estado == "entregado" }
        val total = actividades.size
        val puntosObtenidos = actividades.sumOf { it.calificacion?.toDouble() ?: 0.0 }
        val puntosTotales = actividades.sumOf { it.valor_maximo.toDouble() }

        // Calcular promedio de calificaciones
        val calificaciones = actividades.mapNotNull { it.calificacion }
        val promedio = if (puntosTotales > 0) {
            (puntosObtenidos / puntosTotales * 10)
        } else 0.0

        tvPromedioGeneral.text = String.format("%.2f", promedio)
        tvActividadesEntregadas.text = "$entregadas de $total"
        tvPuntosTotales.text = "$puntosObtenidos/$puntosTotales pts"

        val tasaEntrega = if (total > 0) (entregadas * 100 / total) else 0
        tvTasaEntrega.text = "$tasaEntrega%"
    }

    private fun mostrarActividades(actividades: List<ActividadDetalle>) {
        val adapter = ActividadesAlumnoAdapter(actividades)
        recyclerActividades.adapter = adapter
    }
}
