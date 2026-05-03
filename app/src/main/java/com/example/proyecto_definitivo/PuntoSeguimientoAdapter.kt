package com.example.proyecto_definitivo // Tu paquete actual

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Nota: Eliminamos el import viejo de calc_definitiva.
// Como PuntoSeguimientoUI ya está en este mismo paquete, no necesitas importarlo.

class PuntoSeguimientoAdapter(
    private val listaPuntos: List<PuntoSeguimientoUI>
) : RecyclerView.Adapter<PuntoSeguimientoAdapter.PuntoSeguimientoViewHolder>() { // <-- ¡ESTA LÍNEA ESTABA SUCIA!

    class PuntoSeguimientoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutItemPuntoSeguimiento: LinearLayout =
            itemView.findViewById(R.id.layoutItemPuntoSeguimiento)
        val tvNombrePuntoSeguimiento: TextView =
            itemView.findViewById(R.id.tvNombrePuntoSeguimiento)
        val tvTipoPuntoSeguimiento: TextView =
            itemView.findViewById(R.id.tvTipoPuntoSeguimiento)
        val tvEstadoPuntoSeguimiento: TextView =
            itemView.findViewById(R.id.tvEstadoPuntoSeguimiento)
        val tvTiempoDesdeAnteriorSeguimiento: TextView =
            itemView.findViewById(R.id.tvTiempoDesdeAnteriorSeguimiento)
        val tvTiempoAcumuladoSeguimiento: TextView =
            itemView.findViewById(R.id.tvTiempoAcumuladoSeguimiento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PuntoSeguimientoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_punto_seguimiento, parent, false)
        return PuntoSeguimientoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PuntoSeguimientoViewHolder, position: Int) {
        val punto = listaPuntos[position]

        val context = holder.itemView.context
        holder.tvNombrePuntoSeguimiento.text = "${punto.orden}. ${punto.nombre}"
        holder.tvTipoPuntoSeguimiento.text = context.getString(R.string.label_type, punto.tipo)

        when {
            punto.completado -> {
                holder.tvEstadoPuntoSeguimiento.text = context.getString(R.string.state_completed)
                holder.tvEstadoPuntoSeguimiento.setTextColor(Color.parseColor("#1B5E20"))
                holder.layoutItemPuntoSeguimiento.setBackgroundColor(Color.parseColor("#C8E6C9"))
            }

            punto.esSiguiente -> {
                holder.tvEstadoPuntoSeguimiento.text = context.getString(R.string.state_next)
                holder.tvEstadoPuntoSeguimiento.setTextColor(Color.parseColor("#0D47A1"))
                holder.layoutItemPuntoSeguimiento.setBackgroundColor(Color.parseColor("#BBDEFB"))
            }

            else -> {
                holder.tvEstadoPuntoSeguimiento.text = context.getString(R.string.state_pending)
                holder.tvEstadoPuntoSeguimiento.setTextColor(Color.parseColor("#616161"))
                holder.layoutItemPuntoSeguimiento.setBackgroundColor(Color.parseColor("#F5F5F5"))
            }
        }

        holder.tvTiempoDesdeAnteriorSeguimiento.text =
            if (punto.completado) {
                context.getString(R.string.time_from_previous, formatearDuracion(punto.tiempoDesdeAnteriorMs))
            } else {
                context.getString(R.string.time_from_previous_empty)
            }

        holder.tvTiempoAcumuladoSeguimiento.text =
            if (punto.completado) {
                context.getString(R.string.accumulated_time, formatearDuracion(punto.tiempoAcumuladoRutaMs))
            } else {
                context.getString(R.string.accumulated_time_empty)
            }
    }

    override fun getItemCount(): Int = listaPuntos.size

    private fun formatearDuracion(ms: Long): String {
        val segundos = ms / 1000
        val horas = segundos / 3600
        val minutos = (segundos % 3600) / 60
        val seg = segundos % 60
        return String.format("%02d:%02d:%02d", horas, minutos, seg)
    }
}