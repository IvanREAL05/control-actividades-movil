package com.example.control_actividades

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter para las clases de cada día
class ClaseHorarioAdapter(
    private val clases: List<ClaseCompleta>,
    private val onClaseClick: (ClaseCompleta) -> Unit
) : RecyclerView.Adapter<ClaseHorarioAdapter.ClaseViewHolder>() {

    class ClaseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtHoraInicio: TextView = view.findViewById(R.id.txtHoraInicio)
        val txtHoraFin: TextView = view.findViewById(R.id.txtHoraFin)
        val txtNombreClase: TextView = view.findViewById(R.id.txtNombreClase)
        val txtNRC: TextView = view.findViewById(R.id.txtNRC)
        val txtMateria: TextView = view.findViewById(R.id.txtMateria)
        val txtGrupo: TextView = view.findViewById(R.id.txtGrupo)
        val txtAula: TextView = view.findViewById(R.id.txtAula)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClaseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clase_horario, parent, false)
        return ClaseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClaseViewHolder, position: Int) {
        val clase = clases[position]

        // Configurar horario
        holder.txtHoraInicio.text = clase.hora_inicio
        holder.txtHoraFin.text = clase.hora_fin

        // Configurar información de la clase
        holder.txtNombreClase.text = clase.nombre_clase
        holder.txtNRC.text = "NRC: ${clase.nrc}"
        holder.txtMateria.text = "${clase.materia_clave} • ${clase.materia}"
        holder.txtGrupo.text = "Grupo ${clase.grupo} • ${clase.turno}"
        holder.txtAula.text = clase.aula

        // Click listener
        holder.itemView.setOnClickListener {
            onClaseClick(clase)
        }

        // Agregar efecto visual al hacer clic (opcional)
        holder.itemView.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.7f
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1.0f
                }
            }
            false
        }
    }

    override fun getItemCount() = clases.size
}