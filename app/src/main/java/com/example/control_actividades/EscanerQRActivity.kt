package com.example.control_actividades

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import android.animation.ObjectAnimator
import android.widget.ProgressBar
import android.os.Handler
import android.os.Looper
import android.content.Context
import com.example.control_actividades.database.AppDatabase
import com.example.control_actividades.repository.OfflineRepository
import com.example.control_actividades.repository.SaveResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EscanerQRActivity : AppCompatActivity() {

    private lateinit var escaneoLauncher: ActivityResultLauncher<Intent>
    private lateinit var permisoCamaraLauncher: ActivityResultLauncher<String>
    private lateinit var tvScanResult: TextView
    private lateinit var progressBar: ProgressBar

    private var escaneando: Boolean = false
    private var estadoSeleccionado: String = "presente"
    private var currentDialog: AlertDialog? = null

    private val activityId = System.currentTimeMillis().toString()
    private lateinit var offlineRepository: OfflineRepository
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escaner_qr)

        db = AppDatabase.getDatabase(applicationContext)
        offlineRepository = OfflineRepository(
            context = applicationContext,
            db = db,
            apiService = RetrofitClient.instance
        )

        mostrarContadorPendientes()

        val idClase = intent.getIntExtra("id_clase", -1)
        Log.d("ID_CLASE_DEBUG", "EscanerQR recibió ID: $idClase")

        if (idClase == -1) {
            Log.e("ID_CLASE_DEBUG", "ID de clase inválido en escáner")
            Toast.makeText(this, "⚠️ No hay clase activa. No puedes escanear.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        estadoSeleccionado = intent.getStringExtra("modo") ?: "presente"
        tvScanResult = findViewById(R.id.tvScanResult)
        progressBar = findViewById(R.id.progressBar)

        val btnCancelar: Button = findViewById(R.id.btnCancelar)
        val btnVerLista: Button = findViewById(R.id.btnVerLista)
        val btnSincronizar: Button = findViewById(R.id.btnSincronizar)

        btnCancelar.setOnClickListener {
            finish()
        }

        btnVerLista.setOnClickListener {
            val idClase = intent.getIntExtra("id_clase", -1)
            if (idClase != -1) {
                val intent = Intent(this, ListaGrupoActivity::class.java)
                intent.putExtra("id_clase", idClase)
                startActivity(intent)
            } else {
                showToast("⚠️ No se encontró la clase actual.")
            }
        }

        btnSincronizar.setOnClickListener {
            if (tieneConexionInternet()) {
                showToast("📡 Sincronizando registros pendientes...")
                sincronizarManualmente()
            } else {
                showToast("❌ No hay conexión a internet")
            }
        }

        actualizarVisibilidadBotonSincronizar()

        escaneoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val intent = result.data
            val res: IntentResult? = IntentIntegrator.parseActivityResult(result.resultCode, intent)
            escaneando = false

            if (res != null) {
                if (res.contents == null) {
                    tvScanResult.text = getString(R.string.scan_cancelled)
                    showToast("Escaneo cancelado")
                } else {
                    tvScanResult.text = getString(R.string.qr_scanned, res.contents)
                    processScanResult(res.contents)
                }
            } else {
                tvScanResult.text = getString(R.string.no_result)
            }
        }

        permisoCamaraLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                iniciarEscaneo()
            } else {
                showToast("❌ Permiso de cámara denegado. No se puede escanear.")
                tvScanResult.text = getString(R.string.permiso_denegado)
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (escaneando) {
                escaneando = false
                showToast("Escaneo cancelado")
            } else if (ScannerStateManager.isProcessing) {
                showToast("⏳ Espera a que termine el registro...")
            } else {
                finish()
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarEscaneo()
        } else {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentDialog?.dismiss()
        currentDialog = null
        ScannerStateManager.onActivityDestroyed(activityId)
    }

    private fun iniciarEscaneo() {
        if (escaneando) {
            Log.w("ESCANEO", "Ya hay un escaneo en progreso")
            return
        }

        if (ScannerStateManager.isProcessing) {
            Log.w("ESCANEO", "Hay una petición HTTP en proceso GLOBALMENTE, esperando...")
            showToast("⏳ Espera a que termine el registro anterior...")
            return
        }

        escaneando = true
        Log.d("ESCANEO", "Iniciando nuevo escaneo (Activity: $activityId)")

        val integrator = IntentIntegrator(this)
        integrator.setPrompt("Escanea el código QR")
        integrator.setBeepEnabled(false)
        integrator.setOrientationLocked(true)
        escaneoLauncher.launch(integrator.createScanIntent())
    }

    private fun processScanResult(scanData: String) {
        if (!ScannerStateManager.tryLock(activityId)) {
            Log.w("API", "Ya hay una petición en proceso en otra Activity")
            showToast("⏳ Procesando petición anterior...")
            return
        }

        tvScanResult.text = "📡 Enviando asistencia..."
        mostrarIndicadorCarga(true)
        reproducirSonidoEscaneo()
        vibrarCelular()

        val idClase = intent.getIntExtra("id_clase", -1)
        Log.d("ID_CLASE_DEBUG", "Procesando QR con ID de clase: $idClase")
        Log.d("QR_DEBUG", "Contenido del QR enviado: $scanData")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val startTime = System.currentTimeMillis()
                Log.d("API", "🚀 Intentando guardar asistencia...")
                ScannerStateManager.markRequestStarted()

                val resultado = offlineRepository.guardarAsistencia(
                    qr = scanData,
                    estado = estadoSeleccionado,
                    idClase = idClase
                )

                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime
                Log.d("PERFORMANCE", "⏱️ Tiempo de procesamiento: ${responseTime}ms")

                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) {
                        Log.w("LIFECYCLE", "Activity ya destruida, limpiando estado")
                        ScannerStateManager.unlock(activityId)
                        return@withContext
                    }

                    mostrarIndicadorCarga(false)
                    reproducirSonidoExito()
                    animarTextoExito()

                    when (resultado) {
                        is SaveResult.OnlineSuccess -> {
                            Log.d("RESULTADO", "✅ Guardado ONLINE exitoso")
                            tvScanResult.text = "✅ Asistencia registrada"
                            showToastExito("✅ Asistencia registrada exitosamente")
                        }
                        is SaveResult.OfflineSaved -> {
                            Log.d("RESULTADO", "💾 Guardado OFFLINE")
                            tvScanResult.text = "💾 Guardado localmente (sin internet)"
                            showToastExito("💾 Se guardó localmente. Se sincronizará cuando haya internet.")
                        }
                        is SaveResult.Error -> {
                            Log.e("RESULTADO", "❌ Error: ${resultado.message}")
                            tvScanResult.text = "❌ Error al guardar"
                            showToast("❌ ${resultado.message}")
                        }
                    }

                    mostrarContadorPendientes()
                    actualizarVisibilidadBotonSincronizar()
                    ScannerStateManager.unlock(activityId)

                    Handler(Looper.getMainLooper()).postDelayed({
                        if (!isFinishing && !isDestroyed) {
                            mostrarDialogoContinuar()
                        }
                    }, 2000)
                }

            } catch (e: Exception) {
                Log.e("OFFLINE_ERROR", "Error al procesar: ${e.message}", e)

                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) {
                        ScannerStateManager.unlock(activityId)
                        return@withContext
                    }

                    mostrarIndicadorCarga(false)
                    tvScanResult.text = "❌ Error al guardar"
                    showToast("❌ Error: ${e.message}")
                    reproducirSonidoError()
                    vibrarCelular()

                    ScannerStateManager.unlock(activityId)
                    mostrarDialogoContinuar()
                }
            }
        }
    }

    private fun mostrarDialogoContinuar() {
        if (isFinishing || isDestroyed) return

        currentDialog?.dismiss()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("¿Seguir escaneando?")
        builder.setMessage("¿Quieres registrar otro alumno?")

        builder.setPositiveButton("Sí, continuar") { dialog, _ ->
            dialog.dismiss()
            currentDialog = null
            iniciarEscaneo()
        }

        builder.setNegativeButton("Terminar") { dialog, _ ->
            dialog.dismiss()
            currentDialog = null
            finish()
        }

        builder.setCancelable(false)
        currentDialog = builder.create()
        currentDialog?.show()
    }

    private fun mostrarIndicadorCarga(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun animarTextoExito() {
        val scaleX = ObjectAnimator.ofFloat(tvScanResult, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(tvScanResult, "scaleY", 1f, 1.2f, 1f)
        scaleX.duration = 300
        scaleY.duration = 300
        scaleX.start()
        scaleY.start()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showToastExito(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun reproducirSonidoEscaneo() {
        try {
            val mediaPlayer = MediaPlayer.create(this, R.raw.scan_success)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            Log.e("AUDIO", "Error: ${e.message}")
        }
    }

    private fun reproducirSonidoExito() {
        try {
            val mediaPlayer = MediaPlayer.create(this, R.raw.scan_success)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            Log.e("AUDIO", "Error: ${e.message}")
        }
    }

    private fun reproducirSonidoError() {
        try {
            val mediaPlayer = MediaPlayer.create(this, R.raw.error_sound)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            Log.e("AUDIO", "Error: ${e.message}")
        }
    }

    private fun vibrarCelular() {
        try {
            val vibrator = getSystemService(Vibrator::class.java)
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val efecto = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                    it.vibrate(efecto)
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(200)
                }
            }
        } catch (e: Exception) {
            Log.e("VIBRATION", "Error: ${e.message}")
        }
    }

    private fun tieneConexionInternet(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun mostrarContadorPendientes() {
        CoroutineScope(Dispatchers.IO).launch {
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

    private fun actualizarVisibilidadBotonSincronizar() {
        CoroutineScope(Dispatchers.IO).launch {
            val pendientes = offlineRepository.contarPendientes()
            withContext(Dispatchers.Main) {
                val btnSincronizar: Button = findViewById(R.id.btnSincronizar)
                btnSincronizar.visibility = if (pendientes > 0) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    private fun sincronizarManualmente() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                offlineRepository.sincronizarPendientes()

                withContext(Dispatchers.Main) {
                    mostrarContadorPendientes()
                    actualizarVisibilidadBotonSincronizar()
                    showToast("✅ Sincronización completada")
                }
            } catch (e: Exception) {
                Log.e("OFFLINE", "Error al sincronizar: ${e.message}")
                withContext(Dispatchers.Main) {
                    showToast("❌ Error al sincronizar")
                }
            }
        }
    }
}