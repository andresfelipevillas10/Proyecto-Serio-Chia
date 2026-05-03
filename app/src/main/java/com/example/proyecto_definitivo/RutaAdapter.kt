package com.example.proyecto_definitivo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class RutaAdapter(
    private val listaRutas: List<Ruta>,
    private val onEditClick: (Ruta) -> Unit,
    private val onDeleteClick: (Ruta) -> Unit,
    private val onStartClick: (Ruta) -> Unit // ¡NUEVO EVENTO!
) : RecyclerView.Adapter<RutaAdapter.RutaViewHolder>() {

    class RutaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvConfigRouteName)
        val tvDetalles: TextView = itemView.findViewById(R.id.tvConfigRouteDetails)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditRoute)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnDeleteRoute)
        val btnIniciar: MaterialButton = itemView.findViewById(R.id.btnStartRoute) // ¡NUEVO BOTÓN!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_config_route_card, parent, false)
        return RutaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RutaViewHolder, position: Int) {
        val ruta = listaRutas[position]

        holder.tvNombre.text = ruta.nombre
        holder.tvDetalles.text = holder.itemView.context.getString(
            R.string.route_details_format,
            ruta.radioDeteccion,
            ruta.descripcion
        )

        // Conectamos los 3 botones
        holder.btnEditar.setOnClickListener { onEditClick(ruta) }
        holder.btnEliminar.setOnClickListener { onDeleteClick(ruta) }
        holder.btnIniciar.setOnClickListener { onStartClick(ruta) }
    }

    override fun getItemCount(): Int = listaRutas.size
}