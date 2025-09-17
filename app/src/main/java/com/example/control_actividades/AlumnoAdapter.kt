package com.example.control_actividades

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import androidx.core.content.ContextCompat

class AlumnoAdapter(
    private val context: Context,
    private var alumnos: List<AlumnoResponse>,
    private val idClase: Int,
    private val onReload: () -> Unit
) : RecyclerView.Adapter<AlumnoAdapter.AlumnoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlumnoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alumno, parent, false)
        return AlumnoViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlumnoViewHolder, position: Int) {
        val alumno = alumnos[position]
        val nombreCompleto = "${alumno.nombre} ${alumno.apellido}"

        // Configurar información del alumno
        holder.tvNombre.text = nombreCompleto
        holder.tvMatricula.text = alumno.matricula

        // Configurar inicial del avatar
        val inicial = if (nombreCompleto.isNotEmpty()) {
            nombreCompleto.first().uppercaseChar().toString()
        } else {
            "?"
        }
        holder.tvInicial.text = inicial

        // Configurar estado actual
        configurarEstado(holder, alumno.estado)

        // Actualizar color de fondo del Card (mantener blanco limpio)
        actualizarColorCard(holder.itemContainer, alumno.estado)
    }

    override fun getItemCount(): Int = alumnos.size

    private fun configurarEstado(holder: AlumnoViewHolder, estado: String) {
        when (estado.lowercase()) {
            "presente" -> {
                holder.tvEstadoAlumno.text = "Presente"
                holder.cardEstado.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.verdePresente)
                )
                holder.tvEstadoAlumno.setTextColor(
                    ContextCompat.getColor(context, android.R.color.white)
                )
                holder.indicadorEstado.setBackgroundResource(R.drawable.circulo_estado_presente)
            }
            "justificante" -> {
                holder.tvEstadoAlumno.text = "Justificante"
                holder.cardEstado.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.amarilloJustificante)
                )
                holder.tvEstadoAlumno.setTextColor(
                    ContextCompat.getColor(context, android.R.color.black)
                )
                holder.indicadorEstado.setBackgroundResource(R.drawable.circulo_estado_justificado)
            }
            "ausente" -> {
                holder.tvEstadoAlumno.text = "Ausente"
                holder.cardEstado.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.rojoAusente)
                )
                holder.tvEstadoAlumno.setTextColor(
                    ContextCompat.getColor(context, android.R.color.white)
                )
                holder.indicadorEstado.setBackgroundResource(R.drawable.circulo_estado_ausente)
            }
            else -> {
                holder.tvEstadoAlumno.text = "Sin estado"
                holder.cardEstado.setCardBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.darker_gray)
                )
                holder.tvEstadoAlumno.setTextColor(
                    ContextCompat.getColor(context, android.R.color.white)
                )
                holder.indicadorEstado.setBackgroundResource(R.drawable.circulo_estado)
            }
        }
    }

    private fun actualizarColorCard(card: MaterialCardView, estado: String) {
        // Mantener fondo blanco limpio para todas las cards
        card.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white))

        // Opcional: agregar un sutil borde izquierdo según el estado
        // Puedes implementar esto con un drawable personalizado si lo deseas
    }

    class AlumnoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreAlumno)
        val tvMatricula: TextView = view.findViewById(R.id.tvMatriculaAlumno)
        val tvInicial: TextView = view.findViewById(R.id.tvInicialAlumno)
        val tvEstadoAlumno: TextView = view.findViewById(R.id.tvEstadoAlumno)
        val cardEstado: MaterialCardView = view.findViewById(R.id.cardEstado)
        val indicadorEstado: View = view.findViewById(R.id.indicadorEstado)
        val itemContainer: MaterialCardView = view.findViewById(R.id.itemContainer)
    }

    fun updateAlumnos(nuevaLista: List<AlumnoResponse>) {
        alumnos = nuevaLista
        notifyDataSetChanged()
    }
}