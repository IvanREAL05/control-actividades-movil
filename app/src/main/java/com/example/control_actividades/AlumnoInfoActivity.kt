package com.example.control_actividades

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch




class AlumnoInfoActivity : AppCompatActivity() {

    private lateinit var tvNombreAlumno: TextView
    private lateinit var tvMatriculaAlumno: TextView
    private lateinit var tvGrupoAlumno: TextView
    private lateinit var btnEscanear: Button
    private lateinit var toolbar: Toolbar
    private lateinit var escaneoLauncher: ActivityResultLauncher<android.content.Intent>

    // Variables para guardar los datos actuales
    private var currentNombre: String = "Sin información"
    private var currentMatricula: String = "Sin información"
    private var currentGrupo: String = "Sin información"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alumno_info)

        Log.d("LIFECYCLE_DEBUG", "=== onCreate ejecutándose ===")

        // Configurar toolbar
        setupToolbar()

        // Inicializar vistas PRIMERO
        initViews()

        // Configurar escaneo de QR
       setupQRScanner()

        // Restaurar datos guardados si existen
        if (savedInstanceState != null) {
            currentNombre = savedInstanceState.getString("nombre", "Sin información")
            currentMatricula = savedInstanceState.getString("matricula", "Sin información")
            currentGrupo = savedInstanceState.getString("grupo", "Sin información")
            Log.d("LIFECYCLE_DEBUG", "Datos restaurados - Nombre: $currentNombre")

            // Actualizar UI con datos restaurados
            updateUIWithCurrentData()
        }

        // Botón de escaneo
        btnEscanear.setOnClickListener {
            iniciarEscaneo()
        }
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Habilitar botón de regreso
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Manejar clic en botón de regreso
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Guardar los datos actuales antes de que se destruya la Activity
        outState.putString("nombre", currentNombre)
        outState.putString("matricula", currentMatricula)
        outState.putString("grupo", currentGrupo)
        Log.d("LIFECYCLE_DEBUG", "Datos guardados - Nombre: $currentNombre, Matrícula: $currentMatricula, Grupo: $currentGrupo")
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("LIFECYCLE_DEBUG", "=== Cambio de configuración detectado ===")

        // Restaurar datos después del cambio de orientación
        Handler(Looper.getMainLooper()).postDelayed({
            if (currentNombre != "Sin información") {
                updateUIWithCurrentData()
                Log.d("LIFECYCLE_DEBUG", "Datos restaurados después de cambio de orientación")
            }
        }, 100)
    }

    private fun updateUIWithCurrentData() {
        // Actualizar la UI con los datos actuales
        tvNombreAlumno.text = currentNombre
        tvMatriculaAlumno.text = currentMatricula
        tvGrupoAlumno.text = currentGrupo
        Log.d("LIFECYCLE_DEBUG", "UI actualizada con datos actuales")
    }

    private fun initViews() {
        try {
            tvNombreAlumno = findViewById(R.id.tvNombreAlumno)
            tvMatriculaAlumno = findViewById(R.id.tvMatriculaAlumno)
            tvGrupoAlumno = findViewById(R.id.tvGrupoAlumno)
            btnEscanear = findViewById(R.id.btnEscanear)

            Log.d("UI_DEBUG", "Vistas inicializadas correctamente")

        } catch (e: Exception) {
            Log.e("UI_DEBUG", "Error al inicializar vistas", e)
            Toast.makeText(this, "Error al inicializar interfaz", Toast.LENGTH_LONG).show()
        }
    }

   private fun setupQRScanner() {
        escaneoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val intent = result.data
            val res: IntentResult? = IntentIntegrator.parseActivityResult(result.resultCode, intent)
            if (res != null && res.contents != null) {
                val qrData = res.contents
                Log.d("QR_DEBUG", "QR escaneado: $qrData")
                mostrarInfoQR(qrData)
            } else {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun iniciarEscaneo() {
        val integrator = IntentIntegrator(this)
        integrator.setPrompt("Coloca el QR dentro del recuadro")
        integrator.setBeepEnabled(true)
        integrator.setOrientationLocked(true)
        escaneoLauncher.launch(integrator.createScanIntent())
    }
    private fun mostrarInfoQR(qrData: String) {
        Log.d("API_DEBUG", "=== INICIANDO LLAMADA API ===")
        Log.d("API_DEBUG", "QR Data completo: '$qrData'")
        Log.d("API_DEBUG", "Longitud del QR: ${qrData.length}")

        val request = QRInfoRequest(qrData)
        Log.d("API_DEBUG", "Request creado: $request")

        // Usamos lifecycleScope para lanzar la coroutine
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.obtenerInfoQR(request)

                if (response.success && response.data != null) {
                    val info = response.data
                    Log.d("API_DEBUG", "Body completo: $info")
                    Log.d("API_DEBUG", "QR válido: ${info.qr_valido}")
                    Log.d("API_DEBUG", "Nombre: ${info.datos_qr.nombre}")
                    Log.d("API_DEBUG", "Matrícula: ${info.datos_qr.matricula}")
                    Log.d("API_DEBUG", "Grupo: ${info.datos_qr.grupo}")

                    // Ejecutar en hilo principal para actualizar UI
                    runOnUiThread {
                        updateUI(info)
                    }

                } else {
                    Log.e("API_DEBUG", "Respuesta exitosa pero body o data es null")
                    runOnUiThread {
                        Toast.makeText(
                            this@AlumnoInfoActivity,
                            "❌ Respuesta vacía del servidor",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("API_DEBUG", "Error al obtener info del QR", e)
                runOnUiThread {
                    Toast.makeText(
                        this@AlumnoInfoActivity,
                        "❌ Error de conexión o servidor: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun updateUI(datosQR: QRData) {
        try {
            Log.d("UI_DEBUG", "=== Actualizando información del alumno ===")

            // Guardar los datos en variables de clase
            currentNombre = datosQR.datos_qr.nombre
            currentMatricula = datosQR.datos_qr.matricula
            currentGrupo = datosQR.datos_qr.grupo

            // Actualizar textos
            tvNombreAlumno.text = currentNombre
            tvMatriculaAlumno.text = currentMatricula
            tvGrupoAlumno.text = currentGrupo

            tvNombreAlumno.visibility = View.VISIBLE
            tvMatriculaAlumno.visibility = View.VISIBLE
            tvGrupoAlumno.visibility = View.VISIBLE

            tvNombreAlumno.invalidate()
            tvMatriculaAlumno.invalidate()
            tvGrupoAlumno.invalidate()

            Log.d("UI_DEBUG", "Información actualizada: ${datosQR.datos_qr.nombre}")
            Toast.makeText(this, "✅ Información cargada correctamente", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("UI_DEBUG", "❌ Error al actualizar UI: ${e.message}", e)
            Toast.makeText(this, "❌ Error al mostrar información", Toast.LENGTH_SHORT).show()
        }
    }
}