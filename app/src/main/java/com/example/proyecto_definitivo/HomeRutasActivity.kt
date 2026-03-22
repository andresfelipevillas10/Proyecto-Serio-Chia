package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class HomeRutasActivity : AppCompatActivity() {

    // 1. Declaramos las vistas de nuestra NUEVA interfaz
    private lateinit var btnConfigRutas: MaterialCardView
    private lateinit var btnSoporte: MaterialCardView
    private lateinit var rvRoutes: RecyclerView

    // Tabs del Bottom Navigation
    private lateinit var tabRecorrido: LinearLayout
    private lateinit var tabStats: LinearLayout
    private lateinit var tabSettings: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_rutas)

        // 2. Enlazamos los IDs del XML con nuestras variables
        btnConfigRutas = findViewById(R.id.btnConfigRutas)
        btnSoporte = findViewById(R.id.btnSoporte)
        rvRoutes = findViewById(R.id.rvRoutes)

        tabRecorrido = findViewById(R.id.tabRecorrido)
        tabStats = findViewById(R.id.tabStats)
        tabSettings = findViewById(R.id.tabSettings)

        // 3. Preparamos el terreno para el Adapter (Comentado para la siguiente capa)
        // rvRoutes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        // val adapter = RutasAdapter(listaDeRutas)
        // rvRoutes.adapter = adapter

        // 4. Asignamos los eventos Click con "Luces Piloto" (Toasts) y dejamos los Intents listos

        btnConfigRutas.setOnClickListener {
            Toast.makeText(this, "Cargando Configuración de Rutas...", Toast.LENGTH_SHORT).show()
            // Futuro: Equivalente a tu antiguo btnIrConfigurarPuntos / btnIrCrearRuta
            // startActivity(Intent(this, ListaRutas::class.java))
        }

        btnSoporte.setOnClickListener {
            Toast.makeText(this, "Abriendo Soporte Técnico...", Toast.LENGTH_SHORT).show()
            // Futuro: startActivity(Intent(this, SoporteActivity::class.java))
        }

        tabRecorrido.setOnClickListener {
            // Ya estamos en el Home (Recorrido), así que normalmente aquí no hacemos nada,
            // o hacemos que la pantalla haga scroll hacia arriba.
            Toast.makeText(this, "Ya estás en Recorrido", Toast.LENGTH_SHORT).show()
        }

        tabStats.setOnClickListener {
            Toast.makeText(this, "Cargando Estadísticas...", Toast.LENGTH_SHORT).show()
            // Futuro: Equivalente a tu antiguo btnIrHistorialRecorridos
            // startActivity(Intent(this, HistorialRecorridos::class.java))
        }

        tabSettings.setOnClickListener {
            Toast.makeText(this, "Abriendo Ajustes de Usuario...", Toast.LENGTH_SHORT).show()
            // Futuro: Equivalente a tu antiguo btnIrDatosUsuario
            // startActivity(Intent(this, DatosUsuario::class.java))
        }
    }
}