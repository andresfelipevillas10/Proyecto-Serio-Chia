package com.example.proyecto_definitivo


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
// import com.example.proyectochia.Ruta // Asegúrate de importar tu clase Ruta

class RutaRecorridoAdapter(
    private val listaRutas: List<Ruta>,
    private val onIniciarClick: (Ruta) -> Unit
) : RecyclerView.Adapter<RutaRecorridoAdapter.RutaRecorridoViewHolder>() {

    class RutaRecorridoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Apuntamos a los IDs del nuevo diseño de tarjeta
        val tvNombreRuta: TextView = itemView.findViewById(R.id.tvRouteName)
        val tvDescripcionRuta: TextView = itemView.findViewById(R.id.tvRouteDetails)
        val btnIniciar: MaterialButton = itemView.findViewById(R.id.btnStartRoute)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutaRecorridoViewHolder {
        // Usamos un nuevo layout específico para la lista de recorridos
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_card, parent, false)
        return RutaRecorridoViewHolder(view)
    }

    override fun onBindViewHolder(holder: RutaRecorridoViewHolder, position: Int) {
        val ruta = listaRutas[position]

        holder.tvNombreRuta.text = ruta.nombre

        // Unimos la descripción y el radio para que se vea más limpio
        holder.tvDescripcionRuta.text = "${ruta.descripcion} • Radio: ${ruta.radioDeteccion}m"

        // El botón ahora dispara el evento para ir al "Pre-Recorrido"
        holder.btnIniciar.setOnClickListener {
            onIniciarClick(ruta)
        }
    }

    override fun getItemCount(): Int = listaRutas.size
}