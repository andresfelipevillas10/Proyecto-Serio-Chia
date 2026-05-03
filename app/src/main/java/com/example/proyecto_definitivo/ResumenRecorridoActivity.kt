package com.example.proyecto_definitivo
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.FirebaseDatabase

class ResumenRecorridoActivity : AppCompatActivity() {

    private lateinit var tvResumenRutaNombre: TextView
    private lateinit var tvResumenTiempoTotal: TextView
    private lateinit var recyclerResumenPuntos: RecyclerView
    private lateinit var btnVolverInicio: MaterialButton

    private val db = FirebaseDatabase.getInstance().reference
    private var recorridoId: String = ""

    private val listaPuntosCompletados = mutableListOf<PuntoSeguimientoUI>()
    private lateinit var adapter: PuntoSeguimientoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resumen_recorrido)

        // 1. Enlazar vistas
        tvResumenRutaNombre = findViewById(R.id.tvResumenRutaNombre)
        tvResumenTiempoTotal = findViewById(R.id.tvResumenTiempoTotal)
        recyclerResumenPuntos = findViewById(R.id.recyclerResumenPuntos)
        btnVolverInicio = findViewById(R.id.btnVolverInicio)

        // 2. Configurar el RecyclerView con el adaptador que me pasaste
        recyclerResumenPuntos.layoutManager = LinearLayoutManager(this)
        adapter = PuntoSeguimientoAdapter(listaPuntosCompletados)
        recyclerResumenPuntos.adapter = adapter

        // 3. Recibir el ID del recorrido
        recorridoId = intent.getStringExtra("recorridoId") ?: ""

        if (recorridoId.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_recorrido_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 4. Botón para volver al Home y limpiar el historial de navegación
        btnVolverInicio.setOnClickListener {
            val intent = Intent(this, HomeRutasActivity::class.java)
            // Esto asegura que al volver al Home, si el usuario le da "Atrás", la app se cierre en vez de volver al resumen
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // 5. Cargar la data de Firebase
        cargarResumenGeneral()
        cargarPuntosCompletados()
    }

    private fun cargarResumenGeneral() {
        db.child("recorridos").child(recorridoId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val nombre = snapshot.child("rutaNombre").getValue(String::class.java) ?: getString(R.string.unknown_route)

                // Calculamos el tiempo total si no está guardado como tal
                val inicio = snapshot.child("inicioTiempo").getValue(Long::class.java) ?: 0L
                val fin = snapshot.child("finTiempo").getValue(Long::class.java) ?: 0L

                var tiempoTotal = snapshot.child("tiempoTotalMs").getValue(Long::class.java) ?: 0L
                if (tiempoTotal == 0L && fin > 0L) {
                    tiempoTotal = fin - inicio
                }

                tvResumenRutaNombre.text = nombre
                tvResumenTiempoTotal.text = formatearDuracion(tiempoTotal)
            }
        }.addOnFailureListener {
            Toast.makeText(this, getString(R.string.error_loading_summary), Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarPuntosCompletados() {
        db.child("recorridos").child(recorridoId).child("puntosRegistrados").get().addOnSuccessListener { snapshot ->
            listaPuntosCompletados.clear()

            for (puntoSnap in snapshot.children) {
                // Mapeamos los datos de Firebase a la clase PuntoSeguimientoUI que necesita el Adapter
                val puntoUI = PuntoSeguimientoUI(
                    puntoId = puntoSnap.child("puntoId").getValue(String::class.java) ?: "",
                    nombre = puntoSnap.child("nombre").getValue(String::class.java) ?: getString(R.string.unknown_point),
                    orden = puntoSnap.child("orden").getValue(Int::class.java) ?: 0,
                    tipo = puntoSnap.child("tipo").getValue(String::class.java) ?: "marca",
                    latitud = puntoSnap.child("latitud").getValue(Double::class.java) ?: 0.0,
                    longitud = puntoSnap.child("longitud").getValue(Double::class.java) ?: 0.0,
                    completado = true, // Si está en este nodo de BD, es porque el GPS lo detectó
                    tiempoDesdeAnteriorMs = puntoSnap.child("tiempoDesdeAnteriorMs").getValue(Long::class.java) ?: 0L,
                    tiempoAcumuladoRutaMs = puntoSnap.child("tiempoAcumuladoRutaMs").getValue(Long::class.java) ?: 0L
                )
                listaPuntosCompletados.add(puntoUI)
            }

            // Los ordenamos por si Firebase los bajó en desorden
            listaPuntosCompletados.sortBy { it.orden }
            adapter.notifyDataSetChanged()

        }.addOnFailureListener {
            Toast.makeText(this, getString(R.string.error_loading_stops), Toast.LENGTH_SHORT).show()
        }
    }

    // Misma función de formato de tiempo que usaste en tu adaptador
    private fun formatearDuracion(ms: Long): String {
        val segundos = ms / 1000
        val horas = segundos / 3600
        val minutos = (segundos % 3600) / 60
        val seg = segundos % 60
        return String.format("%02d:%02d:%02d", horas, minutos, seg)
    }
}