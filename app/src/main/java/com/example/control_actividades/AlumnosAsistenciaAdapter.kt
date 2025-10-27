package com.example.control_actividades

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class AlumnosAsistenciaAdapter(
    private var listaAlumnos: List<Estudiante>,
    private val onAlumnoClick: (Estudiante) -> Unit
) : RecyclerView.Adapter<AlumnosAsistenciaAdapter.AlumnoViewHolder>() {

    private var listaFiltrada = listaAlumnos.toMutableList()

    inner class AlumnoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardAlumno: CardView = view.findViewById(R.id.cardAlumno)
        val tvInicial: TextView = view.findViewById(R.id.tvInicial)
        val tvNombreAlumno: TextView = view.findViewById(R.id.tvNombreAlumno)
        val tvIdAlumno: TextView = view.findViewById(R.id.tvIdAlumno)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlumnoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contar_asistencia, parent, false)
        return AlumnoViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlumnoViewHolder, position: Int) {
        val alumno = listaFiltrada[position]

        // Configurar nombre completo del alumno
        holder.tvNombreAlumno.text = alumno.nombreCompleto

        // Configurar matrícula del alumno (en lugar de ID)
        holder.tvIdAlumno.text = alumno.matricula

        // Obtener iniciales del nombre
        val iniciales = alumno.getIniciales()
        holder.tvInicial.text = iniciales

        // Colores variados para los avatares según la inicial
        val colorAvatar = getColorForInitial(iniciales.firstOrNull()?.toString() ?: "?")
        holder.tvInicial.parent?.let { parent ->
            if (parent is CardView) {
                parent.setCardBackgroundColor(Color.parseColor(colorAvatar))
            }
        }

        // Click listener para seleccionar el alumno
        holder.cardAlumno.setOnClickListener {
            onAlumnoClick(alumno)
        }

        // Animación de entrada suave
        holder.cardAlumno.alpha = 0f
        holder.cardAlumno.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay((position * 50).toLong())
            .start()
    }

    override fun getItemCount(): Int = listaFiltrada.size

    /**
     * Filtra la lista de alumnos por nombre o matrícula
     */
    fun filtrar(query: String) {
        listaFiltrada = if (query.isEmpty()) {
            listaAlumnos.toMutableList()
        } else {
            listaAlumnos.filter { alumno ->
                alumno.nombreCompleto.contains(query, ignoreCase = true) ||
                        alumno.matricula.contains(query, ignoreCase = true) ||
                        alumno.nombre.contains(query, ignoreCase = true) ||
                        alumno.apellido.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    /**
     * Actualiza la lista completa de alumnos
     */
    fun actualizarLista(nuevaLista: List<Estudiante>) {
        listaAlumnos = nuevaLista
        listaFiltrada = nuevaLista.toMutableList()
        notifyDataSetChanged()
    }

    /**
     * Obtiene el tamaño de la lista filtrada
     */
    fun getListaFiltradaSize(): Int = listaFiltrada.size

    /**
     * Retorna un color basado en la inicial del nombre
     */
    private fun getColorForInitial(inicial: String): String {
        return when (inicial.uppercase()) {
            "A", "N" -> "#9B59B6" // Morado
            "B", "O" -> "#3498DB" // Azul
            "C", "P" -> "#E74C3C" // Rojo
            "D", "Q" -> "#2ECC71" // Verde
            "E", "R" -> "#F39C12" // Naranja
            "F", "S" -> "#1ABC9C" // Turquesa
            "G", "T" -> "#E67E22" // Naranja oscuro
            "H", "U" -> "#9B59B6" // Morado
            "I", "V" -> "#34495E" // Gris oscuro
            "J", "W" -> "#16A085" // Verde azulado
            "K", "X" -> "#C0392B" // Rojo oscuro
            "L", "Y" -> "#8E44AD" // Morado oscuro
            "M", "Z" -> "#27AE60" // Verde oscuro
            else -> "#95A5A6" // Gris por defecto
        }
    }
}