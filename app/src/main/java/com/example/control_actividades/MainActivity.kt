package com.example.control_actividades

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.activity.addCallback
import android.view.View
import android.widget.ProgressBar
import android.util.Log

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PAGE_SIZE = 10 // Número de avisos por página
    }

    //private lateinit var btnEstudiante: Button
    private lateinit var btnDocente: Button
    private lateinit var btnAlumno: Button
    private lateinit var tvEscuela: TextView
    private lateinit var btnAcercaDe: Button
    private lateinit var progressBar: ProgressBar
    private var isAppEnabled = false

    // ========== NUEVAS VARIABLES PARA PAGINACIÓN ==========
    private lateinit var recyclerView: RecyclerView
    private lateinit var avisoAdapter: AvisoAdapter
    private lateinit var layoutManager: LinearLayoutManager

    private var currentPage = 1
    private var isLoadingAvisos = false
    private var hasMorePages = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configurar fondo de la actividad
        window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorBackground))

        // Inicializar vistas
        initViews()
        // Configurar elementos de la UI
        setupUI()

        // Verificar si la app está habilitada ANTES de configurar todo lo demás
        verificarAppHabilitada()

        // Controlar botón atrás
        onBackPressedDispatcher.addCallback(this) {
            finishAffinity()
        }
    }

    private fun initViews() {
        tvEscuela = findViewById(R.id.tvEscuela)
        //btnEstudiante = findViewById(R.id.btnEstudiante)
        btnAlumno = findViewById((R.id.btnAlumno))
        btnDocente = findViewById(R.id.btnDocente)
        btnAcercaDe = findViewById(R.id.btnAcercaDe)
        progressBar = findViewById(R.id.progressBar)

        // ========== INICIALIZAR VISTAS PARA PAGINACIÓN ==========
        recyclerView = findViewById(R.id.recyclerView)
        setupRecyclerView()
    }

    // ========== NUEVO: CONFIGURAR RECYCLERVIEW CON PAGINACIÓN ==========
    private fun setupRecyclerView() {
        avisoAdapter = AvisoAdapter()
        layoutManager = LinearLayoutManager(this)

        recyclerView.apply {
            adapter = avisoAdapter
            layoutManager = this@MainActivity.layoutManager
            addOnScrollListener(scrollListener)
        }
        avisoAdapter.setEmpty(true)

    }

    // ========== NUEVO: LISTENER PARA SCROLL INFINITO ==========
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            // Solo cargar más si la app está habilitada
            if (!isAppEnabled) return

            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

            // Cargar más cuando estemos cerca del final
            if (!isLoadingAvisos && hasMorePages) {
                if (visibleItemCount + firstVisibleItemPosition >= totalItemCount - 2) {
                    loadMoreAvisos()
                }
            }
        }
    }

    private fun setupUI() {
        // Configurar botones
        setupButtons()
    }

    private fun setupButtons() {
        btnDocente.apply {
            setOnClickListener {
                if (isAppEnabled) {
                    navigateToTeacherMode()
                } else {
                    showToast("La aplicación está deshabilitada")
                }
            }
        }

        btnAcercaDe.apply {
            setOnClickListener { mostrarDialogoAcercaDe() }
        }

        btnAlumno.apply {
            setOnClickListener {
                if (isAppEnabled) {
                    // Navegar a la pantalla de Alumno
                    val intent = Intent(this@MainActivity, AlumnoInfoActivity::class.java)
                    startActivity(intent)
                } else {
                    showToast("La aplicación está deshabilitada")
                }
            }
        }


        // Añadir iconos a los botones (opcional)
        setButtonIcons()
    }

    private fun verificarAppHabilitada() {
        Log.d("LoginActivity", "Verificando si la app está habilitada...")
        progressBar.visibility = View.VISIBLE

        AppMasterRetrofitClient.instance.getAppEnabled().enqueue(object : Callback<AppEnabledResponse> {
            override fun onResponse(call: Call<AppEnabledResponse>, response: Response<AppEnabledResponse>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    isAppEnabled = response.body()!!.appEnabled
                    Log.d("LoginActivity", "App habilitada: $isAppEnabled")
                    configurarInterfazSegunEstado()
                } else {
                    Log.e("LoginActivity", "Error en respuesta de verificación de app")
                    isAppEnabled = false
                    configurarInterfazSegunEstado()
                    Toast.makeText(this@MainActivity, "Error al verificar estado de la app", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AppEnabledResponse>, t: Throwable) {
                Log.e("LoginActivity", "Error de red al verificar app: ${t.message}")
                progressBar.visibility = View.GONE
                isAppEnabled = false
                configurarInterfazSegunEstado()
                Toast.makeText(this@MainActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun configurarInterfazSegunEstado() {
        Log.d("LoginActivity", "Configurando interfaz según estado: isAppEnabled = $isAppEnabled")

        if (isAppEnabled) {
            // App habilitada: mostrar avisos y habilitar botón
            Log.d("LoginActivity", "App HABILITADA - Cargando avisos y habilitando botón")
            btnDocente.isEnabled = true
            btnDocente.alpha = 1.0f
            recyclerView.visibility = View.VISIBLE
            // Solo cargar avisos si la app está habilitada
            // ========== MODIFICADO: USAR NUEVA FUNCIÓN DE PAGINACIÓN ==========
            loadInitialAvisos()
        } else {
            // App deshabilitada: NO mostrar avisos ni progressBar adicional
            Log.d("LoginActivity", "App DESHABILITADA - Ocultando avisos y deshabilitando botón")
            btnDocente.isEnabled = false
            btnDocente.alpha = 0.5f
            recyclerView.visibility = View.GONE
            // Configurar RecyclerView vacío sin mostrar progressBar
            /*recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = AvisoAdapter(emptyList())
            recyclerView.visibility = View.GONE*/
            // Asegurar que el progressBar esté oculto
            progressBar.visibility = View.GONE
        }
    }

    private fun mostrarDialogoAcercaDe() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Acerca de la App")
            .setMessage(
                "Nombre de la App: Control de Actividades v1.0\n" +
                        "Director: Mtro.Ernesto Sandoval Munive\n" +
                        "Coordinador: Mtro. Javier Díaz Sánchez\n" +
                        "Analista y Programador: Iván Reyes Álvarez\n" +
                        "Año: 2025"
            )
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun setButtonIcons() {

        // Icono para docente
        val teacherIcon = ContextCompat.getDrawable(this, R.drawable.ic_teacher)?.mutate()
        teacherIcon?.let {
            DrawableCompat.setTint(it, ContextCompat.getColor(this, R.color.white))
            btnDocente.setCompoundDrawablesWithIntrinsicBounds(teacherIcon, null, null, null)
            btnDocente.compoundDrawablePadding = 16
        }

        val studentIcon = ContextCompat.getDrawable(this, R.drawable.ic_student)?.mutate()
        studentIcon?.let {
            DrawableCompat.setTint(it, ContextCompat.getColor(this, R.color.white))
            btnAlumno.setCompoundDrawablesWithIntrinsicBounds(studentIcon, null, null, null)
            btnAlumno.compoundDrawablePadding = 16
        }

        val infoIcon = ContextCompat.getDrawable(this, R.drawable.ic_info)?.mutate()
        infoIcon?.let {
            DrawableCompat.setTint(it, ContextCompat.getColor(this, R.color.white))
            btnAcercaDe.setCompoundDrawablesWithIntrinsicBounds(infoIcon, null, null, null)
            btnAcercaDe.compoundDrawablePadding = 16
        }
    }

   private fun navigateToTeacherMode() {
        try {
            startActivity(Intent(this, DocenteLoginActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        } catch (e: Exception) {
            showToast("Error al iniciar modo docente")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // ========== NUEVA: CARGAR AVISOS INICIALES CON PAGINACIÓN ==========
    private fun loadInitialAvisos() {
        currentPage = 1
        hasMorePages = true
        avisoAdapter.clearAvisos()
        loadAvisos(currentPage) // Quitar el parámetro isInitialLoad
    }

    // ========== NUEVA: CARGAR MÁS AVISOS ==========
    private fun loadMoreAvisos() {
        if (!isLoadingAvisos && hasMorePages) {
            currentPage++
            loadAvisos(currentPage) // Quitar el parámetro isInitialLoad
        }
    }

    // ========== NUEVA: FUNCIÓN PRINCIPAL PARA CARGAR AVISOS ==========
    private fun loadAvisos(page: Int = 1, isRefresh: Boolean = false) {
        isLoadingAvisos = true // Agregar esta línea
        // Si es refresh, limpiar lista
        if (isRefresh) {
            avisoAdapter.clearAvisos()
            currentPage = 1
        }

        // Solo mostrar loading si no es la primera carga después de refresh
        if (page > 1 && !isRefresh) {
            recyclerView.post {
                avisoAdapter.setLoading(true)
            }
        }

        val call = RetrofitClient.instance.obtenerAvisosPaginados(page, PAGE_SIZE)

        call.enqueue(object : Callback<AvisoResponse> {
            override fun onResponse(call: Call<AvisoResponse>, response: Response<AvisoResponse>) {
                isLoadingAvisos = false
                avisoAdapter.setLoading(false)

                if (response.isSuccessful) {
                    val avisosResponse = response.body()
                    avisosResponse?.let { avisos ->
                        Log.d("LoginActivity", "Avisos recibidos página $page: ${avisos.data.size}")
                        Log.d("LoginActivity", "Total páginas: ${avisos.pagination.totalPages}, Página actual: ${avisos.pagination.currentPage}")

                        // Verificar si no hay avisos en la primera página
                        if (page == 1 && avisos.data.isEmpty()) {
                            // Mostrar estado vacío
                            avisoAdapter.setEmpty(true)
                            Log.d("LoginActivity", "No hay avisos disponibles - mostrando estado vacío")
                        } else {
                            // Hay avisos, quitar estado vacío y agregar datos
                            avisoAdapter.setEmpty(false)
                            avisoAdapter.addAvisos(avisos.data)
                        }

                        // Actualizar estado de paginación
                        currentPage = avisos.pagination.currentPage
                        hasMorePages = avisos.pagination.hasNextPage
                        avisoAdapter.setHasMorePages(hasMorePages)

                        if (!hasMorePages) {
                            Log.d("LoginActivity", "No hay más páginas disponibles")
                        }
                    }
                } else {
                    // Manejar error de respuesta
                    handleLoadError("Error del servidor: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<AvisoResponse>, t: Throwable) {
                isLoadingAvisos = false
                avisoAdapter.setLoading(false)
                handleLoadError("Error de conexión: ${t.message}")
            }
        })
    }

    private fun handleLoadError(errorMessage: String) {
        Log.e("LoginActivity", errorMessage)

        // Si es la primera carga y falla, mostrar estado vacío con mensaje de error
        if (currentPage <= 1 && avisoAdapter.getAvisosCount() == 0) {
            // Podrías crear un estado de error personalizado o usar el empty
            avisoAdapter.setEmpty(true)
        }

    }
}