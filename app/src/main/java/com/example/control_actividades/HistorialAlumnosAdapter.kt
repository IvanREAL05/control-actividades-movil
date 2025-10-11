package com.example.control_actividades

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlin.math.roundToInt
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan

class HistorialAlumnosAdapter(
    private val alumnos: List<AlumnoHistorialCompleto>,
    private val totalActividades: Int,
    private val totalPonderacion: Int,
    private val onVerDetallesClick: (AlumnoHistorialCompleto) -> Unit = {}
) : RecyclerView.Adapter<HistorialAlumnosAdapter.AlumnoViewHolder>() {

    // Mapa para rastrear qué elementos están expandidos
    private val expandedPositions = mutableSetOf<Int>()

    inner class AlumnoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // Views del header
        private val layoutHeader: View = itemView.findViewById(R.id.layoutHeader)
        private val tvIniciales: TextView = itemView.findViewById(R.id.tvIniciales)
        private val tvNombreAlumno: TextView = itemView.findViewById(R.id.tvNombreAlumnoHistorial)
        private val tvProgreso: TextView = itemView.findViewById(R.id.tvProgresoActividades)
        private val tvPromedioCalif: TextView = itemView.findViewById(R.id.tvPromedioCalif)
        private val ivExpandir: ImageView = itemView.findViewById(R.id.ivExpandir)

        // Views de la sección expandible
        private val layoutDetalles: View = itemView.findViewById(R.id.layoutDetallesExpandible)
        private val tvTotalPuntos: TextView = itemView.findViewById(R.id.tvTotalPuntos)
        private val recyclerActividades: RecyclerView = itemView.findViewById(R.id.recyclerActividadesAlumno)
        private val tvEntregadas: TextView = itemView.findViewById(R.id.tvEntregadas)
        private val tvPendientes: TextView = itemView.findViewById(R.id.tvPendientes)
        private val btnVerDetalles: MaterialButton = itemView.findViewById(R.id.btnVerDetalles)

        fun bind(alumno: AlumnoHistorialCompleto, position: Int) {
            // Configurar header
            configurarHeader(alumno)

            // Configurar sección expandible
            configurarSeccionExpandible(alumno)

            // Configurar estado de expansión
            val isExpanded = expandedPositions.contains(position)
            layoutDetalles.visibility = if (isExpanded) View.VISIBLE else View.GONE
            ivExpandir.rotation = if (isExpanded) 180f else 0f

            // Manejar click para expandir/contraer
            layoutHeader.setOnClickListener {
                toggleExpansion(position)
            }

            // Botón ver detalles
            btnVerDetalles.setOnClickListener {
                onVerDetallesClick(alumno)
            }
        }

        private fun configurarHeader(alumno: AlumnoHistorialCompleto) {
            // Iniciales del alumno
            val iniciales = "${alumno.nombre.firstOrNull() ?: ""}${alumno.apellido.firstOrNull() ?: ""}".uppercase()
            tvIniciales.text = iniciales

            // Nombre completo
            // Nombre completo con apellido resaltado en naranja
            val nombreCompleto = "${alumno.apellido} ${alumno.nombre}"
            val spannable = SpannableString(nombreCompleto)
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#FFA500")), // Naranja
                0,
                alumno.apellido.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            tvNombreAlumno.text = spannable

            // Progreso básico
            tvProgreso.text = "📚 ${alumno.actividadesEntregadas}/${alumno.totalActividades} actividades • 🏆 ${alumno.puntosObtenidos} pts"

            // Calificación promedio (escala 0-10)
            val promedio = if (alumno.puntosTotales > 0) {
                ((alumno.puntosObtenidos.toFloat() / alumno.puntosTotales) * 10 * 10).roundToInt() / 10.0
            } else 0.0

            tvPromedioCalif.text = promedio.toString()

            // Color de la calificación según el rendimiento
            val colorCalif = when {
                promedio >= 9.0 -> android.graphics.Color.parseColor("#4CAF50") // Verde
                promedio >= 7.0 -> android.graphics.Color.parseColor("#FF9800") // Naranja
                else -> android.graphics.Color.parseColor("#F44336") // Rojo
            }
            tvPromedioCalif.setTextColor(colorCalif)
        }

        private fun configurarSeccionExpandible(alumno: AlumnoHistorialCompleto) {
            // Total de puntos
            tvTotalPuntos.text = "Total: ${alumno.puntosObtenidos}/${alumno.puntosTotales} pts"

            // Configurar RecyclerView de actividades
            recyclerActividades.layoutManager = LinearLayoutManager(itemView.context)
            val actividadesAdapter = ActividadesAlumnoAdapter(alumno.actividades)
            recyclerActividades.adapter = actividadesAdapter

            // Estadísticas
            val pendientes = alumno.totalActividades - alumno.actividadesEntregadas
            tvEntregadas.text = "✅ ${alumno.actividadesEntregadas} entregadas"
            tvPendientes.text = "⏳ $pendientes pendientes"
        }

        private fun toggleExpansion(position: Int) {
            val isExpanded = expandedPositions.contains(position)

            if (isExpanded) {
                // Contraer
                expandedPositions.remove(position)
                layoutDetalles.visibility = View.GONE
                rotateIcon(ivExpandir, 180f, 0f)
            } else {
                // Expandir
                expandedPositions.add(position)
                layoutDetalles.visibility = View.VISIBLE
                rotateIcon(ivExpandir, 0f, 180f)
            }
        }

        private fun rotateIcon(imageView: ImageView, fromDegrees: Float, toDegrees: Float) {
            val rotateAnimator = ObjectAnimator.ofFloat(imageView, "rotation", fromDegrees, toDegrees)
            rotateAnimator.duration = 200
            rotateAnimator.start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlumnoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alumno_historial, parent, false)
        return AlumnoViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlumnoViewHolder, position: Int) {
        holder.bind(alumnos[position], position)
    }

    override fun getItemCount() = alumnos.size
}

// Adapter para las actividades individuales de cada alumno
class ActividadesAlumnoAdapter(
    private val actividades: List<ActividadDetalle>
) : RecyclerView.Adapter<ActividadesAlumnoAdapter.ActividadViewHolder>() {

    inner class ActividadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTituloActividad: TextView = itemView.findViewById(R.id.tvTituloActividad)
        private val tvEstadoActividad: TextView = itemView.findViewById(R.id.tvEstadoActividad)
        private val tvPuntosActividad: TextView = itemView.findViewById(R.id.tvPuntosActividad)

        fun bind(actividad: ActividadDetalle) {
            tvTituloActividad.text = actividad.titulo

            // Estado con emoji y color
            when (actividad.estado) {
                "entregado" -> {
                    tvEstadoActividad.text = "✅ Entregado"
                    tvEstadoActividad.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                    val puntos = actividad.calificacion ?: 0
                    tvPuntosActividad.text = "$puntos/${actividad.valor_maximo} pts"
                    tvPuntosActividad.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                }
                else -> {
                    tvEstadoActividad.text = "⏳ Pendiente"
                    tvEstadoActividad.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                    tvPuntosActividad.text = "0/${actividad.valor_maximo} pts"
                    tvPuntosActividad.setTextColor(android.graphics.Color.parseColor("#666666"))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActividadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_actividad_alumno, parent, false)
        return ActividadViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActividadViewHolder, position: Int) {
        holder.bind(actividades[position])
    }

    override fun getItemCount() = actividades.size
}