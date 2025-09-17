package com.example.control_actividades

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch



class JustificanteActivity : AppCompatActivity() {

    private lateinit var etFechaExpedicion: EditText
    private lateinit var etMatricula: EditText
    private lateinit var etNombreEstudiante: EditText
    private lateinit var etFechaInicio: EditText
    private lateinit var etFechaFin: EditText
    private lateinit var etGestor: EditText
    private lateinit var etNumeroGestor: EditText
    private lateinit var etSituacion: EditText
    private lateinit var etFolio: EditText
    private lateinit var etEjecutivo: EditText
    private lateinit var tvNombreArchivo: TextView
    private lateinit var tvNombreArchivoIne: TextView
    private lateinit var pdfPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var inePickerLauncher: ActivityResultLauncher<Intent>

    private var uriPdfSeleccionado: Uri? = null
    private var uriIneSeleccionado: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_justificante)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Enviar Justificante"

        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Vincular vistas
        etFechaExpedicion = findViewById(R.id.etFechaExpedicion)
        etMatricula = findViewById(R.id.etMatricula)
        etNombreEstudiante = findViewById(R.id.etNombreEstudiante)
        etFechaInicio = findViewById(R.id.etFechaInicio)
        etFechaFin = findViewById(R.id.etFechaFin)
        etGestor = findViewById(R.id.etGestor)
        etNumeroGestor = findViewById(R.id.etNumeroGestor)
        etSituacion = findViewById(R.id.etSituacion)
        etFolio = findViewById(R.id.etFolio)
        etEjecutivo = findViewById(R.id.etEjecutivo)
        tvNombreArchivo = findViewById(R.id.tvNombreArchivo)
        tvNombreArchivoIne = findViewById(R.id.tvNombreArchivoIne)

        findViewById<Button>(R.id.btnSeleccionarPdf).setOnClickListener {
            seleccionarPdf()
        }

        findViewById<Button>(R.id.btnSeleccionarIne).setOnClickListener {
            seleccionarIne()
        }

        findViewById<Button>(R.id.btnEnviarJustificante).setOnClickListener {
            enviarFormulario()
        }

        pdfPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    uriPdfSeleccionado = uri
                    tvNombreArchivo.text = obtenerNombreArchivo(uri)
                }
            }
        }

        inePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    uriIneSeleccionado = uri
                    tvNombreArchivoIne.text = obtenerNombreArchivo(uri)
                }
            }
        }
    }

    private fun seleccionarPdf() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        pdfPickerLauncher.launch(Intent.createChooser(intent, "Selecciona el justificante PDF"))
    }

    private fun seleccionarIne() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        inePickerLauncher.launch(Intent.createChooser(intent, "Selecciona la copia del INE"))
    }

    private fun obtenerNombreArchivo(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            return cursor.getString(nameIndex)
        }
        return "archivo"
    }

    private fun crearRequestBody(valor: String): RequestBody =
        valor.toRequestBody("text/plain".toMediaTypeOrNull())

    private fun validarCampos(): Boolean {
        if (etMatricula.text.isNullOrBlank()) {
            etMatricula.error = "Matrícula requerida"
            etMatricula.requestFocus()
            return false
        }

        if (etNombreEstudiante.text.isNullOrBlank()) {
            etNombreEstudiante.error = "Nombre del estudiante requerido"
            etNombreEstudiante.requestFocus()
            return false
        }

        if (etFechaExpedicion.text.isNullOrBlank()) {
            etFechaExpedicion.error = "Fecha de expedición requerida"
            etFechaExpedicion.requestFocus()
            return false
        } else if (!esFechaValida(etFechaExpedicion.text.toString())) {
            etFechaExpedicion.error = "Formato inválido (yyyy-MM-dd)"
            etFechaExpedicion.requestFocus()
            return false
        }

        if (etFechaInicio.text.isNullOrBlank()) {
            etFechaInicio.error = "Fecha de inicio requerida"
            etFechaInicio.requestFocus()
            return false
        } else if (!esFechaValida(etFechaInicio.text.toString())) {
            etFechaInicio.error = "Formato inválido (yyyy-MM-dd)"
            etFechaInicio.requestFocus()
            return false
        }

        if (etFechaFin.text.isNullOrBlank()) {
            etFechaFin.error = "Fecha de fin requerida"
            etFechaFin.requestFocus()
            return false
        } else if (!esFechaValida(etFechaFin.text.toString())) {
            etFechaFin.error = "Formato inválido (yyyy-MM-dd)"
            etFechaFin.requestFocus()
            return false
        }

        if (uriPdfSeleccionado == null) {
            Toast.makeText(this, "Selecciona el justificante PDF", Toast.LENGTH_SHORT).show()
            return false
        }

        if (uriIneSeleccionado == null) {
            Toast.makeText(this, "Selecciona la copia del INE", Toast.LENGTH_SHORT).show()
            return false
        }

        if (etGestor.text.isNullOrBlank()) {
            etGestor.error = "Nombre del gestor requerido"
            etGestor.requestFocus()
            return false
        }

        if (etSituacion.text.isNullOrBlank()) {
            etSituacion.error = "Situación requerida"
            etSituacion.requestFocus()
            return false
        }

        return true
    }

    private fun esFechaValida(fecha: String): Boolean {
        val regex = Regex("""\d{4}-\d{2}-\d{2}""")
        return regex.matches(fecha)
    }


    private fun enviarFormulario() {
        if (!validarCampos()) return

        val api = RetrofitClient.instance

        // Crear archivos temporales si se seleccionaron
        val multipartPdf: MultipartBody.Part? = uriPdfSeleccionado?.let {
            val archivoPdf = crearArchivoTemporal(it)
            val requestPdf = archivoPdf.asRequestBody("application/pdf".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("documento_pdf", archivoPdf.name, requestPdf)
        }

        val multipartIne: MultipartBody.Part? = uriIneSeleccionado?.let {
            val archivoIne = crearArchivoTemporal(it)
            val requestIne = archivoIne.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("documento_ine", archivoIne.name, requestIne)
        }

        // Crear RequestBody de texto para cada campo (usar "" si es opcional y está vacío)
        fun crearRequest(texto: String?): RequestBody =
            (texto ?: "").toRequestBody("text/plain".toMediaTypeOrNull())

        // 🔹 Llamada suspend dentro de coroutine
        lifecycleScope.launch {
            try {
                api.enviarJustificante(
                    crearRequest(etFechaExpedicion.text.toString()),
                    crearRequest(etMatricula.text.toString()),
                    crearRequest(etNombreEstudiante.text.toString()),
                    crearRequest(etFechaInicio.text.toString()),
                    crearRequest(etFechaFin.text.toString()),
                    crearRequest(etGestor.text.toString()),
                    crearRequest(etNumeroGestor.text.toString()),
                    crearRequest(etSituacion.text.toString()),
                    crearRequest(etFolio.text.toString()),
                    crearRequest(etEjecutivo.text.toString()),
                    multipartPdf ?: MultipartBody.Part.createFormData("documento_pdf", "", "".toRequestBody()),
                    multipartIne ?: MultipartBody.Part.createFormData("documento_ine", "", "".toRequestBody())
                )

                Toast.makeText(
                    this@JustificanteActivity,
                    "✅ Justificante enviado correctamente",
                    Toast.LENGTH_LONG
                ).show()
                finish()

            } catch (e: Exception) {
                Toast.makeText(
                    this@JustificanteActivity,
                    "❌ Error al enviar justificante: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun crearArchivoTemporal(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val archivoTemp = File(cacheDir, obtenerNombreArchivo(uri))
        val outputStream = FileOutputStream(archivoTemp)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return archivoTemp
    }
}