package com.example.control_actividades

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class AvisoAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_LOADING = 1
        private const val TYPE_EMPTY = 2
    }

    private val listaAvisos = mutableListOf<Aviso>()
    private var isLoading = false
    private var hasMorePages = true
    private var isEmpty = false

    // ViewHolder para avisos
    class AvisoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTituloAviso)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionAviso)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFechaAviso)
        val tvEnlace: TextView = itemView.findViewById(R.id.tvEnlaceAviso)
    }

    // ViewHolder para indicador de carga
    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
    }

    // ViewHolder para estado vacío
    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivEmpty: ImageView = itemView.findViewById(R.id.ivEmpty)
        val tvEmptyTitle: TextView = itemView.findViewById(R.id.tvEmptyTitle)
        val tvEmptyMessage: TextView = itemView.findViewById(R.id.tvEmptyMessage)
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            isEmpty && listaAvisos.isEmpty() -> TYPE_EMPTY
            isLoading && position == listaAvisos.size -> TYPE_LOADING
            else -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_LOADING -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_loading, parent, false)
                LoadingViewHolder(view)
            }
            TYPE_EMPTY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_empty, parent, false)
                EmptyViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_aviso, parent, false)
                AvisoViewHolder(view)
            }
        }
    }

    private fun formatearFecha(fechaIso: String): String {
        return try {
            val formatoOriginal = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            formatoOriginal.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val fecha = formatoOriginal.parse(fechaIso)

            val formatoDestino = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            formatoDestino.format(fecha!!)
        } catch (e: Exception) {
            fechaIso
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AvisoViewHolder -> {
                val aviso = listaAvisos[position]

                holder.tvTitulo.text = aviso.nombre_evento
                holder.tvDescripcion.text = aviso.descripcion
                holder.tvFecha.text = formatearFecha(aviso.fecha)

                if (!aviso.enlace.isNullOrBlank()) {
                    holder.tvEnlace.text = "Ver enlace"
                    holder.tvEnlace.visibility = View.VISIBLE
                    holder.tvEnlace.paintFlags = holder.tvEnlace.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG

                    var url = aviso.enlace
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://$url"
                    }

                    holder.tvEnlace.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        holder.tvEnlace.context.startActivity(intent)
                    }
                } else {
                    holder.tvEnlace.visibility = View.GONE
                }
            }
            is LoadingViewHolder -> {
                // El progressBar ya está visible por defecto
            }
            is EmptyViewHolder -> {
                // Configurar el estado vacío
                holder.tvEmptyTitle.text = "No hay avisos disponibles"
                holder.tvEmptyMessage.text = "Cuando se publiquen nuevos avisos, aparecerán aquí"
                // El icono se configura en el XML
            }
        }
    }

    override fun getItemCount(): Int {
        return when {
            isEmpty && listaAvisos.isEmpty() -> 1
            isLoading -> listaAvisos.size + 1 // Siempre que esté cargando, añadimos el item "loading"
            else -> listaAvisos.size
        }
    }

    // Agregar nuevos avisos
    fun addAvisos(nuevosAvisos: List<Aviso>) {
        listaAvisos.addAll(nuevosAvisos)
        isEmpty = listaAvisos.isEmpty()
        notifyDataSetChanged()
    }

    // Limpiar lista
    fun clearAvisos() {
        listaAvisos.clear()
        isEmpty = false
        notifyDataSetChanged()
    }
    // Controlar estado de carga
    fun setLoading(loading: Boolean) {
        isLoading = loading
        notifyDataSetChanged()
    }

    // Controlar si hay más páginas
    fun setHasMorePages(hasMore: Boolean) {
        hasMorePages = hasMore
        if (!hasMore && isLoading) {
            setLoading(false)
        }
    }

    // Mostrar estado vacío
    fun setEmpty(empty: Boolean) {
        isEmpty = empty
        notifyDataSetChanged()
    }

    fun getAvisosCount(): Int = listaAvisos.size
    fun getAllAvisos(): List<Aviso> = listaAvisos.toList()
}