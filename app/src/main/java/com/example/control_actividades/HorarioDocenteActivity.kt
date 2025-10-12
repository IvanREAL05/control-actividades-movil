package com.example.control_actividades

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class HorarioDocenteActivity : AppCompatActivity() {
    private var idProfesor: Int = -1

    // Views del layout
    private lateinit var txtNombreProfesor: TextView
    private lateinit var txtTotalClases: TextView
    private lateinit var progressBarHorario: ProgressBar
    private lateinit var scrollViewHorario: ScrollView
    private lateinit var layoutDias: LinearLayout
    private lateinit var layoutSinClases: LinearLayout
    private lateinit var fabRefrescar: FloatingActionButton

    companion object {
        private const val TAG = "HorarioDocenteActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horario_docente)

        initializeViews()
        setupListeners()

        // Recuperar ID del profesor
        idProfesor = intent.getIntExtra("id_profesor", -1)

        if (idProfesor == -1) {
            Toast.makeText(this, "Error: ID de profesor no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Configurar ActionBar
        supportActionBar?.apply {
            title = "Mi Horario"
            setDisplayHomeAsUpEnabled(true)
        }

        // Cargar horario del profesor
        cargarHorarioCompleto()
    }

    private fun initializeViews() {
        txtNombreProfesor = findViewById(R.id.txtNombreProfesor)
        txtTotalClases = findViewById(R.id.txtTotalClases)
        progressBarHorario = findViewById(R.id.progressBarHorario)
        scrollViewHorario = findViewById(R.id.scrollViewHorario)
        layoutDias = findViewById(R.id.layoutDias)
        layoutSinClases = findViewById(R.id.layoutSinClases)
        fabRefrescar = findViewById(R.id.fabRefrescar)
    }

    private fun setupListeners() {
        fabRefrescar.setOnClickListener {
            cargarHorarioCompleto()
        }
    }

    private fun cargarHorarioCompleto() {
        Log.d(TAG, "Cargando horario para profesor ID: $idProfesor")
        mostrarEstadoCarga(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getTodasClasesProfesor(idProfesor)

                Log.d(TAG, "Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val responseData = response.body()
                    Log.d(TAG, "Response body: $responseData")

                    if (responseData?.success == true) {
                        val data = responseData.data
                        mostrarHorarioCompleto(data)
                    } else {
                        mostrarError("No se pudieron obtener las clases")
                    }
                } else {
                    val errorMessage = try {
                        response.errorBody()?.string() ?: "Error desconocido"
                    } catch (e: Exception) {
                        "Error del servidor: ${response.code()}"
                    }
                    Log.e(TAG, "Error response: $errorMessage")
                    mostrarError("Error del servidor: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading horario", e)
                mostrarError("Error de conexión: ${e.message}")
            }
        }
    }

    private fun mostrarHorarioCompleto(data: ClasesPorDiaData) {
        Log.d(TAG, "Mostrando horario: ${data.total_clases} clases")
        mostrarEstadoCarga(false)

        // Actualizar header
        txtNombreProfesor.text = data.profesor
        val totalText = if (data.total_clases == 1) {
            "1 clase programada"
        } else {
            "${data.total_clases} clases programadas"
        }
        txtTotalClases.text = totalText

        // Limpiar layout de días
        layoutDias.removeAllViews()

        if (data.clases_por_dia.isEmpty()) {
            mostrarSinClases()
            return
        }

        // Orden de días de la semana (según tu endpoint)
        val ordenDias = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

        var totalDiasMostrados = 0

        // Mostrar días en orden
        for (dia in ordenDias) {
            val clasesDelDia = data.clases_por_dia[dia]
            if (!clasesDelDia.isNullOrEmpty()) {
                Log.d(TAG, "Agregando día $dia con ${clasesDelDia.size} clases")
                agregarDiaAlLayout(dia, clasesDelDia)
                totalDiasMostrados++
            }
        }

        if (totalDiasMostrados > 0) {
            // Mostrar contenido
            scrollViewHorario.visibility = View.VISIBLE
            layoutSinClases.visibility = View.GONE
        } else {
            mostrarSinClases()
        }
    }

    private fun agregarDiaAlLayout(nombreDia: String, clases: List<ClaseCompleta>) {
        // Inflar el layout del día
        val diaView = layoutInflater.inflate(R.layout.item_dia_horario, layoutDias, false)

        // Configurar header del día
        val txtNombreDia = diaView.findViewById<TextView>(R.id.txtNombreDia)
        val txtConteoClases = diaView.findViewById<TextView>(R.id.txtConteoClases)
        val recyclerViewClasesDia = diaView.findViewById<RecyclerView>(R.id.recyclerViewClasesDia)

        txtNombreDia.text = nombreDia
        val conteoText = if (clases.size == 1) "1 clase" else "${clases.size} clases"
        txtConteoClases.text = conteoText

        // Configurar RecyclerView del día
        val adapter = ClaseHorarioAdapter(clases) { claseSeleccionada ->
            // Cuando el usuario selecciona una clase
            seleccionarClase(claseSeleccionada)
        }

        recyclerViewClasesDia.apply {
            layoutManager = LinearLayoutManager(this@HorarioDocenteActivity)
            this.adapter = adapter
            isNestedScrollingEnabled = false
        }

        // Agregar el día al layout principal
        layoutDias.addView(diaView)
    }

    private fun seleccionarClase(clase: ClaseCompleta) {
        Log.d(TAG, "Clase seleccionada: ${clase.nombre_clase} (ID: ${clase.id_clase})")

        // Mostrar confirmación
        Toast.makeText(this,
            "Clase seleccionada: ${clase.nombre_clase}",
            Toast.LENGTH_SHORT).show()

        // Retornar el ID de la clase seleccionada con información adicional
        val resultIntent = Intent().apply {
            putExtra("id_clase_seleccionada", clase.id_clase)
            putExtra("nombre_clase", clase.nombre_clase)
            putExtra("nrc", clase.nrc)
            putExtra("aula", clase.aula)
            putExtra("materia", clase.materia)
            putExtra("materia_clave", clase.materia_clave)  // ✅ Agregado
            putExtra("grupo", clase.grupo)
            putExtra("turno", clase.turno)                   // ✅ Agregado
            putExtra("nivel", clase.nivel)                   // ✅ Agregado
            putExtra("dia", clase.dia)
            putExtra("hora_inicio", clase.hora_inicio)
            putExtra("hora_fin", clase.hora_fin)
            putExtra("nombre_profesor", clase.nombre_profesor) // ✅ Agregado
            putExtra("id_grupo", clase.id_grupo)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun mostrarEstadoCarga(mostrar: Boolean) {
        if (mostrar) {
            progressBarHorario.visibility = View.VISIBLE
            scrollViewHorario.visibility = View.GONE
            layoutSinClases.visibility = View.GONE
        } else {
            progressBarHorario.visibility = View.GONE
        }
    }

    private fun mostrarSinClases() {
        scrollViewHorario.visibility = View.GONE
        layoutSinClases.visibility = View.VISIBLE
    }

    private fun mostrarError(mensaje: String) {
        Log.e(TAG, "Error mostrado: $mensaje")
        mostrarEstadoCarga(false)
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()

        // Si hay error, mostrar vista sin clases
        mostrarSinClases()
    }

    // Manejar botón de retroceso del ActionBar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        // Si no se seleccionó ninguna clase, retornar CANCELED
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }
}

