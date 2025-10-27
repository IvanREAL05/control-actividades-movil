package com.example.control_actividades

import android.content.Intent
import android.os.Bundle
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AlertDialog
import android.text.style.BackgroundColorSpan
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.view.View
import android.view.Menu

class DocenteHomeActivity : AppCompatActivity() {

    private lateinit var tvInfoClase: TextView
    private lateinit var tvActividadesRecientes: TextView
    private lateinit var tvObservaciones: TextView  // 🆕 Para mostrar observaciones

    private var idProfesor: Int = -1
    private var idClaseActual: Int = -1
    private var idClaseSeleccionada: Int = -1
    private var claseSeleccionadaInfo: ClaseInfo? = null

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshIntervalMillis = 60_000L // 1 minuto

    private val refrescarClaseRunnable = object : Runnable {
        override fun run() {
            obtenerClaseActual()
            obtenerActividadesRecientes()
            handler.postDelayed(this, refreshIntervalMillis)
        }
    }

    private var accionPendiente: ((Int) -> Unit)? = null

    companion object {
        const val REQUEST_SELECCION_CLASE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_docente_home)

        // Configurar Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Inicializar vistas
        tvInfoClase = findViewById(R.id.tvInfoClase)
        tvActividadesRecientes = findViewById(R.id.tvActividadesRecientes)
        tvObservaciones = findViewById(R.id.tvObservaciones)

        // Recuperar id del profesor
        val sharedPref = getSharedPreferences("docentePrefs", MODE_PRIVATE)
        idProfesor = sharedPref.getInt("id_profesor", -1)

        if (idProfesor == -1) {
            Toast.makeText(this, "No se encontró sesión, vuelve a iniciar", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, DocenteLoginActivity::class.java))
            finish()
            return
        }

        // Iniciar actualización automática
        handler.post(refrescarClaseRunnable)
        obtenerClaseActual()
        obtenerActividadesRecientes()

        // Configurar visibilidad de botones especiales
        configurarBotonesEspeciales()

        // Configurar listeners de botones
        configurarListeners()

        // Manejo del botón físico "atrás" con confirmación
        onBackPressedDispatcher.addCallback(this) {
            mostrarDialogoConfirmacionSalir()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_docente_home, menu)
        return true
    }

    // Manejar clics en el menú
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Opciones del menú superior
            R.id.action_acceder_dashboard -> {
                val intent = Intent(this, ScanLoginDashboardActivity::class.java)
                intent.putExtra("id_profesor", idProfesor) // ✅ Enviar explícitamente
                startActivity(intent)
                true
            }
            R.id.action_cerrar_sesion -> {
                confirmarCerrarSesion()
                true
            }
            // Botón "atrás" del toolbar (si lo tienes habilitado)
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun mostrarPerfilDocente() {
        val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
        val nombreProfesor = sharedPref.getString("nombre_profesor", "Docente")
        val idProfesor = sharedPref.getInt("id_profesor", -1)

        AlertDialog.Builder(this)
            .setTitle("👤 Mi Perfil")
            .setMessage("Nombre: $nombreProfesor\nID: $idProfesor")
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun confirmarCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                // Limpiar SharedPreferences
                val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
                sharedPref.edit().clear().apply()

                // Volver al login
                val intent = Intent(this, DocenteLoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }


    private fun configurarBotonesEspeciales() {
        val btnRegistrarJustificante = findViewById<Button>(R.id.btnRegistrarJustificante)
        btnRegistrarJustificante.isEnabled = false
        btnRegistrarJustificante.alpha = 0.5f
    }

    private fun configurarListeners() {
        // Botón principal
        findViewById<Button>(R.id.btnRegistrarActividad).setOnClickListener {
            manejarAccionClase { idClase ->
                val intent = Intent(this, ActividadesActivity::class.java)
                intent.putExtra("id_clase", idClase)
                startActivity(intent)
            }
        }

        findViewById<Button>(R.id.btnRegistrarAsistencia).setOnClickListener {
            manejarAccionClase { idClase ->
                val intent = Intent(this, EscanerQRActivity::class.java)
                intent.putExtra("id_clase", idClase)
                startActivity(intent)
            }
        }

        findViewById<Button>(R.id.btnRegistrarJustificante).setOnClickListener {
            manejarAccionClase { idClase ->
                val intent = Intent(this, JustificanteActivity::class.java)
                intent.putExtra("id_clase", idClase)
                startActivity(intent)
            }
        }

        findViewById<Button>(R.id.btnVerLista).setOnClickListener {
            manejarAccionClase { idClase ->
                val intent = Intent(this, ListaGrupoActivity::class.java)
                intent.putExtra("id_clase", idClase)
                intent.putExtra("id_profesor", idProfesor)
                startActivity(intent)
            }
        }

        findViewById<Button>(R.id.btnVerEstadisticas).setOnClickListener {
            manejarAccionClase { idClase ->
                val intent = Intent(this, EstadisticasGrupoActivity::class.java)
                intent.putExtra("id_clase", idClase)
                intent.putExtra("id_profesor", idProfesor)
                startActivity(intent)
            }
        }

        findViewById<Button>(R.id.btnBitacora).setOnClickListener {
            if (idClaseActual != -1 && idClaseSeleccionada != -1) {
                mostrarOpcionesBitacora()
            } else {
                abrirHorarioParaSeleccion(null)
            }
        }
    }

    private fun mostrarOpcionesBitacora() {
        AlertDialog.Builder(this)
            .setTitle("Gestión de Bitácora")
            .setMessage("Tienes una clase seleccionada manualmente y otra activa. ¿Qué deseas hacer?")
            .setPositiveButton("Elegir Nueva Clase") { _, _ ->
                abrirHorarioParaSeleccion(null)
            }
            .setNeutralButton("Usar Clase Activa") { _, _ ->
                limpiarSeleccionManual()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun abrirHorarioParaSeleccion(accion: ((Int) -> Unit)?) {
        val intent = Intent(this, HorarioDocenteActivity::class.java)
        intent.putExtra("id_profesor", idProfesor)
        accionPendiente = accion
        startActivityForResult(intent, REQUEST_SELECCION_CLASE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SELECCION_CLASE && resultCode == RESULT_OK) {
            val idClase = data?.getIntExtra("id_clase_seleccionada", -1) ?: -1

            if (idClase != -1) {
                val claseInfo = crearClaseInfoDesdeExtras(data)
                idClaseSeleccionada = idClase
                claseSeleccionadaInfo = claseInfo

                actualizarInfoClaseSeleccionada()
                actualizarEstadoBotones()
                obtenerActividadesRecientes()

                accionPendiente?.let { accion ->
                    accion(idClase)
                    accionPendiente = null
                }

                Toast.makeText(this, "Clase seleccionada: ${claseInfo?.materia ?: "N/A"}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No se seleccionó ninguna clase", Toast.LENGTH_SHORT).show()
                accionPendiente = null
            }
        }
    }

    private fun crearClaseInfoDesdeExtras(data: Intent?): ClaseInfo? {
        if (data == null) return null
        Log.d("DocenteHome", "📦 ID_GRUPO RECIBIDO: ${data.getIntExtra("id_grupo", -1)}")
        return try {
            ClaseInfo(
                id_clase = data.getIntExtra("id_clase_seleccionada", -1),
                nombre_clase = data.getStringExtra("nombre_clase"),
                nrc = data.getStringExtra("nrc"),
                aula = data.getStringExtra("aula"),
                materia = data.getStringExtra("materia"),
                materia_clave = data.getStringExtra("materia_clave"),
                grupo = data.getStringExtra("grupo"),
                turno = data.getStringExtra("turno"),
                nivel = data.getStringExtra("nivel"),
                hora_inicio = data.getStringExtra("hora_inicio"),
                hora_fin = data.getStringExtra("hora_fin"),
                dia = data.getStringExtra("dia"),
                nombre_profesor = data.getStringExtra("nombre_profesor"),
                id_grupo = data.getIntExtra("id_grupo", -1)
            )
        } catch (e: Exception) {
            Log.e("DocenteHome", "Error creando ClaseInfo: ${e.message}")
            null
        }
    }

    private fun manejarAccionClase(accion: (Int) -> Unit) {
        when {
            idClaseSeleccionada != -1 -> accion(idClaseSeleccionada)
            idClaseActual != -1 -> accion(idClaseActual)
            else -> abrirHorarioParaSeleccion(accion)
        }
    }

    private fun limpiarSeleccionManual() {
        idClaseSeleccionada = -1
        claseSeleccionadaInfo = null
        obtenerClaseActual()
        obtenerActividadesRecientes()
        actualizarEstadoBotones()

        if (idClaseActual != -1) {
            Toast.makeText(this, "Ahora usando clase activa", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Selección manual limpiada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun obtenerActividadesRecientes() {
        val idClase = if (idClaseSeleccionada != -1) idClaseSeleccionada else idClaseActual

        if (idClase == -1) {
            tvActividadesRecientes.text = "Selecciona una clase para ver las actividades"
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.obtenerActividadesRecientes(idClase)

                if (response.isSuccessful && response.body() != null) {
                    val actividades = response.body()!!
                    if (actividades.isNotEmpty()) {
                        tvActividadesRecientes.text = "Última actividad: ${actividades[0].titulo}"
                    } else {
                        tvActividadesRecientes.text = "No hay actividades registradas hoy"
                    }
                } else {
                    tvActividadesRecientes.text = "Error al cargar actividades recientes"
                }

            } catch (e: Exception) {
                tvActividadesRecientes.text = "Error al cargar actividades recientes"
                Log.e("DocenteHome", "Error al obtener actividades recientes", e)
            }
        }
    }

    private fun actualizarEstadoBotones() {
        val btnElegirClase = findViewById<Button>(R.id.btnBitacora)
        btnElegirClase.isEnabled = true
        btnElegirClase.alpha = 1.0f

        when {
            idClaseSeleccionada != -1 -> {
                if (idClaseActual != -1) {
                    btnElegirClase.text = "📆 Cambiar Bitácora (Manual activa)"
                } else {
                    btnElegirClase.text = "📆 Cambiar Bitácora (Manual)"
                }
            }
            idClaseActual != -1 -> {
                btnElegirClase.text = "📆 Elegir Otra Bitácora"
            }
            else -> {
                btnElegirClase.text = "📆 Elegir Bitácora"
            }
        }
    }

    // 🆕 Actualiza la UI con información de clase seleccionada manualmente
    private fun actualizarInfoClaseSeleccionada() {
        claseSeleccionadaInfo?.let { clase ->
            val builder = SpannableStringBuilder()

            // Nombre profesor en negrita
            val profesorStart = builder.length
            builder.append("Profesor: ${clase.nombre_profesor ?: "N/A"}\n")
            builder.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                profesorStart,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            builder.append("Materia: ${clase.materia ?: "N/A"}\n")
            builder.append("NRC: ${clase.nrc ?: "N/A"}\n")
            builder.append("Grupo: ${clase.grupo ?: "N/A"}\n")

            if (clase.hora_inicio != null && clase.hora_fin != null) {
                val horarioStart = builder.length
                builder.append("Horario: ${clase.hora_inicio} - ${clase.hora_fin}\n")
                val guinda = android.graphics.Color.parseColor("#800000")
                builder.setSpan(
                    ForegroundColorSpan(guinda),
                    horarioStart,
                    builder.length - 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.setSpan(
                    StyleSpan(android.graphics.Typeface.BOLD),
                    horarioStart,
                    builder.length - 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            if (clase.dia != null) {
                builder.append("Día: ${clase.dia}\n")
            }

            // Indicador de selección manual
            val indicadorStart = builder.length
            if (idClaseActual != -1) {
                builder.append("📋 Clase MANUAL (tiene prioridad sobre la activa)")
                builder.setSpan(
                    ForegroundColorSpan(android.graphics.Color.parseColor("#FF9800")),
                    indicadorStart,
                    builder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else {
                builder.append("📋 Clase seleccionada manualmente")
                builder.setSpan(
                    ForegroundColorSpan(android.graphics.Color.parseColor("#1976D2")),
                    indicadorStart,
                    builder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            builder.setSpan(
                StyleSpan(android.graphics.Typeface.ITALIC),
                indicadorStart,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            tvInfoClase.text = builder

            // Obtener observaciones del grupo
            clase.id_grupo?.let { idGrupo ->
                obtenerObservacionesGrupo(idGrupo)
            } ?: run {
                tvObservaciones.visibility = View.GONE
            }
        }
    }

    // 🆕 Obtiene y muestra observaciones del grupo
    private fun obtenerObservacionesGrupo(idGrupo: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.obtenerObservacionesPorGrupo(idGrupo)

                if (response.isSuccessful && response.body() != null) {
                    val observaciones = response.body()!!
                    if (observaciones.total_observaciones > 0) {
                        tvObservaciones.visibility = View.VISIBLE
                        tvObservaciones.text = "⚠️ ${observaciones.total_observaciones} observación${if (observaciones.total_observaciones > 1) "es" else ""}"
                    } else {
                        tvObservaciones.visibility = View.GONE
                    }
                } else {
                    tvObservaciones.visibility = View.GONE
                }
            } catch (e: Exception) {
                tvObservaciones.visibility = View.GONE
                Log.e("DocenteHome", "Error al obtener observaciones", e)
            }
        }
    }

    // 🆕 Obtiene clase actual del backend
    private fun obtenerClaseActual() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getClaseActual(idProfesor)

                if (response.isSuccessful && response.body() != null) {
                    val claseResponse = response.body()!!
                    val claseActual = claseResponse.data.clase_actual

                    if (claseActual != null) {
                        idClaseActual = claseActual.id_clase

                        // Solo actualizar UI si NO hay selección manual
                        if (idClaseSeleccionada == -1) {
                            mostrarInfoClaseActiva(claseResponse)
                        }
                    } else {
                        idClaseActual = -1
                        if (idClaseSeleccionada == -1) {
                            mostrarSinClase(claseResponse)
                        }
                    }

                    actualizarEstadoBotones()
                } else {
                    idClaseActual = -1
                    if (idClaseSeleccionada == -1) {
                        tvInfoClase.text = "Sin clase actual"
                    }
                    actualizarEstadoBotones()
                }
            } catch (e: Exception) {
                idClaseActual = -1
                actualizarEstadoBotones()
                if (idClaseSeleccionada == -1) {
                    Log.e("DocenteHome", "Error al obtener clase actual", e)
                }
            }
        }
    }

    // 🆕 Muestra información de clase activa
    private fun mostrarInfoClaseActiva(response: ClaseActualResponse) {
        val data = response.data
        val clase = data.clase_actual

        val builder = SpannableStringBuilder()

        // Profesor (usa el del nivel data)
        val profesorStart = builder.length
        builder.append("Profesor: ${data.nombre_profesor ?: "N/A"}\n")
        builder.setSpan(
            StyleSpan(android.graphics.Typeface.BOLD),
            profesorStart,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Datos de la clase (dentro de clase_actual)
        builder.append("Materia: ${clase?.materia ?: clase?.nombre_clase ?: "N/A"}\n")
        builder.append("NRC: ${clase?.nrc ?: "N/A"}\n")
        builder.append("Grupo: ${clase?.grupo ?: "N/A"}\n")
        builder.append("Aula: ${clase?.aula ?: "N/A"}\n")

        if (clase?.hora_inicio != null && clase.hora_fin != null) {
            val horarioStart = builder.length
            builder.append("Horario: ${clase.hora_inicio} - ${clase.hora_fin}\n")
            val guinda = android.graphics.Color.parseColor("#800000")
            builder.setSpan(
                ForegroundColorSpan(guinda),
                horarioStart,
                builder.length - 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                horarioStart,
                builder.length - 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else {
            builder.append("Horario: No disponible\n")
        }

        // Indicador de clase activa
        val activaStart = builder.length
        builder.append("🟢 CLASE ACTIVA")
        builder.setSpan(
            BackgroundColorSpan(android.graphics.Color.parseColor("#195e1b")),
            activaStart,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.setSpan(
            ForegroundColorSpan(android.graphics.Color.WHITE),
            activaStart,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.setSpan(
            StyleSpan(android.graphics.Typeface.BOLD),
            activaStart,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvInfoClase.text = builder

        // Observaciones (solo si existe id_grupo)
        clase?.id_grupo?.let { idGrupo ->
            obtenerObservacionesGrupo(idGrupo)
        } ?: run {
            tvObservaciones.visibility = View.GONE
        }
    }

    // 🆕 Muestra información cuando no hay clase
    private fun mostrarSinClase(claseResponse: ClaseActualResponse?) {
        val builder = SpannableStringBuilder()

        val profesorStart = builder.length
        builder.append("Profesor: ${claseResponse?.data?.nombre_profesor ?: "N/A"}\n")
        builder.setSpan(
            StyleSpan(android.graphics.Typeface.BOLD),
            profesorStart,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        builder.append("Sin clase actual\n")
        builder.append("Selecciona una clase para comenzar")

        tvInfoClase.text = builder
        tvObservaciones.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refrescarClaseRunnable)
    }

    private fun mostrarDialogoConfirmacionSalir() {
        AlertDialog.Builder(this)
            .setTitle("¿Salir?")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                val prefs = getSharedPreferences("docentePrefs", MODE_PRIVATE)
                prefs.edit().clear().apply()

                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}