package com.example.control_actividades

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class ActividadAdapter(
    private val actividades: List<Actividad>,
    private val onEditarClick: (Actividad) -> Unit,
    private val onEliminarClick: (Actividad) -> Unit,
    private val onItemClick: (Actividad) -> Unit,
    private val onQRClick: (Actividad) -> Unit,
    private val onVerClick: (Actividad) -> Unit
) : RecyclerView.Adapter<ActividadAdapter.ActividadViewHolder>() {
    private var selectedPosition: Int = RecyclerView.NO_POSITION

    inner class ActividadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Views principales
        val cardActividad: MaterialCardView = itemView.findViewById(R.id.cardActividad)
        val iconoActividad: TextView = itemView.findViewById(R.id.iconoActividad)
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvFechaHora: TextView = itemView.findViewById(R.id.tvFechaHora)
        val tvDuracion: TextView = itemView.findViewById(R.id.tvDuracion)
        val tvTipo: TextView = itemView.findViewById(R.id.tvTipo)

        // Estado badge
        val cardEstadoBadge: MaterialCardView = itemView.findViewById(R.id.cardEstadoBadge)
        val tvBadgeEstado: TextView = itemView.findViewById(R.id.tvBadgeEstado)

        // Botones (ahora son MaterialCardView)
        val btnQR: MaterialCardView = itemView.findViewById(R.id.btnQR)
        val btnVerResultados: MaterialCardView = itemView.findViewById(R.id.btnVerResultados)
        val btnEditar: MaterialCardView = itemView.findViewById(R.id.btnEditar)
        val btnEliminar: MaterialCardView = itemView.findViewById(R.id.btnEliminar)

        fun bind(actividad: Actividad, isSelected: Boolean) {
            // Información básica
            tvTitulo.text = actividad.titulo
            tvDescripcion.text = actividad.descripcion ?: "Sin descripción"

            // Configurar icono (puedes personalizar según el tipo de actividad)
            iconoActividad.text = when {
                actividad.titulo?.contains("examen", true) == true -> "📝"
                actividad.titulo?.contains("tarea", true) == true -> "📚"
                actividad.titulo?.contains("proyecto", true) == true -> "🏗️"
                else -> "📋"
            }

            // Fecha y hora (usando los campos reales)
            tvFechaHora.text = "Entrega: ${actividad.fecha_entrega}"
            if (actividad.hora_entrega != null) {
                tvFechaHora.text = "${tvFechaHora.text} ${actividad.hora_entrega}"
            }

            // Duración y tipo (usando campos reales)
            tvDuracion.text = "Creada: ${actividad.fecha_creacion}"
            tvTipo.text = "Valor: ${actividad.valor_maximo ?: 0} pts"

            // Estado badge (usando el campo estado real)
            val estadoTexto = when (actividad.estado?.lowercase()) {
                "activa", "vigente" -> "ACTIVA"
                "vencida", "finalizada" -> "VENCIDA"
                "pendiente" -> "PENDIENTE"
                null -> "SIN ESTADO"
                else -> actividad.estado.uppercase()
            }
            tvBadgeEstado.text = estadoTexto

            val badgeColor = when (actividad.estado?.lowercase()) {
                "activa", "vigente" -> itemView.context.getColor(android.R.color.holo_green_dark)
                "vencida", "finalizada" -> itemView.context.getColor(android.R.color.holo_red_dark)
                "pendiente" -> itemView.context.getColor(android.R.color.holo_orange_dark)
                else -> itemView.context.getColor(android.R.color.darker_gray)
            }
            cardEstadoBadge.setCardBackgroundColor(badgeColor)

            // Cambiar color de la card principal si está seleccionado
            if (isSelected) {
                cardActividad.setCardBackgroundColor(
                    itemView.context.getColor(R.color.verdePresente)
                )
                cardActividad.alpha = 0.9f
            } else {
                cardActividad.setCardBackgroundColor(
                    itemView.context.getColor(android.R.color.white)
                )
                cardActividad.alpha = 1.0f
            }

            // Click en la card completa
            cardActividad.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition

                if (previousPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previousPosition)
                }
                notifyItemChanged(selectedPosition)

                onItemClick(actividad)
            }

            // Clicks en los botones circulares
            btnQR.setOnClickListener { onQRClick(actividad) }
            btnVerResultados.setOnClickListener { onVerClick(actividad) }
            btnEditar.setOnClickListener { onEditarClick(actividad) }
            btnEliminar.setOnClickListener { onEliminarClick(actividad) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActividadViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_actividad, parent, false)
        return ActividadViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActividadViewHolder, position: Int) {
        holder.bind(actividades[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = actividades.size
}