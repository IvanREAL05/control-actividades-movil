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
        private val tvEstadoIndicador: TextView = itemView.findViewById(R.id.tvEstadoIndicador)

        // 🔥 NUEVOS VIEWS - Chips y barra de progreso
        private val tvChipPorcentaje: TextView? = itemView.findViewById(R.id.tvChipPorcentaje)
        private val viewProgreso: View = itemView.findViewById(R.id.viewProgreso)

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
            // Iniciales
            val iniciales = "${alumno.nombre.firstOrNull() ?: ""}${alumno.apellido.firstOrNull() ?: ""}".uppercase()
            tvIniciales.text = iniciales

            // Nombre completo
            val nombreCompleto = "${alumno.apellido} ${alumno.nombre}"
            tvNombreAlumno.text = nombreCompleto

            // ✅ VERSIÓN VIEJA: Progreso simple
            tvProgreso.text = "${alumno.actividadesEntregadas} de ${alumno.totalActividades} actividades"

            // ✅ VERSIÓN VIEJA: Promedio escala 0-100 (enteros)
            val promedio = if (alumno.puntosTotales > 0) {
                (alumno.puntosObtenidos * 100) / alumno.puntosTotales
            } else 0

            tvPromedioCalif.text = promedio.toString()

            // Color según rendimiento
            val colorCalif = when {
                promedio >= 90 -> Color.parseColor("#4CAF50") // Verde
                promedio >= 70 -> Color.parseColor("#FF9800") // Naranja
                else -> Color.parseColor("#F44336") // Rojo
            }
            tvPromedioCalif.setTextColor(colorCalif)

            // ✅ Porcentaje de progreso (versión vieja)
            val porcentajeProgreso = if (alumno.totalActividades > 0) {
                (alumno.actividadesEntregadas * 100) / alumno.totalActividades
            } else 0

            tvChipPorcentaje?.text = "📚 $porcentajeProgreso%"

            // Animación de barra (si existe)
            viewProgreso?.let { barra ->
                val layoutParams = barra.layoutParams
                layoutParams.width = 0
                barra.layoutParams = layoutParams

                barra.post {
                    val parentWidth = (barra.parent as? View)?.width ?: itemView.width
                    val targetWidth = (parentWidth * porcentajeProgreso / 100)
                    val animator = ObjectAnimator.ofInt(targetWidth)
                    animator.duration = 500
                    animator.addUpdateListener {
                        layoutParams.width = it.animatedValue as Int
                        barra.layoutParams = layoutParams
                    }
                    animator.start()
                }
            }

            // Badge de estrella
            tvEstadoIndicador?.let {
                if (promedio >= 90) {
                    it.visibility = View.VISIBLE
                    it.text = "⭐"
                } else {
                    it.visibility = View.GONE
                }
            }
        }

        private fun configurarSeccionExpandible(alumno: AlumnoHistorialCompleto) {
            // ✅ VERSIÓN VIEJA: Total de puntos
            tvTotalPuntos.text = "🏆 ${alumno.puntosObtenidos}/${alumno.puntosTotales} pts"

            // RecyclerView de actividades
            recyclerActividades.layoutManager = LinearLayoutManager(itemView.context)
            val actividadesAdapter = ActividadesAlumnoAdapter(alumno.actividades)
            recyclerActividades.adapter = actividadesAdapter

            // ✅ VERSIÓN VIEJA: Estadísticas
            val pendientes = alumno.totalActividades - alumno.actividadesEntregadas
            tvEntregadas.text = alumno.actividadesEntregadas.toString()
            tvPendientes.text = "$pendientes actividades"
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
            rotateAnimator.duration = 300
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

            // ✅ VERSIÓN VIEJA: Estados simples
            when (actividad.estado?.lowercase()) {
                "entregado" -> {
                    val puntos = actividad.calificacion ?: 0
                    tvEstadoActividad.text = "✅ Entregado"
                    tvEstadoActividad.setTextColor(Color.parseColor("#4CAF50"))
                    tvPuntosActividad.text = "$puntos/${actividad.valor_maximo} pts"
                    tvPuntosActividad.setTextColor(Color.parseColor("#4CAF50"))
                }
                "pendiente" -> {
                    tvEstadoActividad.text = "⏳ Pendiente"
                    tvEstadoActividad.setTextColor(Color.parseColor("#FFC107"))
                    tvPuntosActividad.text = "0/${actividad.valor_maximo} pts"
                    tvPuntosActividad.setTextColor(Color.parseColor("#999999"))
                }
                else -> {
                    tvEstadoActividad.text = "❌ No entregado"
                    tvEstadoActividad.setTextColor(Color.parseColor("#F44336"))
                    tvPuntosActividad.text = "0/${actividad.valor_maximo} pts"
                    tvPuntosActividad.setTextColor(Color.parseColor("#999999"))
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