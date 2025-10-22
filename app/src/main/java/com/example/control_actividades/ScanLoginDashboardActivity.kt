package com.example.control_actividades

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.*
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

class ScanLoginDashboardActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LOGIN_QR"
    }

    private lateinit var permisoCamaraLauncher: ActivityResultLauncher<String>

    @Volatile
    private var isProcessing = false

    @Volatile
    private var isScanning = false

    // ✅ Job para manejar coroutines manualmente
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "🔄 onCreate iniciado")

        if (!validarSesionProfesor()) {
            return
        }

        configurarPermisosCamara()
    }

    override fun onDestroy() {
        super.onDestroy()
        // ✅ Cancelar jobs pendientes
        loadJob?.cancel()
        isProcessing = false
        isScanning = false
        Log.d(TAG, "💀 Activity destruida - recursos liberados")
    }

    private fun validarSesionProfesor(): Boolean {
        val idProfesor = obtenerIdProfesor()
        if (idProfesor == -1) {
            Log.e(TAG, "❌ No hay ID de profesor disponible")
            Toast.makeText(this, "⚠️ Error: No hay sesión de profesor activa", Toast.LENGTH_LONG).show()
            finish()
            return false
        }
        Log.d(TAG, "✅ ID Profesor obtenido: $idProfesor")
        return true
    }

    private fun obtenerIdProfesor(): Int {
        var idProfesor = intent.getIntExtra("id_profesor", -1)
        if (idProfesor == -1) {
            val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
            idProfesor = sharedPref.getInt("id_profesor", -1)
        }
        return idProfesor
    }

    private fun configurarPermisosCamara() {
        permisoCamaraLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                Log.d(TAG, "✅ Permiso de cámara concedido")
                iniciarFlujoEscaneo()
            } else {
                Log.e(TAG, "❌ Permiso de cámara denegado")
                mostrarDialogoPermisoDenegado()
            }
        }

        if (tienePermisoCamara()) {
            Log.d(TAG, "✅ Permiso de cámara ya concedido")
            iniciarFlujoEscaneo()
        } else {
            Log.d(TAG, "📝 Solicitando permiso de cámara...")
            solicitarPermisoCamara()
        }
    }

    private fun tienePermisoCamara(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun solicitarPermisoCamara() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            AlertDialog.Builder(this)
                .setTitle("Permiso de Cámara Requerido")
                .setMessage("Necesitamos acceso a la cámara para escanear códigos QR del dashboard")
                .setPositiveButton("Entendido") { _, _ ->
                    permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
                }
                .setNegativeButton("Cancelar") { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
        } else {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun mostrarDialogoPermisoDenegado() {
        AlertDialog.Builder(this)
            .setTitle("Permiso Denegado")
            .setMessage("No puedes escanear QR sin permisos de cámara. Puedes activarlos en Configuración > Aplicaciones.")
            .setPositiveButton("Configuración") { _, _ ->
                abrirConfiguracionApp()
            }
            .setNegativeButton("Salir") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun abrirConfiguracionApp() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error abriendo configuración: ${e.message}")
            Toast.makeText(this, "Error abriendo configuración", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun iniciarFlujoEscaneo() {
        if (isScanning) {
            Log.w(TAG, "⚠️ Ya se está escaneando")
            return
        }

        // ✅ SIN verificación previa - directo al scanner
        Log.d(TAG, "📷 Iniciando escaneo directo...")
        iniciarEscaneoQR()
    }

    private fun iniciarEscaneoQR() {
        if (isScanning) {
            Log.w(TAG, "⚠️ Ya se está escaneando")
            return
        }

        try {
            isScanning = true
            Log.d(TAG, "📷 Iniciando escaneo QR...")

            val integrator = IntentIntegrator(this).apply {
                setPrompt("Escanea el QR del Dashboard Web")
                setBeepEnabled(true)
                setOrientationLocked(false) // ✅ Permitir rotación
                setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                setTimeout(60000)
                setCameraId(0)
            }
            integrator.initiateScan()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error iniciando escáner: ${e.message}")
            isScanning = false
            Toast.makeText(this, "Error al iniciar cámara", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(TAG, "📋 onActivityResult - requestCode: $requestCode, resultCode: $resultCode")
        isScanning = false

        if (requestCode != IntentIntegrator.REQUEST_CODE) {
            return
        }

        when (resultCode) {
            Activity.RESULT_OK -> {
                val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
                val sessionId = result?.contents?.trim()

                if (sessionId.isNullOrBlank()) {
                    Log.w(TAG, "❌ QR vacío")
                    Toast.makeText(this, "QR inválido", Toast.LENGTH_SHORT).show()
                    finish()
                } else if (!isProcessing) {
                    procesarQRExitoso(sessionId)
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.w(TAG, "❌ Escaneo cancelado por usuario")
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
                finish()
            }
            else -> {
                Log.w(TAG, "❌ Resultado desconocido: $resultCode")
                finish()
            }
        }
    }

    private fun procesarQRExitoso(sessionId: String) {
        if (isProcessing) {
            Log.w(TAG, "⚠️ Ya se está procesando un QR")
            return
        }

        isProcessing = true
        Log.d(TAG, "✅ Session ID escaneado: $sessionId")

        if (!validarSessionId(sessionId)) {
            Log.e(TAG, "❌ Session ID con formato inválido: $sessionId")
            Toast.makeText(this, "QR inválido. Escanea un código válido del dashboard.", Toast.LENGTH_LONG).show()
            isProcessing = false
            finish()
            return
        }

        val idProfesor = obtenerIdProfesor()
        if (idProfesor == -1) {
            Log.e(TAG, "❌ No se pudo obtener ID de profesor")
            Toast.makeText(this, "⚠️ Error: No hay sesión de profesor activa", Toast.LENGTH_LONG).show()
            isProcessing = false
            finish()
            return
        }

        Log.d(TAG, "✅ ID Profesor: $idProfesor")
        mostrarSelectorClase(sessionId, idProfesor)
    }

    private fun validarSessionId(sessionId: String): Boolean {
        return sessionId.isNotBlank() && sessionId.length >= 10
    }

    private fun mostrarSelectorClase(sessionId: String, idProfesor: Int) {
        Log.d(TAG, "🔄 Obteniendo clases para profesor: $idProfesor")

        // ✅ Cancelar job anterior si existe
        loadJob?.cancel()

        // ✅ Nuevo job con SupervisorJob para evitar cancelación prematura
        loadJob = lifecycleScope.launch(SupervisorJob()) {
            try {
                Log.d(TAG, "🌐 Llamando API para obtener clases...")

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getClasesPorProfesor(idProfesor)
                }

                Log.d(TAG, "📚 Clases obtenidas: ${response.clases.size}")

                // ✅ Verificar que la Activity sigue activa
                if (isFinishing || isDestroyed) {
                    Log.w(TAG, "⚠️ Activity finalizando, no mostrar diálogo")
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    when {
                        response.clases.isEmpty() -> {
                            Log.w(TAG, "⚠️ No hay clases asignadas")
                            mostrarDialogoNoClases()
                        }
                        response.clases.size == 1 -> {
                            val claseUnica = response.clases[0]
                            Log.d(TAG, "🎯 Única clase disponible: ${claseUnica.id_clase}")
                            confirmarLogin(sessionId, idProfesor, claseUnica.id_clase)
                        }
                        else -> {
                            mostrarDialogoSeleccionClase(response.clases, sessionId, idProfesor)
                        }
                    }
                }

            } catch (e: CancellationException) {
                Log.w(TAG, "⚠️ Carga de clases cancelada")
                // No hacer nada, es intencional
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error obteniendo clases: ${e.message}", e)

                if (!isFinishing && !isDestroyed) {
                    withContext(Dispatchers.Main) {
                        manejarErrorConexion(e, "cargar clases", sessionId, idProfesor)
                    }
                }
            }
        }
    }

    private fun mostrarDialogoNoClases() {
        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle("Sin Clases Asignadas")
            .setMessage("No tienes clases asignadas para este período.")
            .setPositiveButton("Aceptar") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun mostrarDialogoSeleccionClase(
        clases: List<ClaseProfesorItem>,
        sessionId: String,
        idProfesor: Int
    ) {
        if (isFinishing || isDestroyed) {
            Log.w(TAG, "⚠️ Activity finalizando, no mostrar diálogo")
            return
        }

        val opciones = clases.map { clase ->
            "${clase.materia} - ${clase.grupo}" + (clase.nrc?.let { " - NRC: $it" } ?: "")
        }.toTypedArray()

        Log.d(TAG, "📝 Mostrando diálogo con ${opciones.size} opciones")

        AlertDialog.Builder(this)
            .setTitle("Selecciona la clase para el Dashboard")
            .setItems(opciones) { _, which ->
                val claseSeleccionada = clases[which]
                Log.d(TAG, "🎯 Clase seleccionada: ${claseSeleccionada.id_clase}")
                confirmarLogin(sessionId, idProfesor, claseSeleccionada.id_clase)
            }
            .setNegativeButton("Cancelar") { _, _ ->
                Log.d(TAG, "❌ Usuario canceló selección")
                isProcessing = false
                finish()
            }
            .setOnCancelListener {
                Log.d(TAG, "❌ Diálogo cancelado")
                isProcessing = false
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun confirmarLogin(sessionId: String, idProfesor: Int, idClase: Int) {
        Log.d(TAG, "🚀 CONFIRMAR LOGIN INICIADO")
        Log.d(TAG, "   - Session ID: $sessionId")
        Log.d(TAG, "   - Profesor: $idProfesor")
        Log.d(TAG, "   - Clase: $idClase")

        val request = ConfirmarSesionRequest(
            sessionId = sessionId,
            idProfesor = idProfesor,
            idClase = idClase
        )
        Log.d(TAG, "📦 Request creado: $request")

        // ✅ Nuevo job independiente para el login
        lifecycleScope.launch(SupervisorJob()) {
            try {
                Log.d(TAG, "🌐 Enviando confirmación al backend...")
                Log.d(TAG, "📡 URL: ${RetrofitClient.getBaseUrl()}api/login/auth/confirmar-sesion")
                Log.d(TAG, "📦 Body: sessionId=$sessionId, idProfesor=$idProfesor, idClase=$idClase")

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.confirmarSesionDashboard(request)
                }

                Log.d(TAG, "✅ Respuesta recibida exitosamente")
                Log.d(TAG, "📄 Success: ${response.success}")
                Log.d(TAG, "📄 Mensaje: ${response.mensaje}")
                Log.d(TAG, "📄 Clase: ${response.clase}")

                if (!isFinishing && !isDestroyed) {
                    withContext(Dispatchers.Main) {
                        mostrarExitoYFinalizar(response.mensaje)
                    }
                }

            } catch (e: CancellationException) {
                Log.w(TAG, "⚠️ Login cancelado")
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR CRÍTICO en confirmarLogin")
                Log.e(TAG, "   Tipo: ${e.javaClass.simpleName}")
                Log.e(TAG, "   Mensaje: ${e.message}")
                Log.e(TAG, "   Stack trace:", e)

                if (!isFinishing && !isDestroyed) {
                    withContext(Dispatchers.Main) {
                        manejarErrorConexion(e, "confirmar login", sessionId, idProfesor)
                    }
                }
            }
        }
    }

    private fun mostrarExitoYFinalizar(mensaje: String) {
        Toast.makeText(this, "✅ $mensaje", Toast.LENGTH_LONG).show()
        Log.d(TAG, "🏁 Finalizando activity por éxito")

        lifecycleScope.launch {
            delay(1500)
            finish()
        }
    }

    private fun manejarErrorConexion(
        error: Exception,
        operacion: String,
        sessionId: String = "",
        idProfesor: Int = -1
    ) {
        if (isFinishing || isDestroyed) {
            Log.w(TAG, "⚠️ Activity finalizando, no mostrar error")
            return
        }

        Log.e(TAG, "🔌 Error en $operacion: ${error.message}", error)

        val (titulo, mensajeError) = when {
            error is SocketTimeoutException -> "Timeout" to "El servidor tardó demasiado"
            error is ConnectException -> "Conexión Fallida" to "No se puede conectar al servidor"
            error is UnknownHostException -> "Sin Conexión" to "Verifica tu conexión a internet"
            error is HttpException -> when (error.code()) {
                404 -> "No Encontrado" to "Recurso no encontrado (404)"
                500 -> "Error del Servidor" to "Error interno del servidor (500)"
                503 -> "No Disponible" to "El servidor está en mantenimiento"
                else -> "Error HTTP" to "Error del servidor: ${error.code()}"
            }
            else -> "Error de Conexión" to "Error: ${error.message ?: "Desconocido"}"
        }

        try {
            AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage("""
                    $mensajeError
                    
                    Operación: $operacion
                    
                    Verifica:
                    • Conexión a internet activa
                    • Dashboard web abierto
                    • Misma red que el servidor
                """.trimIndent())
                .setPositiveButton("Reintentar") { _, _ ->
                    Log.d(TAG, "🔄 Reintentando...")
                    isProcessing = false

                    when (operacion) {
                        "cargar clases" -> {
                            if (idProfesor != -1 && sessionId.isNotBlank()) {
                                mostrarSelectorClase(sessionId, idProfesor)
                            } else {
                                finish()
                            }
                        }
                        "confirmar login" -> {
                            if (idProfesor != -1 && sessionId.isNotBlank()) {
                                mostrarSelectorClase(sessionId, idProfesor)
                            } else {
                                finish()
                            }
                        }
                        else -> finish()
                    }
                }
                .setNegativeButton("Cancelar") { _, _ ->
                    Log.d(TAG, "❌ Cancelado por usuario")
                    isProcessing = false
                    finish()
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error mostrando diálogo: ${e.message}")
            isProcessing = false
            finish()
        }
    }
}