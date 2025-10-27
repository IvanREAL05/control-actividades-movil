package com.example.control_actividades

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText


class AlumnosBottomSheet(
    private val nombreClase: String,
    private val listaAlumnos: List<Estudiante>,
    private val onAlumnoSeleccionado: (Estudiante) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var adapter: AlumnosAsistenciaAdapter
    private lateinit var rvAlumnos: RecyclerView
    private lateinit var etBuscarAlumno: TextInputEditText
    private lateinit var tvContadorAlumnos: TextView
    private lateinit var tvClaseNombre: TextView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var btnCerrarBottomSheet: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_alumnos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inicializarVistas(view)
        configurarRecyclerView()
        configurarBuscador()
        configurarBotones()
        actualizarContador()
    }

    private fun inicializarVistas(view: View) {
        rvAlumnos = view.findViewById(R.id.rvAlumnos)
        etBuscarAlumno = view.findViewById(R.id.etBuscarAlumno)
        tvContadorAlumnos = view.findViewById(R.id.tvContadorAlumnos)
        tvClaseNombre = view.findViewById(R.id.tvClaseNombre)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        btnCerrarBottomSheet = view.findViewById(R.id.btnCerrarBottomSheet)

        // Configurar nombre de la clase
        tvClaseNombre.text = nombreClase
    }

    private fun configurarRecyclerView() {
        adapter = AlumnosAsistenciaAdapter(listaAlumnos) { alumno ->
            // Cuando se selecciona un alumno
            onAlumnoSeleccionado(alumno)
            dismiss() // Cerrar el BottomSheet
        }

        rvAlumnos.layoutManager = LinearLayoutManager(requireContext())
        rvAlumnos.adapter = adapter
        rvAlumnos.setHasFixedSize(true)

        // Mostrar/ocultar vista vacía
        mostrarVistaVacia(listaAlumnos.isEmpty())
    }

    private fun configurarBuscador() {
        etBuscarAlumno.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                adapter.filtrar(query)
                actualizarContador()

                // Mostrar vista vacía si no hay resultados
                mostrarVistaVacia(adapter.getListaFiltradaSize() == 0)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun configurarBotones() {
        btnCerrarBottomSheet.setOnClickListener {
            dismiss()
        }
    }

    private fun actualizarContador() {
        val cantidad = adapter.getListaFiltradaSize()
        tvContadorAlumnos.text = if (cantidad == 1) {
            "$cantidad alumno"
        } else {
            "$cantidad alumnos"
        }
    }

    private fun mostrarVistaVacia(mostrar: Boolean) {
        if (mostrar) {
            rvAlumnos.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvAlumnos.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }

    override fun getTheme(): Int {
        return R.style.BottomSheetDialogTheme
    }

    companion object {
        const val TAG = "AlumnosBottomSheet"

        /**
         * Método factory para crear una instancia del BottomSheet
         */
        fun newInstance(
            nombreClase: String,
            listaAlumnos: List<Estudiante>,
            onAlumnoSeleccionado: (Estudiante) -> Unit
        ): AlumnosBottomSheet {
            return AlumnosBottomSheet(nombreClase, listaAlumnos, onAlumnoSeleccionado)
        }
    }
}