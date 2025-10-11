package com.example.control_actividades

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.util.Log
import android.graphics.Color
import androidx.cardview.widget.CardView
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat

class ObservacionesAdapter(
    private val context: Context,
    private var listaAlumnos: List<AlumnoResponse> = emptyList(),
    private var observacionesExistentes: Map<Int, Observacion> = emptyMap(),
    private val onObservacionChanged: (Int, Int) -> Unit,  // ✅ Callback para cambios
    private val onEliminarObservacion: (Int) -> Unit       // ✅ Callback para eliminaciones
) : RecyclerView.Adapter<ObservacionesAdapter.ViewHolder>() {

    // Estados disponibles
    private val estadosDisponibles = listOf(
        EstadoObservacion(0, "Sin observación", "➖", Color.GRAY),
        EstadoObservacion(1, "🏆", "🏆", Color.parseColor("#4CAF50")),
        EstadoObservacion(2, "🔴", "🔴", Color.parseColor("#F44336")),
        EstadoObservacion(3, "🩺", "🩺", Color.parseColor("#2196F3")),
        EstadoObservacion(4, "💛", "💛", Color.parseColor("#FF9800")),
        EstadoObservacion(5, "🟣", "🟣", Color.parseColor("#9C27B0")),
        EstadoObservacion(6, "👷‍♂️", "👷‍♂️", Color.parseColor("#607D8B"))
    )

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNombreAlumno: TextView = view.findViewById(R.id.txtNombreAlumno)
        val txtMatricula: TextView = view.findViewById(R.id.txtMatricula)
        val txtEstadoActual: TextView = view.findViewById(R.id.txtEstadoActual)
        val contenedorEstado: CardView = view.findViewById(R.id.contenedorEstado)
        val spinnerEstado: Spinner = view.findViewById(R.id.spinnerEstado)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_observacion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alumno = listaAlumnos[position]
        val observacionActual = observacionesExistentes[alumno.id_estudiante]

        // ✅ Configurar info del alumno
        val apellidoNombre = "${alumno.apellido} ${alumno.nombre}"
        val spannable = SpannableString(apellidoNombre)
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(holder.itemView.context, R.color.blue)),
            0, // inicio (primer carácter)
            alumno.apellido.length, // fin (justo donde acaba el apellido)
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        holder.txtNombreAlumno.text = spannable
        holder.txtMatricula.text = alumno.matricula

        // ✅ Configurar spinner
        configurarSpinner(holder, alumno.id_estudiante, observacionActual?.estado ?: 0)

        // ✅ Mostrar estado actual visualmente
        mostrarEstadoActual(holder, observacionActual?.estado ?: 0)

        // ✅ Configurar botón eliminar
        configurarBotonEliminar(holder, alumno.id_estudiante, observacionActual != null)
    }

    private fun configurarSpinner(holder: ViewHolder, estudianteId: Int, estadoActual: Int) {
        // Crear adapter para el spinner
        val spinnerAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            estadosDisponibles.map { it.nombre }
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.spinnerEstado.adapter = spinnerAdapter

        // Establecer selección actual
        holder.spinnerEstado.setSelection(estadoActual)

        // ✅ Listener para cambios inmediatos
        holder.spinnerEstado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != estadoActual) {
                    Log.d("ADAPTER_DEBUG", "🔄 Cambio detectado: estudiante=$estudianteId, nuevo estado=$position")
                    onObservacionChanged(estudianteId, position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No hacer nada
            }
        }
    }

    private fun mostrarEstadoActual(holder: ViewHolder, estado: Int) {
        val estadoInfo = estadosDisponibles.find { it.valor == estado }
            ?: estadosDisponibles[0] // Por defecto "Sin observación"

        // ✅ Mostrar ícono en el contenedor
        holder.txtEstadoActual.text = estadoInfo.icono

        // ✅ Cambiar color de fondo del contenedor
        holder.contenedorEstado.setCardBackgroundColor(
            if (estado == 0) Color.parseColor("#F5F5F5") else estadoInfo.color
        )
    }

    private fun configurarBotonEliminar(holder: ViewHolder, estudianteId: Int, tieneObservacion: Boolean) {
        // Solo mostrar botón eliminar si hay observación
        holder.btnEliminar.visibility = if (tieneObservacion) View.VISIBLE else View.GONE

        holder.btnEliminar.setOnClickListener {
            Log.d("ADAPTER_DEBUG", "🗑️ Eliminar observación: estudiante=$estudianteId")
            onEliminarObservacion(estudianteId)
        }
    }

    // ✅ Métodos para actualizar datos
    fun actualizarAlumnos(nuevosAlumnos: List<AlumnoResponse>) {
        listaAlumnos = nuevosAlumnos.sortedWith(compareBy(
            { it.apellido.lowercase() },   // ordena por apellido
            { it.nombre.lowercase() }      // y si son iguales, por nombre
        ))
        notifyDataSetChanged()
    }

    fun actualizarObservaciones(nuevasObservaciones: Map<Int, Observacion>) {
        observacionesExistentes = nuevasObservaciones
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = listaAlumnos.size

    // ✅ Clase de datos para los estados
    data class EstadoObservacion(
        val valor: Int,
        val nombre: String,
        val icono: String,
        val color: Int
    )
}