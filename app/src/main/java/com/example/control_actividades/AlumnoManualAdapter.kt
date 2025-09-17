package com.example.control_actividades

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.LifecycleOwner



class AlumnoManualAdapter(
    private val context: Context,
    private var alumnos: MutableList<AlumnoResponse>,
    private val idClase: Int,
    private val onEstadoActualizado: () -> Unit,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<AlumnoManualAdapter.AlumnoManualViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlumnoManualViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alumno_manual, parent, false)
        return AlumnoManualViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlumnoManualViewHolder, position: Int) {
        val alumno = alumnos[position]
        val nombreCompleto = "${alumno.nombre} ${alumno.apellido}"

        holder.tvNombre.text = nombreCompleto
        holder.tvMatricula.text = alumno.matricula

        // Configurar RadioButtons circulares
        setupRadioButtons(holder, alumno, position)
    }

    private fun setupRadioButtons(holder: AlumnoManualViewHolder, alumno: AlumnoResponse, position: Int) {
        // Establecer el estado actual
        when (alumno.estado.lowercase()) {
            "presente" -> holder.radioPresente.isChecked = true
            "justificante" -> holder.radioTarde.isChecked = true
            "ausente" -> holder.radioAusente.isChecked = true
            else -> holder.radioAusente.isChecked = true // Por defecto ausente
        }

        // Listeners para cada RadioButton
        holder.radioPresente.setOnClickListener {
            if (holder.radioPresente.isChecked) {
                cambiarEstado(alumno, position, "presente")
            }
        }

        holder.radioTarde.setOnClickListener {
            if (holder.radioTarde.isChecked) {
                cambiarEstado(alumno, position, "justificante")
            }
        }

        holder.radioAusente.setOnClickListener {
            if (holder.radioAusente.isChecked) {
                cambiarEstado(alumno, position, "ausente")
            }
        }
    }

    private fun cambiarEstado(alumno: AlumnoResponse, position: Int, nuevoEstado: String) {
        // Solo actualizar si cambió el estado
        if (nuevoEstado != alumno.estado.lowercase()) {
            // Crear una nueva instancia del alumno con el estado actualizado
            val alumnoActualizado = alumno.copy(estado = nuevoEstado)
            alumnos[position] = alumnoActualizado

            // Mostrar feedback visual inmediato
            Toast.makeText(context, "⏳ Actualizando...", Toast.LENGTH_SHORT).show()

            // Actualizar en el servidor
            actualizarEstadoEnServidor(alumno.id_estudiante, idClase, nuevoEstado)
        }
    }

    override fun getItemCount(): Int = alumnos.size

    private fun actualizarEstadoEnServidor(idAlumno: Int, idClase: Int, nuevoEstado: String) {
        val request = ActualizarEstadoRequest(idAlumno, idClase, nuevoEstado)

        lifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.actualizarEstado(request)
                if (response.isSuccessful) {
                    val estadoTexto = when (nuevoEstado.lowercase()) {
                        "presente" -> "✅ Presente"
                        "justificante" -> "🟡 Justificante"
                        "ausente" -> "❌ Ausente"
                        else -> "Estado actualizado"
                    }
                    Toast.makeText(context, estadoTexto, Toast.LENGTH_SHORT).show()
                    onEstadoActualizado.invoke()
                } else {
                    Toast.makeText(context, "❌ Error al actualizar: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "❌ Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class AlumnoManualViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreAlumnoManual)
        val tvMatricula: TextView = view.findViewById(R.id.tvMatriculaAlumnoManual)
        val radioGroupEstado: RadioGroup = view.findViewById(R.id.radioGroupEstado)
        val radioPresente: RadioButton = view.findViewById(R.id.radioPresente)
        val radioTarde: RadioButton = view.findViewById(R.id.radioTarde)
        val radioAusente: RadioButton = view.findViewById(R.id.radioAusente)
    }

    fun updateAlumnos(nuevaLista: MutableList<AlumnoResponse>) {
        alumnos = nuevaLista
        notifyDataSetChanged()
    }
}