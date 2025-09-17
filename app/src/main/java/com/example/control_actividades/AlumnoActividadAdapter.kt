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

class AlumnoActividadAdapter(
    private var alumnos: MutableList<AlumnoActividad>,
    private val actividadId: Int,
    private val ApiService: ApiService,
    private val onEstadoChanged: (AlumnoActividad, String) -> Unit
) : RecyclerView.Adapter<AlumnoActividadAdapter.AlumnoViewHolder>() {

    class AlumnoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val spinnerEstado: Spinner = itemView.findViewById(R.id.spinnerEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlumnoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alumno_dos, parent, false)
        return AlumnoViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlumnoViewHolder, position: Int) {
        val alumno = alumnos[position]
        holder.tvNombre.text = "${alumno.nombre} ${alumno.apellido}"

        val estados = listOf("pendiente", "entregado", "no entregado")

        val spinnerAdapter = ArrayAdapter(
            holder.itemView.context,
            android.R.layout.simple_spinner_item,
            estados
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.spinnerEstado.adapter = spinnerAdapter

        // 🔹 PRIMERO removemos cualquier listener anterior
        holder.spinnerEstado.onItemSelectedListener = null

        // Seleccionar el estado actual
        val currentIndex = estados.indexOf(alumno.estado)
        holder.spinnerEstado.setSelection(if (currentIndex != -1) currentIndex else 0, false)

        // Cambiar color del nombre según el estado actual
        cambiarColorTexto(holder, alumno.estado)

        holder.spinnerEstado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val nuevoEstado = estados[pos]

                if (nuevoEstado != alumno.estado) {
                    val estadoAnterior = alumno.estado
                    alumno.estado = nuevoEstado
                    cambiarColorTexto(holder, nuevoEstado)
                    onEstadoChanged(alumno, nuevoEstado)

                    val actualizacion = ActualizacionEstado(
                        estudianteId = alumno.idEstudiante,
                        actividadId = actividadId,
                        nuevoEstado = nuevoEstado
                    )

                    // 🔹 Usar coroutine para llamar al endpoint suspend
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response = ApiService.actualizarEstadoEstudiante(actualizacion)

                            withContext(Dispatchers.Main) {
                                if (response.success) {
                                    // Actualizar el item en la lista principal
                                    val currentPos = holder.adapterPosition
                                    if (currentPos != RecyclerView.NO_POSITION && currentPos < alumnos.size) {
                                        alumnos[currentPos] = alumno.copy(estado = nuevoEstado)
                                        notifyItemChanged(currentPos)
                                    }
                                    Log.d("AlumnoAdapter", "✅ Estado actualizado correctamente en servidor")
                                } else {
                                    // Revertir cambio si la API responde con error
                                    alumno.estado = estadoAnterior
                                    val indexAnterior = estados.indexOf(estadoAnterior)
                                    holder.spinnerEstado.setSelection(indexAnterior, false)
                                    cambiarColorTexto(holder, estadoAnterior)
                                    Log.e("AlumnoAdapter", "❌ Error API: ${response.message}")
                                }
                            }
                        } catch (e: Exception) {
                            // Revertir si hubo error de conexión
                            withContext(Dispatchers.Main) {
                                alumno.estado = estadoAnterior
                                val indexAnterior = estados.indexOf(estadoAnterior)
                                holder.spinnerEstado.setSelection(indexAnterior, false)
                                cambiarColorTexto(holder, estadoAnterior)
                                Log.e("AlumnoAdapter", "❌ Error de conexión: ${e.message}", e)
                            }
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    // Función para cambiar color según estado
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