package com.example.proyecto_definitivo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PuntoAdapter(
    private val listaPuntos: List<PuntoRuta>,
    private val onModificarClick: (PuntoRuta) -> Unit,
    private val onEliminarClick: (PuntoRuta) -> Unit
) : RecyclerView.Adapter<PuntoAdapter.PuntoViewHolder>() {

    class PuntoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrden: TextView = itemView.findViewById(R.id.tvOrdenPuntoItem)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombrePuntoItem)
        val tvTipo: TextView = itemView.findViewById(R.id.tvTipoPuntoItem)
        val btnModificar: ImageButton = itemView.findViewById(R.id.btnModificarPunto)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarPunto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PuntoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_punto_ruta, parent, false)
        return PuntoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PuntoViewHolder, position: Int) {
        val punto = listaPuntos[position]

        // Formato "01", "02" como en el diseño
        holder.tvOrden.text = punto.orden.toString().padStart(2, '0')
        holder.tvNombre.text = punto.nombre

        // Texto limpio sin fondos de colores extraños
        val tipoFormateado = when (punto.Tipo.lowercase()) {
            "origen" -> "Inicio / Obligatorio"
            "fin" -> "Destino / Obligatorio"
            "obligatoria" -> "Parada Obligatoria"
            "opcional" -> "Parada Opcional"
            else -> punto.Tipo.replaceFirstChar { it.uppercase() }
        }
        holder.tvTipo.text = tipoFormateado

        holder.btnModificar.setOnClickListener { onModificarClick(punto) }
        holder.btnEliminar.setOnClickListener { onEliminarClick(punto) }
    }

    override fun getItemCount(): Int = listaPuntos.size
}