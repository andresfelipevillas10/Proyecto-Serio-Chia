package com.example.proyecto_definitivo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class RecorridoActivoAdapter(
    private val lista: List<Recorrido>,
    private val onTap: (Recorrido) -> Unit
) : RecyclerView.Adapter<RecorridoActivoAdapter.ViewHolder>() {

    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardRecorridoActivo)
        val tvNombreRuta: TextView = view.findViewById(R.id.tvNombreRuta)
        val tvHoraInicio: TextView = view.findViewById(R.id.tvHoraInicio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recorrido_pasajero, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recorrido = lista[position]
        holder.tvNombreRuta.text = recorrido.rutaNombre

        val horaInicio = if (recorrido.inicioTiempo > 0) {
            "Salida: ${timeFormatter.format(Date(recorrido.inicioTiempo))}"
        } else {
            "En servicio"
        }
        holder.tvHoraInicio.text = horaInicio

        holder.card.setOnClickListener { onTap(recorrido) }
    }

    override fun getItemCount() = lista.size
}
