package com.example.proyecto_definitivo

import android.graphics.Color
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
        val tvTipoBadge: TextView = itemView.findViewById(R.id.tvTipoPuntoItem)
        val tvCoords: TextView = itemView.findViewById(R.id.tvCoordenadasPuntoItem)
        val btnModificar: ImageButton = itemView.findViewById(R.id.btnModificarPunto)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarPunto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PuntoViewHolder {
        // Apuntamos al nuevo diseño
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_punto_ruta, parent, false)
        return PuntoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PuntoViewHolder, position: Int) {
        val punto = listaPuntos[position]

        holder.tvOrden.text = punto.orden.toString()
        holder.tvNombre.text = punto.nombre
        holder.tvCoords.text = "${punto.latitud}°, ${punto.longitud}°"

        // Lógica visual para el Badge
        when (punto.Tipo.lowercase()) {
            "obligatoria", "origen", "fin" -> {
                holder.tvTipoBadge.text = "OBLIG."
                holder.tvTipoBadge.setBackgroundResource(R.drawable.bg_badge_oblig)
                holder.tvTipoBadge.setTextColor(Color.parseColor("#006c49"))
            }
            "opcional", "marca" -> {
                holder.tvTipoBadge.text = "OPC."
                holder.tvTipoBadge.setBackgroundResource(R.drawable.bg_badge_opc)
                holder.tvTipoBadge.setTextColor(Color.parseColor("#737780"))
            }
            else -> {
                holder.tvTipoBadge.text = punto.Tipo.uppercase().take(5)
            }
        }

        holder.btnModificar.setOnClickListener { onModificarClick(punto) }
        holder.btnEliminar.setOnClickListener { onEliminarClick(punto) }
    }

    override fun getItemCount(): Int = listaPuntos.size

}