package com.example.control_actividades

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.Toast

class AlumnoActividadAdapter(
    private var alumnos: MutableList<AlumnoActividad>,
    private val actividadId: Int,
    private val tipoActividad: String,
    private val calificacionMaxima: Int,
    private val apiService: ApiService,
    private val onEstadoChanged: (AlumnoActividad, String) -> Unit
) : RecyclerView.Adapter<AlumnoActividadAdapter.AlumnoViewHolder>() {

    class AlumnoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val spinnerEstado: Spinner = itemView.findViewById(R.id.spinnerEstado)
        val tvInicialAlumno: TextView = itemView.findViewById(R.id.tvInicialAlumno)
        val tvMatricula: TextView = itemView.findViewById(R.id.tvMatricula)
        val tvEstadoActividad: TextView = itemView.findViewById(R.id.tvEstadoActividad)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlumnoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alumno_dos, parent, false)
        return AlumnoViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlumnoViewHolder, position: Int) {
        val alumno = alumnos[position]
        holder.tvNombre.text = "${alumno.nombre} ${alumno.apellido}"
        holder.tvMatricula.text = alumno.matricula
        holder.tvInicialAlumno.text = alumno.nombre.firstOrNull()?.uppercase() ?: "?"

        // 🔹 Actualizar TextView de estado de actividad
        actualizarTextoEstadoActividad(holder, alumno)

        val estados = listOf("pendiente", "entregado", "no entregado")

        val spinnerAdapter = ArrayAdapter(
            holder.itemView.context,
            android.R.layout.simple_spinner_item,
            estados
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.spinnerEstado.adapter = spinnerAdapter

        // Evitar trigger inicial
        holder.spinnerEstado.onItemSelectedListener = null
        holder.spinnerEstado.setSelection(estados.indexOf(alumno.estado).coerceAtLeast(0), false)

        cambiarColorTexto(holder, alumno.estado)

        holder.spinnerEstado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val nuevoEstado = estados[pos]

                // Si el estado no cambió, no hacer nada
                if (nuevoEstado == alumno.estado) return

                val estadoAnterior = alumno.estado

                Log.d("AlumnoAdapter", "🔍 Cambio de estado detectado")
                Log.d("AlumnoAdapter", "   Tipo actividad: '$tipoActividad'")
                Log.d("AlumnoAdapter", "   Nuevo estado: '$nuevoEstado'")
                Log.d("AlumnoAdapter", "   ¿Es examen?: ${tipoActividad.equals("examen", ignoreCase = true)}")

                // Determinar si necesitamos pedir calificación
                if (nuevoEstado == "entregado" && tipoActividad.equals("examen", ignoreCase = true)) {
                    // SOLO para exámenes: pedir calificación manual
                    Log.d("AlumnoAdapter", "✅ Abriendo diálogo de calificación (es examen)")
                    mostrarDialogoCalificacion(holder, alumno, nuevoEstado, estadoAnterior)
                } else if (nuevoEstado == "entregado") {
                    // Para otros tipos de actividad: asignar calificación máxima automáticamente
                    Log.d("AlumnoAdapter", "✅ Asignando calificación automática: $calificacionMaxima (no es examen)")
                    alumno.estado = nuevoEstado
                    cambiarColorTexto(holder, nuevoEstado)
                    actualizarTextoEstadoActividad(holder, alumno)
                    enviarActualizacion(holder, alumno, nuevoEstado, estadoAnterior, calificacionMaxima)
                    onEstadoChanged(alumno, nuevoEstado)
                } else {
                    // Para estados "pendiente" o "no entregado": sin calificación
                    Log.d("AlumnoAdapter", "✅ Cambiando estado sin calificación")
                    alumno.estado = nuevoEstado
                    cambiarColorTexto(holder, nuevoEstado)
                    actualizarTextoEstadoActividad(holder, alumno)
                    enviarActualizacion(holder, alumno, nuevoEstado, estadoAnterior, null)
                    onEstadoChanged(alumno, nuevoEstado)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // 🔹 Nuevo método para actualizar el TextView de estado
    private fun actualizarTextoEstadoActividad(holder: AlumnoViewHolder, alumno: AlumnoActividad) {
        val textoEstado = when (alumno.estado.lowercase()) {
            "entregado" -> {
                if (alumno.calificacion != null) {
                    "Calificación: ${alumno.calificacion}/$calificacionMaxima"
                } else {
                    "Entregado"
                }
            }
            "pendiente" -> "Pendiente de entrega"
            "no entregado" -> "No entregado"
            else -> "Sin registrar actividad"
        }

        holder.tvEstadoActividad.text = textoEstado

        // Color según estado
        val colorRes = when (alumno.estado.lowercase()) {
            "entregado" -> android.R.color.holo_green_dark
            "pendiente" -> android.R.color.holo_orange_dark
            "no entregado" -> android.R.color.holo_red_dark
            else -> android.R.color.darker_gray
        }
        holder.tvEstadoActividad.setTextColor(holder.itemView.context.getColor(colorRes))
    }

    // Mostrar diálogo para asignar calificación (solo para exámenes)
    private fun mostrarDialogoCalificacion(
        holder: AlumnoViewHolder,
        alumno: AlumnoActividad,
        nuevoEstado: String,
        estadoAnterior: String
    ) {
        val input = EditText(holder.itemView.context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Calificación (0 - $calificacionMaxima)"
        }

        AlertDialog.Builder(holder.itemView.context)
            .setTitle("Asignar calificación")
            .setMessage("Ingrese la calificación para ${alumno.nombre}")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Guardar") { dialog, _ ->
                val calificacionStr = input.text.toString().trim()

                if (calificacionStr.isEmpty()) {
                    Toast.makeText(holder.itemView.context, "⚠️ Debe ingresar una calificación", Toast.LENGTH_SHORT).show()
                    revertirCambio(holder, alumno, estadoAnterior)
                    return@setPositiveButton
                }

                val calificacionIngresada = calificacionStr.toIntOrNull()

                if (calificacionIngresada == null || calificacionIngresada < 0 || calificacionIngresada > calificacionMaxima) {
                    Toast.makeText(
                        holder.itemView.context,
                        "⚠️ Calificación inválida. Debe estar entre 0 y $calificacionMaxima",
                        Toast.LENGTH_SHORT
                    ).show()
                    revertirCambio(holder, alumno, estadoAnterior)
                } else {
                    // Actualizar estado y enviar
                    alumno.estado = nuevoEstado
                    cambiarColorTexto(holder, nuevoEstado)
                    actualizarTextoEstadoActividad(holder, alumno)
                    enviarActualizacion(holder, alumno, nuevoEstado, estadoAnterior, calificacionIngresada)
                    onEstadoChanged(alumno, nuevoEstado)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                revertirCambio(holder, alumno, estadoAnterior)
                dialog.dismiss()
            }
            .show()
    }

    // Enviar actualización al backend
    private fun enviarActualizacion(
        holder: AlumnoViewHolder,
        alumno: AlumnoActividad,
        nuevoEstado: String,
        estadoAnterior: String,
        calificacion: Int?
    ) {
        val actualizacion = ActualizacionEstado(
            estudianteId = alumno.idEstudiante,
            actividadId = actividadId,
            nuevoEstado = nuevoEstado,
            calificacion = calificacion
        )

        Log.d("AlumnoAdapter", "📤 Enviando actualización:")
        Log.d("AlumnoAdapter", "   Estado: $nuevoEstado")
        Log.d("AlumnoAdapter", "   Calificación: $calificacion")
        Log.d("AlumnoAdapter", "   Tipo actividad: $tipoActividad")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.actualizarEstadoEstudiante(actualizacion)
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        Log.d("AlumnoAdapter", "✅ Actualización exitosa")
                        Toast.makeText(holder.itemView.context, "✅ Estado actualizado", Toast.LENGTH_SHORT).show()

                        // Actualizar la vista con los datos de la respuesta
                        actualizarTextoEstadoActividad(holder, alumno)
                        notifyItemChanged(holder.adapterPosition)
                    } else {
                        Log.e("AlumnoAdapter", "❌ Error del servidor: ${response.message}")
                        revertirCambio(holder, alumno, estadoAnterior)
                        Toast.makeText(holder.itemView.context, "❌ ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("AlumnoAdapter", "❌ Error de conexión: ${e.message}", e)
                    revertirCambio(holder, alumno, estadoAnterior)
                    Toast.makeText(holder.itemView.context, "❌ Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun revertirCambio(holder: AlumnoViewHolder, alumno: AlumnoActividad, estadoAnterior: String) {
        alumno.estado = estadoAnterior
        val estados = listOf("pendiente", "entregado", "no entregado")
        holder.spinnerEstado.setSelection(estados.indexOf(estadoAnterior), false)
        cambiarColorTexto(holder, estadoAnterior)
        actualizarTextoEstadoActividad(holder, alumno)
        onEstadoChanged(alumno, estadoAnterior)
    }

    private fun cambiarColorTexto(holder: AlumnoViewHolder, estado: String) {
        val colorRes = when (estado.lowercase()) {
            "entregado" -> android.R.color.holo_green_dark
            "pendiente" -> android.R.color.holo_orange_dark
            "no entregado" -> android.R.color.holo_red_dark
            else -> android.R.color.black
        }
        holder.tvNombre.setTextColor(holder.itemView.context.getColor(colorRes))
    }

    fun updateAlumnos(nuevosAlumnos: List<AlumnoActividad>) {
        Log.d("Adapter", "📝 Actualizando adapter con ${nuevosAlumnos.size} elementos")
        alumnos.clear()
        alumnos.addAll(nuevosAlumnos.map { it.copy() })
        notifyDataSetChanged()
        Log.d("Adapter", "✅ Adapter actualizado, ahora tiene ${alumnos.size} elementos")
    }

    override fun getItemCount(): Int = alumnos.size
}