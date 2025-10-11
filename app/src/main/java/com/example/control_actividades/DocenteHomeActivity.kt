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
import android.graphics.Color
import android.text.style.BackgroundColorSpan
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DocenteHomeActivity : AppCompatActivity() {

    private lateinit var tvInfoClase: TextView
    private lateinit var tvActividadesRecientes: TextView
    private var idProfesor: Int = -1
    private var idClaseActual: Int = -1  // Para clase activa del backend
    private var idClaseSeleccionada: Int = -1  // Para clase seleccionada manualmente
    private var claseSeleccionadaInfo: ClaseInfo? = null  // Info de la clase seleccionada

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshIntervalMillis = 60_000L // 1 minuto

    private val refrescarClaseRunnable = object : Runnable {
        override fun run() {
            obtenerClaseActual()
            obtenerActividadesRecientes()
            handler.postDelayed(this, refreshIntervalMillis)
        }
    }

    // Variable para almacenar la acción pendiente
    private var accionPendiente: ((Int) -> Unit)? = null

    companion object {
        const val REQUEST_SELECCION_CLASE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_docente_home)

        // Inicializar vistas
        tvInfoClase = findViewById(R.id.tvInfoClase)
        tvActividadesRecientes = findViewById(R.id.tvActividadesRecientes)

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

    /**
     * Configura la visibilidad de botones para usuarios especiales
     */
    private fun configurarBotonesEspeciales() {
        val idUsuarioEspecial = 5 // usuario especial

        // Deshabilitar justificantes
        val btnRegistrarJustificante = findViewById<Button>(R.id.btnRegistrarJustificante)
        btnRegistrarJustificante.isEnabled = false
        btnRegistrarJustificante.alpha = 0.5f
    }

    /**
     * Configura todos los listeners de los botones
     */
    private fun configurarListeners() {
        // BOTÓN PRINCIPAL: Registrar Actividad
        findViewById<Button>(R.id.btnRegistrarActividad).setOnClickListener {
            Log.d("ID_CLASE_DEBUG", "Botón principal: Registrar Actividad presionado")
            manejarAccionClase { idClase ->
                val intent = Intent(this, ActividadesActivity::class.java)
                intent.putExtra("id_clase", idClase)
                startActivity(intent)
            }
        }

        // BOTONES SECUNDARIOS
        findViewById<Button>(R.id.btnRegistrarAsistencia).setOnClickListener {
            Log.d("ID_CLASE_DEBUG", "Estado antes de abrir escáner: activa=$idClaseActual, seleccionada=$idClaseSeleccionada")
            manejarAccionClase { idClase ->
                Log.d("ID_CLASE_DEBUG", "Enviando al escáner ID: $idClase")
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
                Log.d("ID_DEBUG", "🔸 Enviando desde DocenteHome: idClase=$idClase, idProfesor=$idProfesor")
                val intent = Intent(this, ListaGrupoActivity::class.java)
                intent.putExtra("id_clase", idClase)
                intent.putExtra("id_profesor", idProfesor)
                startActivity(intent)
            }
        }

        findViewById<Button>(R.id.btnVerEstadisticas).setOnClickListener {
            manejarAccionClase { idClase ->
                Log.d("DocenteHome", "Enviando a EstadisticasGrupoActivity:")
                Log.d("DocenteHome", "  - id_clase: $idClase")
                Log.d("DocenteHome", "  - id_profesor: $idProfesor")

                val intent = Intent(this, EstadisticasGrupoActivity::class.java)
                intent.putExtra("id_clase", idClase)
                intent.putExtra("id_profesor", idProfesor)
                startActivity(intent)
            }
        }

        // Botón para cambiar bitácora manualmente
        findViewById<Button>(R.id.btnBitacora).setOnClickListener {
            // Mostrar opciones si hay tanto clase activa como seleccionada
            if (idClaseActual != -1 && idClaseSeleccionada != -1) {
                mostrarOpcionesBitacora()
            } else {
                // Ir directamente a HorarioDocenteActivity
                abrirHorarioParaSeleccion(null)
            }
        }
    }

    /**
     * Muestra opciones cuando hay clase activa Y seleccionada manualmente
     */
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

    /**
     * Abre HorarioDocenteActivity para seleccionar clase
     */
    private fun abrirHorarioParaSeleccion(accion: ((Int) -> Unit)?) {
        val intent = Intent(this, HorarioDocenteActivity::class.java)
        intent.putExtra("id_profesor", idProfesor)

        // Guardar la acción pendiente si existe
        accionPendiente = accion

        startActivityForResult(intent, REQUEST_SELECCION_CLASE)
    }

    /**
     * Maneja el resultado de la selección de clase desde HorarioDocenteActivity
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SELECCION_CLASE && resultCode == RESULT_OK) {
            // Usar el nombre correcto del extra que envía HorarioDocenteActivity
            val idClase = data?.getIntExtra("id_clase_seleccionada", -1) ?: -1

            if (idClase != -1) {
                // Crear ClaseInfo desde los extras recibidos
                val claseInfo = crearClaseInfoDesdeExtras(data)

                // Actualizar selección manual
                idClaseSeleccionada = idClase
                claseSeleccionadaInfo = claseInfo

                Log.d("ID_CLASE_DEBUG", "Clase seleccionada desde HorarioActivity: ID=$idClase")
                Log.d("ID_CLASE_DEBUG", "Clase: ${claseInfo?.materia} - ${claseInfo?.grupo}")

                // Actualizar UI
                actualizarInfoClaseSeleccionada()
                actualizarEstadoBotones()
                obtenerActividadesRecientes()

                // Ejecutar acción pendiente si existe
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

    /**
     * Crea un objeto ClaseInfo desde los extras del Intent
     */
    private fun crearClaseInfoDesdeExtras(data: Intent?): ClaseInfo? {
        if (data == null) return null

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
                nombre_profesor = data.getStringExtra("nombre_profesor")
            )
        } catch (e: Exception) {
            Log.e("ID_CLASE_DEBUG", "Error creando ClaseInfo: ${e.message}")
            null
        }
    }

    /**
     * LÓGICA PRINCIPAL: Maneja acciones que requieren una clase
     * Prioridad: 1) Manual 2) Activa 3) Seleccionar nueva
     */
    private fun manejarAccionClase(accion: (Int) -> Unit) {
        Log.d("ID_CLASE_DEBUG", "=== INICIANDO MANEJO DE ACCIÓN ===")
        Log.d("ID_CLASE_DEBUG", "idClaseActual: $idClaseActual")
        Log.d("ID_CLASE_DEBUG", "idClaseSeleccionada: $idClaseSeleccionada")

        when {
            // PRIORIDAD 1: Si hay selección manual, SIEMPRE usarla
            idClaseSeleccionada != -1 -> {
                Log.d("ID_CLASE_DEBUG", "✅ USANDO clase seleccionada manualmente: $idClaseSeleccionada")
                accion(idClaseSeleccionada)
            }
            // PRIORIDAD 2: Si NO hay manual pero SÍ hay activa, usar la activa
            idClaseActual != -1 -> {
                Log.d("ID_CLASE_DEBUG", "✅ USANDO clase activa (no hay manual): $idClaseActual")
                accion(idClaseActual)
            }
            // PRIORIDAD 3: Si no hay ninguna, ir a HorarioDocenteActivity
            else -> {
                Log.d("ID_CLASE_DEBUG", "❌ NO hay clases disponibles, abriendo HorarioDocenteActivity")
                abrirHorarioParaSeleccion(accion)
            }
        }
    }

    /**
     * Limpia la selección manual y vuelve a usar clase activa
     */
    private fun limpiarSeleccionManual() {
        Log.d("ID_CLASE_DEBUG", "Limpiando selección manual...")
        idClaseSeleccionada = -1
        claseSeleccionadaInfo = null

        // Refrescar información
        obtenerClaseActual()
        obtenerActividadesRecientes()
        actualizarEstadoBotones()

        if (idClaseActual != -1) {
            Toast.makeText(this, "Ahora usando clase activa", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Selección manual limpiada", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Obtiene y muestra las actividades recientes de la clase en uso
     */
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
                        tvActividadesRecientes.text = "Última actividad: ${actividades[0].titulo} (hace poco)"
                    } else {
                        tvActividadesRecientes.text = "No hay actividades registradas hoy"
                    }
                } else {
                    tvActividadesRecientes.text = "Error al cargar actividades recientes"
                    Log.e("DocenteHome", "Error API: ${response.code()}")
                }

            } catch (e: Exception) {
                tvActividadesRecientes.text = "Error al cargar actividades recientes"
                Log.e("DocenteHome", "Error al obtener actividades recientes", e)
            }
        }
    }

    /**
     * Actualiza el estado del botón "Cambiar Bitácora"
     */
    private fun actualizarEstadoBotones() {
        val btnElegirClase = findViewById<Button>(R.id.btnBitacora)
        btnElegirClase.isEnabled = true
        btnElegirClase.alpha = 1.0f

        when {
            // Hay selección manual (con o sin clase activa)
            idClaseSeleccionada != -1 -> {
                if (idClaseActual != -1) {
                    btnElegirClase.text = "📆 Cambiar Bitácora (Manual activa)"
                } else {
                    btnElegirClase.text = "📆 Cambiar Bitácora (Manual)"
                }
            }
            // Solo hay clase activa
            idClaseActual != -1 -> {
                btnElegirClase.text = "📆 Elegir Otra Bitácora"
            }
            // No hay ninguna clase
            else -> {
                btnElegirClase.text = "📆 Elegir Bitácora"
            }
        }
    }

    /**
     * Actualiza la UI cuando se selecciona una clase manualmente
     */
    private fun actualizarInfoClaseSeleccionada() {
        claseSeleccionadaInfo?.let { clase ->
            val builder = SpannableStringBuilder()

            // Materia y grupo como título principal
            val tituloStart = builder.length
            builder.append("${clase.materia} - ${clase.grupo}\n")
            builder.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                tituloStart,
                builder.length - 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                ForegroundColorSpan(Color.parseColor("#1976D2")),
                tituloStart,
                builder.length - 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            builder.append("Profesor: ${clase.nombre_profesor ?: "N/A"}\n")
            builder.append("NRC: ${clase.nrc ?: "N/A"}\n")

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
        }
    }

    /**
     * Obtiene la clase actual sin afectar selección manual
     */
    private fun obtenerClaseActual() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getClaseActual(idProfesor)
                if (response.isSuccessful) {
                    val body = response.body()
                    val claseData = body?.data
                    val clase = claseData?.clase_actual

                    if (clase != null) {
                        Log.d("ID_CLASE_DEBUG", "Clase activa detectada: ${clase.id_clase}")
                        idClaseActual = clase.id_clase

                        // Solo actualizar UI si NO hay selección manual
                        if (idClaseSeleccionada == -1) {
                            mostrarInfoClaseActiva(claseData, clase)
                        }
                    } else {
                        idClaseActual = -1
                        if (idClaseSeleccionada == -1) {
                            mostrarSinClase(claseData)
                        }
                    }

                    actualizarEstadoBotones()
                } else {
                    idClaseActual = -1
                    if (idClaseSeleccionada == -1) {
                        tvInfoClase.text = "Sin clase actual\nSelecciona una clase para comenzar"
                    }
                    actualizarEstadoBotones()
                }
            } catch (e: Exception) {
                idClaseActual = -1
                actualizarEstadoBotones()
                if (idClaseSeleccionada == -1) {
                    Toast.makeText(this@DocenteHomeActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("DocenteHome", "Error al obtener clase actual", e)
                }
            }
        }
    }

    /**
     * Muestra información de clase activa
     */
    private fun mostrarInfoClaseActiva(claseData: Any?, clase: Any?) {
        val builder = SpannableStringBuilder()

        // Aquí necesitarás adaptar según tu estructura de datos
        // Ejemplo genérico:
        builder.append("Clase activa detectada\n")

        val activaStart = builder.length
        builder.append("🟢 CLASE ACTIVA")
        builder.setSpan(
            BackgroundColorSpan(android.graphics.Color.parseColor("#4CAF50")),
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
    }

    /**
     * Muestra información cuando no hay clase
     */
    private fun mostrarSinClase(claseData: Any?) {
        val builder = SpannableStringBuilder()
        builder.append("Sin clase actual\n")
        builder.append("Selecciona una clase para comenzar a registrar actividades")
        tvInfoClase.text = builder
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refrescarClaseRunnable)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun mostrarDialogoConfirmacionSalir() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("¿Salir?")
        builder.setMessage("¿Estás seguro de que deseas cerrar sesión?")

        builder.setPositiveButton("Sí") { _, _ ->
            val prefs = getSharedPreferences("docentePrefs", MODE_PRIVATE)
            prefs.edit().clear().apply()

            Log.d("DocenteHome", "Sesión cerrada por usuario")

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
}