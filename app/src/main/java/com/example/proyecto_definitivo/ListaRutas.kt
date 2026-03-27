package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ListaRutas : AppCompatActivity() { // ¡MANTENEMOS TU CLASE INTACTA!

    // 1. Declaramos las nuevas vistas del diseño Tailwind
    private lateinit var rvConfigRutas: RecyclerView
    private lateinit var fabAddRoute: ExtendedFloatingActionButton
    private lateinit var btnBackConfig: ImageButton

    // Tu lógica de Firebase intacta
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private val listaRutas = mutableListOf<Ruta>()
    private lateinit var rutaAdapter: RutaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configurar_rutas)

        // 1. Enlazamos los IDs del XML
        rvConfigRutas = findViewById(R.id.rvConfigRutas)
        fabAddRoute = findViewById(R.id.fabAddRoute)
        btnBackConfig = findViewById(R.id.btnBackConfig)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // 2. Configuración del Bottom Nav (Conexión Vital)
        bottomNav.selectedItemId = R.id.nav_routes // Resalta el icono de Rutas
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Volver al Dashboard principal
                    startActivity(Intent(this, HomeRutasActivity::class.java))
                    overridePendingTransition(0, 0) // Transición instantánea
                    finish()
                    true
                }
                R.id.nav_routes -> true // Ya estamos aquí
                else -> true
            }
        }

        // 3. Setup del RecyclerView
        rvConfigRutas.layoutManager = LinearLayoutManager(this)
        rutaAdapter = RutaAdapter(
            listaRutas = listaRutas,
            onEditClick = { rutaSeleccionada ->
                abrirPantallaConfigurarPuntos(rutaSeleccionada)
            },
            onDeleteClick = { rutaAEliminar ->
                eliminarRutaFirebase(rutaAEliminar)
            },
            onStartClick = { rutaAIniciar ->
                val intent = Intent(this, PreRecorridoActivity::class.java).apply {
                    putExtra("rutaId", rutaAIniciar.id)
                    putExtra("rutaNombre", rutaAIniciar.nombre)
                    putExtra("rutaRadio", rutaAIniciar.radioDeteccion)
                }
                startActivity(intent)
            }
        )
        rvConfigRutas.adapter = rutaAdapter

        // 4. Clics de botones
        btnBackConfig.setOnClickListener {
            // En lugar de finish(), mejor volver al Home para asegurar flujo
            startActivity(Intent(this, HomeRutasActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        fabAddRoute.setOnClickListener {
            // Al crear, se mantiene el contexto de "Inicio"
            startActivity(Intent(this, CrearRuta::class.java))
        }

        cargarRutas()
    }
    private fun cargarRutas() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Quitamos el ProgressBar nativo porque tu diseño nuevo es más limpio
        db.child("rutas").child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaRutas.clear()

                for (rutaSnap in snapshot.children) {
                    val ruta = rutaSnap.getValue(Ruta::class.java)
                    if (ruta != null) {
                        listaRutas.add(ruta)
                    }
                }

                listaRutas.sortByDescending { it.creadaEn }
                rutaAdapter.notifyDataSetChanged()

                if (listaRutas.isEmpty()) {
                    Toast.makeText(this@ListaRutas, getString(R.string.no_routes_yet), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ListaRutas, getString(R.string.error_loading_routes, error.message), Toast.LENGTH_LONG).show()
            }
        })
    }

    // Tu función original intacta
    private fun abrirPantallaConfigurarPuntos(ruta: Ruta) {
        val intent = Intent(this, ConfigurarPuntosRuta::class.java)
        intent.putExtra("rutaId", ruta.id)
        intent.putExtra("rutaNombre", ruta.nombre)
        intent.putExtra("rutaRadio", ruta.radioDeteccion)
        startActivity(intent)
    }

    // Función para el botón rojo de la tarjeta
    private fun eliminarRutaFirebase(ruta: Ruta) {
        val currentUserId = auth.currentUser?.uid ?: return
        db.child("rutas").child(currentUserId).child(ruta.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.route_deleted), Toast.LENGTH_SHORT).show()
            }
    }
}