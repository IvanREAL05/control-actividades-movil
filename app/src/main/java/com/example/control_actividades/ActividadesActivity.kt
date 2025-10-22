package com.example.control_actividades

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.integration.android.IntentIntegrator
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.TextView
import okhttp3.ResponseBody
import java.io.File
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import android.widget.EditText
import android.text.InputType
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.media.MediaScannerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.GlobalScope
import com.example.control_actividades.database.AppDatabase
import com.example.control_actividades.repository.OfflineRepository


class ActividadesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnCrear: MaterialButton
    private lateinit var btnVer: MaterialButton
    private lateinit var btnDescargarExcel: MaterialButton
    private lateinit var cardNoActividades: MaterialCardView
    private lateinit var  btnDescargarExcel2: MaterialButton
    private lateinit var btnHistorialAlumnos: MaterialButton
    private var validacionJob: Job? = null


    private val actividades = mutableListOf<Actividad>()
    private lateinit var adapter: ActividadAdapter
    private lateinit var offlineRepository: OfflineRepository
    private lateinit var db: AppDatabase

    private var idClase: Int = -1

    // Estado para saber qué acción está activa (editar, eliminar, ver)
    private enum class Modo { NONE, EDITAR, ELIMINAR, VER }
    private var modoActual = Modo.NONE

    // ==== Escaneo (igual patrón que EscanerQRActivity) ====

    private lateinit var permisoCamaraLauncher: ActivityResultLauncher<String>
    private lateinit var requestStoragePermission: ActivityResultLauncher<String>
    private var escaneando: Boolean = false
    private var actividadParaEscanear: Actividad? = null
    // =====================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actividades_activity)
        // Inicializar base de datos y repositorio offline
        db = AppDatabase.getDatabase(applicationContext)
        offlineRepository = OfflineRepository(
            context = applicationContext,
            db = db,
            apiService = RetrofitClient.instance
        )

        // Mostrar contador de pendientes
        mostrarContadorPendientes()

        idClase = intent.getIntExtra("id_clase", -1)
        if (idClase == -1) {
            Toast.makeText(this, "No se recibió id_clase", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnCrear = findViewById(R.id.btnCrearActividad)
        btnVer = findViewById(R.id.btnVerActividades)
        recyclerView = findViewById(R.id.recyclerActividades)
        // Inicializar botón de descarga Excel
        // Inicializar views
        btnDescargarExcel = findViewById(R.id.btnDescargarLista)
        cardNoActividades = findViewById(R.id.cardNoActividades)
        btnDescargarExcel2 = findViewById(R.id.btnDescargarListaGeneral)
        btnHistorialAlumnos = findViewById(R.id.btnHistorialAlumnos)

        adapter = ActividadAdapter(
            actividades,
            onEditarClick = { actividad -> abrirEditarActividad(actividad) },
            onEliminarClick = { actividad -> confirmarEliminarActividad(actividad) },
            onItemClick = { actividad -> mostrarDetallesActividad(actividad) },
            onQRClick = { actividad -> escanearQR(actividad) },
            onVerClick = { actividad -> verListaAlumnos(actividad) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnCrear.setOnClickListener {
            modoActual = Modo.NONE
            recyclerView.visibility = View.GONE
            abrirCrearActividad()
        }


        btnVer.setOnClickListener {
            modoActual = Modo.VER
            cargarYMostrarActividades()
        }

        // Configurar listener para descarga Excel
        btnDescargarExcel.setOnClickListener {
            descargarReporteExcel()
        }

        btnDescargarExcel2.setOnClickListener{
            descargarReporteExcelGeneral()
        }

        btnHistorialAlumnos.setOnClickListener {
            Log.d("TRACE", "ActividadesActivity -> HistorialAlumnosActivity | idClase = $idClase")
            val intent = Intent(this, HistorialAlumnosActivity::class.java)
            intent.putExtra("id_clase", idClase) // <-- enviar el idClase aquí
            startActivity(intent)
        }

        // Configurar launcher para permisos de almacenamiento
        requestStoragePermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted: Boolean ->
            if (granted) {
                iniciarDescargaExcel()
            } else {
                Toast.makeText(this, "❌ Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show()
            }
        }

        // ==== Launchers de escaneo & permisos (igual patrón) ====

        permisoCamaraLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                iniciarEscaneo()
            } else {
                Log.e("QR_ACTIVIDAD", "Permiso de cámara denegado")
                showToast("❌ Permiso de cámara denegado.")
            }
        }


        onBackPressedDispatcher.addCallback(this) {
            if (escaneando) {
                escaneando = false
                showToast("Escaneo cancelado")
            } else {
                finish()
            }
        }
        // =======================================================
    }
    // ← AQUÍ VA onNewIntent()
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("QR_ACTIVIDAD", "onNewIntent() llamado - Activity reutilizada")
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("QR_ACTIVIDAD", "onActivityResult() - requestCode: $requestCode, resultCode: $resultCode")

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result == null) {
            Log.w("QR_ACTIVIDAD", "result es null, no es un escaneo QR")
            return
        }

        if (result.contents == null) {
            Log.d("QR_ACTIVIDAD", "Escaneo cancelado por usuario")
            showToast("Escaneo cancelado")
            escaneando = false

            // ✅ Limpiar SOLO cuando el usuario cancela
            limpiarDatosEscaneo()
            return
        }

        // ✅ Si llegamos aquí, ES un escaneo válido
        escaneando = false

        val qr = result.contents
        Log.d("QR_ACTIVIDAD", "✅ QR escaneado: $qr")

        // Recuperar actividad (NO limpiar aquí todavía)
        val actividad = actividadParaEscanear ?: run {
            Log.w("QR_ACTIVIDAD", "actividadParaEscanear es null, recuperando de SharedPreferences")
            val prefs = getSharedPreferences("escaneo_temp", Context.MODE_PRIVATE)
            val idActividad = prefs.getInt("id_actividad", -1)

            if (idActividad == -1) {
                Log.e("QR_ACTIVIDAD", "ERROR: No se encontró id_actividad en SharedPreferences")
                showToast("⚠️ No se pudo recuperar la actividad")
                limpiarDatosEscaneo()
                return
            }

            Actividad(
                id_actividad = idActividad,
                id_clase = prefs.getInt("id_clase", idClase),
                titulo = prefs.getString("titulo_actividad", "") ?: "",
                descripcion = prefs.getString("descripcion_actividad", ""),
                fecha_entrega = prefs.getString("fecha_entrega", "") ?: "",
                hora_entrega = prefs.getString("hora_entrega", ""),
                fecha_creacion = prefs.getString("fecha_creacion", "") ?: "",
                estado = prefs.getString("estado", ""),
                fecha_entrega_real = prefs.getString("fecha_entrega_real", ""),
                vigencia = prefs.getString("vigencia", ""),
                valor_maximo = prefs.getInt("valor_maximo", 0),
                tipo_actividad = prefs.getString("tipo_actividad", "") ?: ""
            )
        }

        Log.d("QR_ACTIVIDAD", "✅ Actividad recuperada: id=${actividad.id_actividad}, titulo='${actividad.titulo}'")

        // Registrar entrega (NO limpiar aquí)
        validarEntregaActividad(qr, actividad.id_actividad, actividad)
    }

    private fun cargarYMostrarActividades() {
        recyclerView.visibility = View.VISIBLE

        // Lanza una corrutina en el ciclo de vida de la actividad
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getActividadesPorClase(idClase)

                actividades.clear()
                actividades.addAll(response.actividades ?: emptyList())
                adapter.notifyDataSetChanged()

                val tvNoActividades: TextView = findViewById(R.id.tvNoActividades)
                if (actividades.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    tvNoActividades.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    tvNoActividades.visibility = View.GONE
                }

                Log.d("ActividadesActivity", "Actividades cargadas: ${actividades.size}")
            } catch (e: Exception) {
                Log.e("ActividadesActivity", "Error al cargar actividades: ${e.message}", e)
                Toast.makeText(this@ActividadesActivity, "Error al cargar actividades", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        if (modoActual != Modo.NONE) {
            cargarYMostrarActividades()
        }
    }

    private fun abrirCrearActividad() {
        val intent = Intent(this, CrearEditarActividadActivity::class.java)
        intent.putExtra("id_clase", idClase)
        intent.putExtra("modo", "crear")
        startActivity(intent)
    }

    private fun abrirEditarActividad(actividad: Actividad) {
        val intent = Intent(this, CrearEditarActividadActivity::class.java)
        intent.putExtra("id_clase", idClase)
        intent.putExtra("modo", "editar")
        intent.putExtra("actividad", actividad)
        startActivity(intent)
    }

    private fun confirmarEliminarActividad(actividad: Actividad) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar actividad")
            .setMessage("¿Estás seguro que deseas eliminar '${actividad.titulo}'?")
            .setPositiveButton("Sí") { _, _ -> eliminarActividad(actividad) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun eliminarActividad(actividad: Actividad) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.eliminarActividad(actividad.id_actividad)

                if (response.isSuccessful) {
                    Toast.makeText(this@ActividadesActivity, "Actividad eliminada", Toast.LENGTH_SHORT).show()
                    Log.d("ActividadesActivity", "Eliminando actividad con id: ${actividad.id_actividad}")

                    actividades.remove(actividad)
                    adapter.notifyDataSetChanged()
                } else {
                    Log.e("EliminarActividad", "Error ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(this@ActividadesActivity, "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("EliminarActividad", "Falla en la llamada: ${e.message}", e)
                Toast.makeText(this@ActividadesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun mostrarDetallesActividad(actividad: Actividad) {
        AlertDialog.Builder(this)
            .setTitle(actividad.titulo)
            .setMessage("Descripción:\n${actividad.descripcion}\n\nFecha entrega: ${actividad.fecha_entrega}\nEstado: ${actividad.vigencia}")
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun limpiarDatosEscaneo() {
        Log.d("QR_ACTIVIDAD", "Limpiando datos de escaneo...")
        getSharedPreferences("escaneo_temp", Context.MODE_PRIVATE).edit().clear().apply()
        actividadParaEscanear = null
    }

    // ===================== ESCANEO PARA ACTIVIDADES =====================

    private fun validarEntregaActividad(qr: String, idActividad: Int, actividad: Actividad) {
        Log.d("QR_VALIDAR", "Llamando a validarEntregaActividad()")
        Log.d("QR_VALIDAR", "QR: $qr")
        Log.d("QR_VALIDAR", "idActividad: $idActividad")
        Log.d("QR_VALIDAR", "Actividad: ${actividad.titulo} (id=${actividad.id_actividad})")

        val request = EntregaRequest(qr, idActividad)

        // ⚠️ Usar GlobalScope para que NO se cancele con el lifecycle
        GlobalScope.launch(Dispatchers.Main) {
            try {
                Log.d("QR_VALIDAR", "🌐 Iniciando petición HTTP...")
                val body = RetrofitClient.instance.validarEntrega(request)
                Log.d("QR_VALIDAR", "✅ Respuesta recibida: $body")

                when {
                    actividad.tipo_actividad.equals("examen", ignoreCase = true) -> {
                        Log.d("QR_VALIDAR", "📝 Examen detectado")
                        abrirDialogCalificacionManual(qr, actividad, body)
                    }
                    body.tarde -> {
                        Log.d("QR_VALIDAR", "⏰ Entrega tardía detectada")
                        abrirDialogCalificacionManual(qr, actividad, body)
                    }
                    body.success -> {
                        val calificacion = body.calificacion ?: actividad.valor_maximo
                        Log.d("QR_VALIDAR", "✅ Entrega a tiempo, calificación: $calificacion")
                        registrarEntregaActividadConCalificacion(qr, actividad.id_actividad, actividad, calificacion)
                    }
                    else -> {
                        Log.w("QR_VALIDAR", "⚠️ Respuesta no exitosa: ${body.mensaje}")
                        showToast(body.mensaje)
                    }
                }

            } catch (e: Exception) {
                Log.e("QR_VALIDAR", "❌ Error en validación: ${e.message}", e)
                showToast("Error: ${e.message}")
            }
        }
    }


    private fun abrirDialogCalificacionManual(qr: String, actividad: Actividad, response: ValidarEntregaResponse) {
        Log.d("QR_MANUAL", "abrirDialogCalificacionManual() para: ${response.nombre}")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "Calificación (0 - 10)"

        // Determinar el título según el tipo
        val titulo = if (actividad.tipo_actividad.equals("examen", ignoreCase = true)) {
            "Examen: ${response.nombre}"
        } else {
            "Entrega tardía: ${response.nombre}"
        }

        val mensaje = if (actividad.tipo_actividad.equals("examen", ignoreCase = true)) {
            "Ingresa calificación para el examen"
        } else {
            "Ingresa calificación manual"
        }

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setView(input)
            .setPositiveButton("Registrar") { _, _ ->
                val calificacion = input.text.toString().toIntOrNull()
                Log.d("QR_MANUAL", "Calificación ingresada: $calificacion")
                if (calificacion == null || calificacion !in 0..10) {
                    Log.e("QR_MANUAL", "Calificación inválida ingresada")
                    showToast("Calificación inválida")
                } else {
                    registrarEntregaActividadConCalificacion(qr, actividad.id_actividad, actividad, calificacion)
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                Log.d("QR_MANUAL", "Usuario canceló el registro manual")
            }
            .show()
    }

    private fun registrarEntregaActividadConCalificacion(
        qr: String,
        idActividad: Int,
        actividad: Actividad,
        calificacion: Int?
    ) {
        Log.d("QR_REGISTRAR", "registrarEntregaActividadConCalificacion()")
        Log.d("QR_REGISTRAR", "QR: $qr")
        Log.d("QR_REGISTRAR", "idActividad: $idActividad")
        Log.d("QR_REGISTRAR", "Actividad: ${actividad.titulo} (id=${actividad.id_actividad})")
        Log.d("QR_REGISTRAR", "Calificación a enviar: $calificacion")

        lifecycleScope.launch {
            try {
                Log.d("QR_REGISTRAR", "🌐 Enviando entrega de actividad...")

                // ✅ Usar el repositorio offline
                offlineRepository.guardarEntregaActividad(qr, idActividad, calificacion)

                // Verificar si se guardó offline o se envió
                val hayConexion = tieneConexionInternet()

                if (hayConexion) {
                    Log.d("QR_REGISTRAR", "✅ Entrega registrada exitosamente")
                    showToast("✅ Entrega registrada exitosamente")
                } else {
                    Log.d("QR_REGISTRAR", "💾 Guardado localmente (sin internet)")
                    showToast("💾 Se guardó localmente. Se sincronizará cuando haya internet.")
                }

                reproducirSonidoEscaneo()

                // Actualizar contador
                mostrarContadorPendientes()

                preguntarSeguirEscaneando(actividad)

            } catch (e: Exception) {
                Log.e("QR_REGISTRAR", "❌ Error al procesar entrega", e)
                reproducirSonidoError()
                vibrarCelular()

                if (!isFinishing && !isDestroyed) {
                    AlertDialog.Builder(this@ActividadesActivity)
                        .setTitle("Error")
                        .setMessage("❌ Error al procesar: ${e.message}")
                        .setPositiveButton("Seguir escaneando") { d, _ ->
                            d.dismiss()
                            iniciarEscaneo()
                        }
                        .setNegativeButton("Cerrar", null)
                        .show()
                }
            }
        }
    }

    private fun escanearQR(actividad: Actividad) {
        Log.d("QR_ACTIVIDAD", "=== INICIANDO ESCANEO ===")
        Log.d("QR_ACTIVIDAD", "Actividad recibida:")
        Log.d("QR_ACTIVIDAD", "  - id_actividad: ${actividad.id_actividad}")
        Log.d("QR_ACTIVIDAD", "  - titulo: '${actividad.titulo}'")

        // Validar que la actividad tenga datos válidos
        if (actividad.id_actividad <= 0) {
            Log.e("QR_ACTIVIDAD", "ERROR: Actividad con ID inválido: ${actividad.id_actividad}")
            showToast("⚠️ Error: Actividad con ID inválido")
            return
        }

        if (actividad.titulo.isBlank()) {
            Log.e("QR_ACTIVIDAD", "ERROR: Actividad sin título")
            showToast("⚠️ Error: Actividad sin título")
            return
        }

        // Guardar TODA la actividad en SharedPreferences
        val prefs = getSharedPreferences("escaneo_temp", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("id_actividad", actividad.id_actividad)
            .putInt("id_clase", actividad.id_clase)
            .putString("titulo_actividad", actividad.titulo)
            .putString("descripcion_actividad", actividad.descripcion ?: "")
            .putString("fecha_entrega", actividad.fecha_entrega)
            .putString("hora_entrega", actividad.hora_entrega ?: "")
            .putString("fecha_creacion", actividad.fecha_creacion)
            .putString("estado", actividad.estado ?: "")
            .putString("fecha_entrega_real", actividad.fecha_entrega_real ?: "")
            .putString("vigencia", actividad.vigencia ?: "")
            .putInt("valor_maximo", actividad.valor_maximo)
            .putString("tipo_actividad", actividad.tipo_actividad)
            .apply()

        Log.d("QR_ACTIVIDAD", "Actividad completa guardada en SharedPreferences")

        actividadParaEscanear = actividad
        Log.d("QR_ACTIVIDAD", "actividadParaEscanear asignada: ${actividadParaEscanear != null}")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarEscaneo()
        } else {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun iniciarEscaneo() {
        if (escaneando) return
        escaneando = true

        val integrator = IntentIntegrator(this)
        integrator.setPrompt("Escanea el QR del alumno para '${actividadParaEscanear?.titulo ?: "actividad"}'")
        integrator.setBeepEnabled(false)
        integrator.setOrientationLocked(true)
        integrator.setCaptureActivity(com.journeyapps.barcodescanner.CaptureActivity::class.java)
        integrator.initiateScan()
    }


    private fun preguntarSeguirEscaneando(actividad: Actividad) {
        AlertDialog.Builder(this)
            .setTitle("¿Seguir escaneando?")
            .setMessage("Actividad: ${actividad.titulo}\nPuedes registrar otro alumno o finalizar.")
            .setPositiveButton("Sí, otro QR") { d, _ ->
                d.dismiss()
                Log.d("QR_ACTIVIDAD", "Usuario eligió seguir escaneando")
                // ✅ Mantener actividadParaEscanear y SharedPreferences
                iniciarEscaneo()
            }
            .setNegativeButton("No") { d, _ ->
                d.dismiss()
                Log.d("QR_ACTIVIDAD", "Usuario eligió finalizar escaneo")
                // ✅ Limpiar SOLO cuando el usuario finaliza
                limpiarDatosEscaneo()
            }
            .setCancelable(false)
            .show()
    }

    // ===================== Utilidades UI/sonido =====================

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun reproducirSonidoEscaneo() {
        val mediaPlayer = MediaPlayer.create(this, R.raw.scan_success)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener { it.release() }
    }

    private fun reproducirSonidoError() {
        val mediaPlayer = MediaPlayer.create(this, R.raw.error_sound)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener { it.release() }
    }

    private fun vibrarCelular() {
        val vibrator = getSystemService(Vibrator::class.java)
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val efecto = VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE)
                it.vibrate(efecto)
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(250)
            }
        }
    }

    private fun verListaAlumnos(actividad: Actividad) {
        val intent = Intent(this, ListaActividadesActivity::class.java)
        intent.putExtra(ListaActividadesActivity.EXTRA_ACTIVIDAD_ID, actividad.id_actividad) // ← Cambiar .id por .id_actividad
        intent.putExtra(ListaActividadesActivity.EXTRA_ACTIVIDAD_TITULO, "Alumnos - ${actividad.titulo}")
        startActivity(intent)
    }


// ===================== DESCARGA DE EXCEL =====================

    private fun descargarReporteExcel() {
        // Verificar que tenemos un id_clase válido
        if (idClase == -1) {
            Toast.makeText(this, "⚠️ Error: No se encontró ID de clase", Toast.LENGTH_SHORT).show()
            return
        }
        // Verificar permisos y descargar directamente
        verificarPermisosYDescargarExcel()
    }

    private fun verificarPermisosYDescargarExcel() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10+ no requiere permisos especiales para escribir en Downloads
                iniciarDescargaExcel()
            }
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                iniciarDescargaExcel()
            }
            else -> {
                requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun iniciarDescargaExcel() {
        // Mostrar estado de descarga
        btnDescargarExcel.isEnabled = false
        btnDescargarExcel.text = "📊 Descargando Excel..."

        Log.d("ExcelDownload", "Iniciando descarga de reporte para clase: $idClase")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.descargarReporteActividades(idClase)

                if (response.isSuccessful && response.body() != null) {
                    Log.d("ExcelDownload", "Descarga exitosa, guardando archivo...")
                    guardarExcelEnDescargas(response.body()!!)
                } else {
                    Log.e("ExcelDownload", "Error al descargar Excel: ${response.code()} - ${response.message()}")
                    Toast.makeText(
                        this@ActividadesActivity,
                        "❌ Error al descargar Excel (${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("ExcelDownload", "Error de conexión al descargar Excel", e)
                Toast.makeText(
                    this@ActividadesActivity,
                    "❌ Error de red: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // Restaurar estado del botón
                btnDescargarExcel.isEnabled = true
                btnDescargarExcel.text = "📊 Descargar Excel"
            }
        }
    }

    private fun guardarExcelEnDescargas(responseBody: ResponseBody) {
        // IMPORTANTE: Mover operación de I/O a un dispatcher IO
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Obtener nombre del archivo desde el header o usar uno por defecto
                val contentDisposition = responseBody.contentType()?.toString() ?: ""
                val fileName = "Reporte_Actividades_${System.currentTimeMillis()}.xlsx"

                // Para Android 10+ (API 29+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }

                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                    uri?.let {
                        contentResolver.openOutputStream(it)?.use { outputStream ->
                            responseBody.byteStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        Log.d("ExcelDownload", "Archivo guardado exitosamente: $fileName")

                        // Volver al hilo principal para UI
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ActividadesActivity,
                                "✅ Excel descargado en Descargas",
                                Toast.LENGTH_LONG
                            ).show()

                            // Abrir el archivo automáticamente
                            abrirArchivoExcel(uri)
                        }
                    } ?: run {
                        Log.e("ExcelDownload", "Error al crear URI para guardar archivo")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ActividadesActivity, "❌ Error al guardar archivo", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    // Para Android 9 y anteriores
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, fileName)

                    FileOutputStream(file).use { outputStream ->
                        responseBody.byteStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    Log.d("ExcelDownload", "Archivo guardado: ${file.absolutePath}")

                    // Volver al hilo principal para UI
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ActividadesActivity,
                            "✅ Excel descargado en Descargas",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    // Notificar al sistema sobre el nuevo archivo
                    MediaScannerConnection.scanFile(
                        this@ActividadesActivity,
                        arrayOf(file.absolutePath),
                        arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                        null
                    )

                    // Abrir el archivo
                    withContext(Dispatchers.Main) {
                        abrirArchivoExcel(Uri.fromFile(file))
                    }
                }

            } catch (e: Exception) {
                Log.e("ExcelDownload", "Error al guardar Excel", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ActividadesActivity,
                        "❌ Error al guardar archivo: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun abrirArchivoExcel(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Verificar si hay una app que pueda abrir Excel
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(
                    this,
                    "No hay aplicación para abrir archivos Excel",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Log.e("ExcelDownload", "Error al abrir archivo", e)
            Toast.makeText(
                this,
                "Archivo descargado, ábrelo desde la carpeta Descargas",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    //Nuevo excel
    private fun descargarReporteExcelGeneral() {
        // Verificar que tenemos un id_clase válido
        if (idClase == -1) {
            Toast.makeText(this, "⚠️ Error: No se encontró ID de clase", Toast.LENGTH_SHORT).show()
            return
        }
        // Verificar permisos y descargar directamente
        verificarPermisosYDescargarExcelGeneral()
    }

    private fun verificarPermisosYDescargarExcelGeneral() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10+ no requiere permisos especiales para escribir en Downloads
                iniciarDescargaExcelGeneral()
            }
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                iniciarDescargaExcelGeneral()
            }
            else -> {
                requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun iniciarDescargaExcelGeneral() {
        // IMPORTANTE: Usar Dispatchers.IO desde el inicio
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                btnDescargarExcel2.isEnabled = false
                btnDescargarExcel2.text = "📊 Descargando Excel..."
            }

            Log.d("ExcelDownload", "Iniciando descarga de reporte general: $idClase")

            try {
                val response = RetrofitClient.instance.descargarReporteClaseCompleto(idClase)

                if (response.isSuccessful && response.body() != null) {
                    Log.d("ExcelDownload", "Descarga exitosa, guardando archivo...")
                    guardarExcelGeneralEnDescargas(response.body()!!)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ActividadesActivity,
                            "✅ Descarga completada",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val code = response.code()
                    val msg = response.message()
                    Log.e("ExcelDownload", "Error al descargar Excel: $code -> $msg")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ActividadesActivity,
                            "❌ Error al descargar Excel ($code)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("ExcelDownload", "Error de conexión al descargar Excel", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ActividadesActivity,
                        "❌ Error de red: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    btnDescargarExcel2.isEnabled = true
                    btnDescargarExcel2.text = "📥 Descargar Excel"
                }
            }
        }
    }


    private fun guardarExcelGeneralEnDescargas(responseBody: ResponseBody) {
        // IMPORTANTE: Mover operación de I/O a un dispatcher IO
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Nombre del archivo con timestamp
                val fileName = "ClaseCompleto_${System.currentTimeMillis()}.xlsx"

                // Para Android 10+ (API 29+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }

                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                    uri?.let {
                        contentResolver.openOutputStream(it)?.use { outputStream ->
                            responseBody.byteStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        Log.d("ExcelDownload", "Archivo guardado exitosamente: $fileName")

                        // Volver al hilo principal para UI
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ActividadesActivity,
                                "✅ Excel completo descargado en Descargas",
                                Toast.LENGTH_LONG
                            ).show()

                            // Abrir el archivo automáticamente
                            abrirArchivoExcel(uri)
                        }
                    } ?: run {
                        Log.e("ExcelDownload", "Error al crear URI para guardar archivo")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ActividadesActivity, "❌ Error al guardar archivo", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    // Para Android 9 y anteriores
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, fileName)

                    FileOutputStream(file).use { outputStream ->
                        responseBody.byteStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    Log.d("ExcelDownload", "Archivo guardado: ${file.absolutePath}")

                    // Volver al hilo principal para UI
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ActividadesActivity,
                            "✅ Excel completo descargado en Descargas",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    // Notificar al sistema sobre el nuevo archivo
                    MediaScannerConnection.scanFile(
                        this@ActividadesActivity,
                        arrayOf(file.absolutePath),
                        arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                        null
                    )

                    // Abrir el archivo
                    withContext(Dispatchers.Main) {
                        abrirArchivoExcel(Uri.fromFile(file))
                    }
                }

            } catch (e: Exception) {
                Log.e("ExcelDownload", "Error al guardar Excel general", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ActividadesActivity,
                        "❌ Error al guardar archivo: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        validacionJob?.cancel()
        Log.d("QR_ACTIVIDAD", "Activity destruida, jobs cancelados")
    }

    // Verificar conexión a internet
    private fun tieneConexionInternet(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Mostrar contador de registros pendientes
    private fun mostrarContadorPendientes() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pendientes = offlineRepository.contarPendientes()

                withContext(Dispatchers.Main) {
                    if (pendientes > 0) {
                        supportActionBar?.subtitle = "📤 $pendientes pendiente(s)"
                        Log.d("OFFLINE", "Hay $pendientes registros pendientes de sincronizar")
                    } else {
                        supportActionBar?.subtitle = null
                    }
                }
            } catch (e: Exception) {
                Log.e("OFFLINE", "Error al contar pendientes: ${e.message}")
            }
        }
    }

    // Sincronizar manualmente
    private fun sincronizarManualmente() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                offlineRepository.sincronizarPendientes()

                withContext(Dispatchers.Main) {
                    mostrarContadorPendientes()
                    showToast("✅ Sincronización completada")
                }
            } catch (e: Exception) {
                Log.e("OFFLINE", "Error al sincronizar: ${e.message}")
            }
        }
    }

}