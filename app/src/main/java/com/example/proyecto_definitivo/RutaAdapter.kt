package com.example.proyecto_definitivo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RutaAdapter(
    private val listaRutas: List<Ruta>,
    private val onEditClick: (Ruta) -> Unit, // Para abrir el Paso 1
    private val onDeleteClick: (Ruta) -> Unit // Para borrarla
) : RecyclerView.Adapter<RutaAdapter.RutaViewHolder>() {

    class RutaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvConfigRouteName)
        val tvDetalles: TextView = itemView.findViewById(R.id.tvConfigRouteDetails)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditRoute)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnDeleteRoute)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutaViewHolder {
        // Apuntamos al nuevo diseño
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_config_route_card, parent, false)
        return RutaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RutaViewHolder, position: Int) {
        val ruta = listaRutas[position]

        holder.tvNombre.text = ruta.nombre
        // Concatenamos para imitar el diseño "12 Paradas • 15 min" (Por ahora ponemos el radio)
        holder.tvDetalles.text = "Radio: ${ruta.radioDeteccion}m • ${ruta.descripcion}"

        holder.btnEditar.setOnClickListener { onEditClick(ruta) }
        holder.btnEliminar.setOnClickListener { onDeleteClick(ruta) }
    }

    override fun getItemCount(): Int = listaRutas.size
}