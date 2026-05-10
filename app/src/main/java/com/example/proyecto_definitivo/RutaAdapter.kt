package com.example.proyecto_definitivo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

import androidx.recyclerview.widget.DiffUtil

/**
 * Adapter para la lista de rutas en la pantalla de configuración.
 *
 * Cada tarjeta muestra el nombre, detalles, y cuatro acciones:
 * - ★ Fijar/desfijar como ruta frecuente
 * - 🗑 Eliminar la ruta
 * - ✏ Editar los puntos de la ruta
 * - ▶ Iniciar recorrido
 */
class RutaAdapter(
    private var listaRutas: List<Ruta>,
    private val onEditClick: (Ruta) -> Unit,
    private val onDeleteClick: (Ruta) -> Unit,
    private val onStartClick: (Ruta) -> Unit,
    private val onPinClick: ((Ruta) -> Unit)? = null
) : RecyclerView.Adapter<RutaAdapter.RutaViewHolder>() {

    class RutaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvConfigRouteName)
        val tvDetalles: TextView = itemView.findViewById(R.id.tvConfigRouteDetails)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditRoute)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnDeleteRoute)
        val btnIniciar: MaterialButton = itemView.findViewById(R.id.btnStartRoute)
        val btnPin: ImageButton = itemView.findViewById(R.id.btnPinRoute)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_config_route_card, parent, false)
        return RutaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RutaViewHolder, position: Int) {
        val ruta = listaRutas[position]
        val context = holder.itemView.context

        holder.tvNombre.text = ruta.nombre
        holder.tvDetalles.text = context.getString(
            R.string.route_details_format,
            ruta.radioDeteccion,
            ruta.descripcion
        )

        // Actualizar ícono de pin según el estado
        val isPinned = FrecuentesManager.isPinned(context, ruta.id)
        holder.btnPin.alpha = if (isPinned) 1.0f else 0.4f

        // Conectamos los 4 botones
        holder.btnEditar.setOnClickListener { onEditClick(ruta) }
        holder.btnEliminar.setOnClickListener { onDeleteClick(ruta) }
        holder.btnIniciar.setOnClickListener { onStartClick(ruta) }
        holder.btnPin.setOnClickListener {
            onPinClick?.invoke(ruta)
        }
    }

    override fun getItemCount(): Int = listaRutas.size

    // 🔥 SOLUCIÓN PARPADEO: DiffUtil para actualizaciones fluidas
    fun actualizarRutas(nuevasRutas: List<Ruta>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = listaRutas.size
            override fun getNewListSize() = nuevasRutas.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return listaRutas[oldItemPosition].id == nuevasRutas[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = listaRutas[oldItemPosition]
                val new = nuevasRutas[newItemPosition]
                return old.nombre == new.nombre &&
                       old.descripcion == new.descripcion &&
                       old.radioDeteccion == new.radioDeteccion
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.listaRutas = ArrayList(nuevasRutas)
        diffResult.dispatchUpdatesTo(this)
    }
}