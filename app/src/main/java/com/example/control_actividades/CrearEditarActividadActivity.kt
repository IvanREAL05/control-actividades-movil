package com.example.control_actividades

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import android.view.View

class CrearEditarActividadActivity : AppCompatActivity() {

    private lateinit var etTitulo: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var tvFechaEntrega: TextView
    private lateinit var btnSeleccionarFecha: Button
    private lateinit var tvHoraEntrega: TextView
    private lateinit var btnSeleccionarHora: Button
    private lateinit var etValorMaximo: EditText
    private lateinit var btnGuardarActividad: Button
    private lateinit var spinnerTipoActividad: Spinner
    private var tipoSeleccionado: String = ""


    private var idClaseSeleccionado: Int = -1 // Se recibe por Intent
    private var modo: String = "crear" // o "editar"
    private var actividadEditando: Actividad? = null

    private var fechaEntregaSeleccionada: String = ""
    private var horaEntregaSeleccionada: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_editar_actividad)

        etTitulo = findViewById(R.id.etTitulo)
        etDescripcion = findViewById(R.id.etDescripcion)
        tvFechaEntrega = findViewById(R.id.tvFechaEntrega)
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha)
        tvHoraEntrega = findViewById(R.id.tvHoraEntrega)
        btnSeleccionarHora = findViewById(R.id.btnSeleccionarHora)
        etValorMaximo = findViewById(R.id.etValorMaximo)
        btnGuardarActividad = findViewById(R.id.btnGuardarActividad)
        spinnerTipoActividad = findViewById(R.id.spinnerTipoActividad)
        val tiposArray = resources.getStringArray(R.array.tipos_actividad)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            tiposArray.map { it.replaceFirstChar { c -> c.uppercase() } } // solo visual
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoActividad.adapter = adapter

        spinnerTipoActividad.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                tipoSeleccionado = tiposArray[position] // minúscula para backend
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                tipoSeleccionado = ""
            }
        }

        spinnerTipoActividad.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                tipoSeleccionado = parent?.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                tipoSeleccionado = ""
            }
        }

        // SOLO esto para el cursor:
        listOf(etTitulo, etDescripcion, etValorMaximo).forEach { editText ->
            editText.setTextColor(android.graphics.Color.BLACK)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                editText.textCursorDrawable = null
            }
        }
        // --- Configurar Toolbar ---
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Nueva Actividad"
            setDisplayHomeAsUpEnabled(true) // habilita flecha back
            setDisplayShowHomeEnabled(true)
        }
        toolbar.setTitleTextColor(android.graphics.Color.WHITE)
        // Manejar click de flecha de back
        toolbar.setNavigationOnClickListener {
            onBackPressed() // vuelve a la activity anterior
        }



        modo = intent.getStringExtra("modo") ?: "crear"
        idClaseSeleccionado = intent.getIntExtra("id_clase", -1)

        if (modo == "editar") {
            actividadEditando = intent.getParcelableExtra<Actividad>("actividad")
            cargarDatosActividad()
        }

        btnSeleccionarFecha.setOnClickListener { mostrarDatePicker() }
        btnSeleccionarHora.setOnClickListener { mostrarTimePicker() }
        btnGuardarActividad.setOnClickListener { guardarActividad() }
    }

    private fun cargarDatosActividad() {
        actividadEditando?.let { act ->
            etTitulo.setText(act.titulo)
            etDescripcion.setText(act.descripcion ?: "")

            val partesFecha = act.fecha_entrega.split("T")
            fechaEntregaSeleccionada = partesFecha[0]
            tvFechaEntrega.text = fechaEntregaSeleccionada

            horaEntregaSeleccionada = act.hora_entrega ?: "23:59:59"
            tvHoraEntrega.text = horaEntregaSeleccionada

            etValorMaximo.setText(act.valor_maximo?.toString() ?: "10")
        }
    }

    private fun mostrarDatePicker() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(
            this,
            android.R.style.Theme_Material_Light_Dialog,
            { _, y, m, d ->
                fechaEntregaSeleccionada = String.format("%04d-%02d-%02d", y, m + 1, d)
                tvFechaEntrega.text = fechaEntregaSeleccionada
            },
            year, month, day
        )

        dialog.setOnShowListener {
            dialog.getButton(DatePickerDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(android.graphics.Color.parseColor("#1976D2")) // Azul principal
                text = "Seleccionar"
                textSize = 16f
                isAllCaps = false
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            dialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.apply {
                setTextColor(android.graphics.Color.parseColor("#42A5F5")) // Azul más claro
                text = "Cancelar"
                textSize = 16f
                isAllCaps = false
            }
        }

        dialog.show()
    }

    private fun mostrarTimePicker() {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        val timeDialog = TimePickerDialog(
            this,
            android.R.style.Theme_Material_Light_Dialog,
            { _, h, m ->
                horaEntregaSeleccionada = String.format("%02d:%02d:00", h, m)
                tvHoraEntrega.text = horaEntregaSeleccionada
            },
            hour, minute,
            true
        )

        timeDialog.setOnShowListener {
            timeDialog.getButton(TimePickerDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(android.graphics.Color.parseColor("#1976D2")) // Azul principal
                text = "Confirmar"
                textSize = 16f
                isAllCaps = false
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            timeDialog.getButton(TimePickerDialog.BUTTON_NEGATIVE)?.apply {
                setTextColor(android.graphics.Color.parseColor("#42A5F5")) // Azul más claro
                text = "Cancelar"
                textSize = 16f
                isAllCaps = false
            }
        }

        timeDialog.show()
    }

    private fun guardarActividad() {
        val titulo = etTitulo.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim().takeIf { it.isNotEmpty() }
        val valorMaximoStr = etValorMaximo.text.toString().trim()
        val valorMaximo = valorMaximoStr.toIntOrNull()

        if (titulo.isEmpty()) {
            Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }
        if (fechaEntregaSeleccionada.isEmpty()) {
            Toast.makeText(this, "Selecciona una fecha de entrega", Toast.LENGTH_SHORT).show()
            return
        }
        // Si no seleccionó hora, poner 23:59:59 por defecto
        if (horaEntregaSeleccionada.isEmpty()) {
            horaEntregaSeleccionada = "23:59:59"
        }
        if (valorMaximo == null || valorMaximo !in 0..10) {
            Toast.makeText(this, "Ingresa un valor máximo válido entre 0 y 10", Toast.LENGTH_SHORT).show()
            return
        }
        if (idClaseSeleccionado == -1) {
            Toast.makeText(this, "No se recibió la clase para asignar la actividad", Toast.LENGTH_SHORT).show()
            return
        }

        // Deshabilitar botón y opcional mostrar progress
        btnGuardarActividad.isEnabled = false
        // progressBar.visibility = View.VISIBLE

        val actividadRequest = ActividadRequest(
            titulo = titulo,
            descripcion = descripcion ?: "",
            fecha_entrega = fechaEntregaSeleccionada,
            hora_entrega = horaEntregaSeleccionada ?: "23:59:59",
            id_clase = idClaseSeleccionado,
            valor_maximo = valorMaximo,
            tipo_actividad = tipoSeleccionado
        )

        if (modo == "crear") {
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.crearActividad(actividadRequest)

                    btnGuardarActividad.isEnabled = true
                    // progressBar.visibility = View.GONE

                    Toast.makeText(this@CrearEditarActividadActivity, "✔️ Actividad creada con ID ${response.id_actividad}", Toast.LENGTH_SHORT).show()
                    finish()

                } catch (e: HttpException) {
                    btnGuardarActividad.isEnabled = true
                    // progressBar.visibility = View.GONE
                    val errorMsg = e.response()?.errorBody()?.string() ?: "Error HTTP ${e.code()}"
                    Toast.makeText(this@CrearEditarActividadActivity, "❌ Error al crear: $errorMsg", Toast.LENGTH_LONG).show()

                } catch (e: Exception) {
                    btnGuardarActividad.isEnabled = true
                    // progressBar.visibility = View.GONE
                    Toast.makeText(this@CrearEditarActividadActivity, "⚠️ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (modo == "editar" && actividadEditando != null) {
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.editarActividad(
                        actividadEditando!!.id_actividad,
                        actividadRequest
                    )

                    btnGuardarActividad.isEnabled = true
                    Toast.makeText(this@CrearEditarActividadActivity, "✔️ ${response.mensaje}", Toast.LENGTH_SHORT).show()
                    finish()

                } catch (e: HttpException) {
                    btnGuardarActividad.isEnabled = true
                    val errorMsg = e.response()?.errorBody()?.string() ?: "Error HTTP ${e.code()}"
                    Toast.makeText(this@CrearEditarActividadActivity, "❌ Error al editar: $errorMsg", Toast.LENGTH_LONG).show()

                } catch (e: Exception) {
                    btnGuardarActividad.isEnabled = true
                    Toast.makeText(this@CrearEditarActividadActivity, "⚠️ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}