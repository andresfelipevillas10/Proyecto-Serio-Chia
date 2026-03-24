package com.example.proyecto_definitivo // Tu paquete

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HomeRutasActivity : AppCompatActivity() {

    private lateinit var btnConfigRutas: MaterialCardView
    private lateinit var btnSoporte: MaterialCardView
    private lateinit var rvRoutes: RecyclerView

    private lateinit var tabRecorrido: LinearLayout
    private lateinit var tabStats: LinearLayout
    private lateinit var tabSettings: LinearLayout

    // 🔥 Variables para la Base de Datos y el Adaptador
    private val db = FirebaseDatabase.getInstance().reference
    private val listaRutasAccesoRapido = mutableListOf<Ruta>()
    private lateinit var adapterRutas: RutaRecorridoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_rutas)

        btnConfigRutas = findViewById(R.id.btnConfigRutas)
        btnSoporte = findViewById(R.id.btnSoporte)
        rvRoutes = findViewById(R.id.rvRoutes)

        tabRecorrido = findViewById(R.id.tabRecorrido)
        tabStats = findViewById(R.id.tabStats)
        tabSettings = findViewById(R.id.tabSettings)

        // 1. Configurar la lista de Acceso Rápido
        configurarRecyclerView()

        // 2. Configurar los botones estáticos
        setupClickListeners()

        // 3. Descargar las rutas para mostrarlas en el Home
        cargarRutasDeAccesoRapido()
    }

    private fun configurarRecyclerView() {
        rvRoutes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false) // Formato Carrusel (Horizontal)

        adapterRutas = RutaRecorridoAdapter(listaRutasAccesoRapido) { rutaSeleccionada ->
            // Cuando el conductor toca el botón "INICIAR" en la tarjeta del Home:
            val intent = Intent(this, PreRecorridoActivity::class.java)
            intent.putExtra("rutaId", rutaSeleccionada.id)
            intent.putExtra("rutaNombre", rutaSeleccionada.nombre)
            intent.putExtra("rutaRadio", rutaSeleccionada.radioDeteccion)
            startActivity(intent)
        }
        rvRoutes.adapter = adapterRutas
    }

    private fun cargarRutasDeAccesoRapido() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (currentUserId.isEmpty()) return

        // Consultamos la base de datos para traer las rutas disponibles
        db.child("rutas").child(currentUserId).limitToFirst(5).get().addOnSuccessListener { snapshot ->
            listaRutasAccesoRapido.clear()

            for (rutaSnap in snapshot.children) {
                val ruta = rutaSnap.getValue(Ruta::class.java)
                if (ruta != null) {
                    listaRutasAccesoRapido.add(ruta)
                }
            }
            // Refrescamos la lista para que aparezcan las tarjetas
            adapterRutas.notifyDataSetChanged()

        }.addOnFailureListener {
            Toast.makeText(this, "Error al cargar las rutas recientes", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        btnConfigRutas.setOnClickListener {
            // Abrimos el panel de administración
            val intent = Intent(this, ListaRutas::class.java)
            startActivity(intent)
        }

        btnSoporte.setOnClickListener {
            showToast("Abriendo soporte...")
        }

        tabRecorrido.setOnClickListener {
            showToast("Ya estás en la pantalla principal")
        }

        tabStats.setOnClickListener {
            showToast("Estadísticas en desarrollo...")
        }

        tabSettings.setOnClickListener {
            showToast("Abriendo perfil...")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}