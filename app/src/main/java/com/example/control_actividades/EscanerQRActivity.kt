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
import retrofit2.Response
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import android.animation.ObjectAnimator
import android.widget.ProgressBar
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class EscanerQRActivity : AppCompatActivity() {

    private lateinit var escaneoLauncher: ActivityResultLauncher<Intent>
    private lateinit var permisoCamaraLauncher: ActivityResultLauncher<String>
    private lateinit var tvScanResult: TextView
    private lateinit var progressBar: ProgressBar
    private var escaneando: Boolean = false
    private var estadoSeleccionado: String = "presente"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escaner_qr)

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
        progressBar = findViewById(R.id.progressBar) // Agregar ProgressBar al layout

        val btnCancelar: Button = findViewById(R.id.btnCancelar)
        val btnVerLista: Button = findViewById(R.id.btnVerLista)

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

    private fun iniciarEscaneo() {
        if (escaneando) return
        escaneando = true

        val integrator = IntentIntegrator(this)
        integrator.setPrompt("Escanea el código QR")
        integrator.setBeepEnabled(false)
        integrator.setOrientationLocked(true)
        escaneoLauncher.launch(integrator.createScanIntent())
    }

    private fun processScanResult(scanData: String) {
        // 🔹 FEEDBACK INMEDIATO - Usuario sabe que algo está pasando
        tvScanResult.text = "📡 Enviando asistencia..."
        mostrarIndicadorCarga(true)
        reproducirSonidoEscaneo()
        vibrarCelular()

        val idClase = intent.getIntExtra("id_clase", -1)
        Log.d("ID_CLASE_DEBUG", "Procesando QR con ID de clase: $idClase")
        Log.d("QR_DEBUG", "Contenido del QR enviado: $scanData")

        val request = AsistenciaRequest(
            qr = scanData,
            estado = estadoSeleccionado,
            id_clase = idClase
        )

        Log.d("API_REQUEST", "Request completo: QR=${scanData}, Estado=${estadoSeleccionado}, ID_Clase=${idClase}")

        // 🔹 Lanzar coroutine para llamar a Retrofit
        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                val response = RetrofitClient.instance.registrarAsistencia(request)
                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime
                Log.d("PERFORMANCE", "⏱️ Tiempo de respuesta: ${responseTime}ms")

                mostrarIndicadorCarga(false)

                if (response.success) {
                    // 🔹 ÉXITO
                    reproducirSonidoExito()
                    animarTextoExito()

                    val mensaje = response.mensaje ?: "Asistencia registrada correctamente"
                    tvScanResult.text = "✅ $mensaje"
                    showToastExito("✅ $mensaje")
                    Log.d("API", "Respuesta exitosa: $mensaje")

                    // 🔹 Auto-continuar después de 2 segundos si es exitoso
                    Handler(Looper.getMainLooper()).postDelayed({
                        mostrarDialogoContinuar()
                    }, 2000)

                } else {
                    // 🔹 ERROR LÓGICO (backend devolvió success=false)
                    tvScanResult.text = "❌ Error al registrar"
                    showToast("❌ Error: ${response.mensaje ?: "Revisa tu conexión"}")
                    reproducirSonidoError()
                    vibrarCelular()
                    mostrarDialogoContinuar()
                }
            } catch (e: Exception) {
                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime
                Log.e("PERFORMANCE", "❌ Error después de ${responseTime}ms: ${e.message}")

                mostrarIndicadorCarga(false)
                tvScanResult.text = "❌ Error de conexión"
                showToast("❌ Verifica tu conexión a Internet")
                reproducirSonidoError()
                vibrarCelular()
                mostrarDialogoContinuar()
            }
        }
    }

    private fun manejarError(response: Response<AsistenciaResponse>, responseTime: Long) {
        val errorBody = response.errorBody()?.string()
        Log.e("API", "Error en la respuesta (${responseTime}ms): $errorBody")

        when {
            errorBody?.contains("Ya existe un registro de asistencia") == true -> {
                // 🔹 Ya registrado - Feedback específico pero no alarmante
                reproducirSonidoAdvertencia()
                tvScanResult.text = "ℹ️ Ya registrado previamente"
                mostrarDialogoAlerta(
                    "Ya registrado 🎓",
                    "Este alumno ya tiene asistencia registrada en esta clase."
                )
            }
            errorBody?.contains("Clase no encontrada") == true -> {
                reproducirSonidoError()
                tvScanResult.text = "⚠️ Clase no encontrada"
                showToast("⚠️ Clase no encontrada - Revisa tu horario")
                mostrarDialogoContinuar()
            }
            else -> {
                reproducirSonidoError()
                tvScanResult.text = "❌ Error del servidor"
                showToast("❌ Error del servidor")
                mostrarDialogoContinuar()
            }
        }
    }

    private fun mostrarIndicadorCarga(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) android.view.View.VISIBLE else android.view.View.GONE
        if (mostrar) {
            // Animación de rotación del progress bar
            val animator = ObjectAnimator.ofFloat(progressBar, "rotation", 0f, 360f)
            animator.duration = 1000
            animator.repeatCount = ObjectAnimator.INFINITE
            animator.start()
        }
    }

    private fun animarTextoExito() {
        // Animación de escala para el texto de éxito
        val scaleX = ObjectAnimator.ofFloat(tvScanResult, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(tvScanResult, "scaleY", 1f, 1.2f, 1f)
        scaleX.duration = 300
        scaleY.duration = 300
        scaleX.start()
        scaleY.start()
    }

    private fun mostrarDialogoContinuar() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("¿Seguir escaneando?")
        builder.setMessage("¿Quieres registrar otro alumno?")

        builder.setPositiveButton("Sí, continuar") { dialog, _ ->
            dialog.dismiss()
            iniciarEscaneo()
        }

        builder.setNegativeButton("Terminar") { dialog, _ ->
            dialog.dismiss()
            finish()
        }

        builder.setCancelable(false)
        builder.show()
    }

    private fun mostrarDialogoAlerta(titulo: String, mensaje: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(titulo)
        builder.setMessage(mensaje)
        builder.setPositiveButton("Continuar") { dialog, _ ->
            dialog.dismiss()
            mostrarDialogoContinuar()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showToastExito(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // 🔹 Diferentes sonidos para diferentes situaciones
    private fun reproducirSonidoEscaneo() {
        try {
            val mediaPlayer = MediaPlayer.create(this, R.raw.scan_success)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            Log.e("AUDIO", "Error reproduciendo sonido de escaneo: ${e.message}")
        }
    }

    private fun reproducirSonidoExito() {
        try {
            val mediaPlayer = MediaPlayer.create(this, R.raw.scan_success)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            Log.e("AUDIO", "Error reproduciendo sonido de éxito: ${e.message}")
        }
    }

    private fun reproducirSonidoError() {
        try {
            val mediaPlayer = MediaPlayer.create(this, R.raw.error_sound)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            Log.e("AUDIO", "Error reproduciendo sonido de error: ${e.message}")
        }
    }

    private fun reproducirSonidoAdvertencia() {
        try {
            val mediaPlayer = MediaPlayer.create(this, R.raw.error_sound)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            Log.e("AUDIO", "Error reproduciendo sonido de advertencia: ${e.message}")
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
            Log.e("VIBRATION", "Error vibrando: ${e.message}")
        }
    }
}