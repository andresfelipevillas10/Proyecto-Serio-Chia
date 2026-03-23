package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        // 2. MAGIA AQUÍ: Le decimos a tu clase vieja que use el diseño nuevo
        setContentView(R.layout.activity_configurar_rutas)

        // Enlazamos los IDs del XML nuevo
        rvConfigRutas = findViewById(R.id.rvConfigRutas)
        fabAddRoute = findViewById(R.id.fabAddRoute)
        btnBackConfig = findViewById(R.id.btnBackConfig)

        rvConfigRutas.layoutManager = LinearLayoutManager(this)

        // Inicializamos el Adapter (con las acciones de editar y eliminar)
        rutaAdapter = RutaAdapter(
            listaRutas = listaRutas,
            onEditClick = { rutaSeleccionada ->
                abrirPantallaConfigurarPuntos(rutaSeleccionada)
            },
            onDeleteClick = { rutaAEliminar ->
                eliminarRutaFirebase(rutaAEliminar)
            }
        )

        rvConfigRutas.adapter = rutaAdapter

        // Los clics de los botones principales
        btnBackConfig.setOnClickListener {
            finish() // Cierra esta pantalla y vuelve atrás
        }

        fabAddRoute.setOnClickListener {
            // Te lleva a tu clase de creación de rutas
            val intent = Intent(this, CrearRuta::class.java) // Asegúrate de que apunte a tu clase de Crear Ruta
            startActivity(intent)
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