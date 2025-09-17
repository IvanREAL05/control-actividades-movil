package com.example.control_actividades

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

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

    // Variables para manejar datos
    private var idClase: Int = -1
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estadisticas_grupo)

        // Obtener el ID de la clase del intent
        idClase = intent.getIntExtra("id_clase", -1)
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

        // Cargar datos
        cargarEstadisticas()
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
        // Fecha de consulta
        tvFechaConsulta.text = "Consultado el: ${resumen.fechaConsulta}"

        // Totales
        tvTotalActividades.text = resumen.totales.actividades.toString()
        tvValorMaximo.text = resumen.totales.valorMaximoPromedio?.toString() ?: "N/A"

        // Estado de entregas
        with(resumen.estadoEntregas) {
            tvEntregado.text = entregado.toString()
            tvPendiente.text = pendiente.toString()
            tvNoEntregado.text = noEntregado.toString()
        }

        // Calificaciones
        with(resumen.calificaciones) {
            tvPromedioGeneral.text = String.format("%.1f", promedioGeneral)

            with(distribucion) {
                tvRango0_5.text = rango0a5.toString()
                tvRango6_7.text = rango6a7.toString()
                tvRango8_10.text = rango8a10.toString()
            }
        }

        // Rankings - manejar valores nulos
        with(resumen.mejoresPeores) {
            tvMasEntregada.text = masEntregada ?: "Sin datos"
            tvMenosEntregada.text = menosEntregada ?: "Sin datos"
            tvMayorPromedio.text = mayorPromedio ?: "Sin datos"
            tvMenorPromedio.text = menorPromedio ?: "Sin datos"
        }

        // Opcional: Mostrar estadísticas adicionales con Toast
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
        // Mostrar mensaje de error en lugar de "Cargando..."
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

        // Mostrar toast con el error
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