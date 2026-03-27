package com.example.proyecto_definitivo // Tu paquete



import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeRutasActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    // Vistas del Dashboard
    private lateinit var tvGreeting: TextView
    private lateinit var tvRouteName: TextView
    private lateinit var tvProgressLabel: TextView
    private lateinit var routeProgress: ProgressBar
    private lateinit var cardActiveRoute: MaterialCardView
    private lateinit var btnFollowRoute: MaterialButton

    // 🚨 EL CAMBIO CLAVE: Ahora Kotlin sabe que son MaterialCardView
    private lateinit var btnNewRoute: MaterialCardView
    private lateinit var btnReportIncident: MaterialCardView

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var badgeLive: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_rutas) // Tu XML perfecto

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: ""
        dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        initViews()
        loadUserData()
        setupBottomNav()
    }

    private fun initViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        tvRouteName = findViewById(R.id.tvRouteName)
        tvProgressLabel = findViewById(R.id.tvProgressLabel)
        routeProgress = findViewById(R.id.routeProgress)
        cardActiveRoute = findViewById(R.id.cardActiveRoute)
        btnFollowRoute = findViewById(R.id.btnFollowRoute)

        // 🚨 Inicializamos las tarjetas interactivas
        btnNewRoute = findViewById(R.id.btnNewRoute)
        btnReportIncident = findViewById(R.id.btnReportIncident)

        bottomNav = findViewById(R.id.bottomNav)
        badgeLive = findViewById(R.id.badgeLive)

        // Los CardViews tienen OnClickListener igual que los botones
        btnFollowRoute.setOnClickListener {
            showToast("Iniciando navegación GPS...")
            // Aquí irá el Intent a tu mapa
        }

        btnNewRoute.setOnClickListener {
            startActivity(Intent(this, CrearRuta::class.java))
        }

        btnReportIncident.setOnClickListener {
            showToast("Reporte de incidente abierto")
        }
    }

    private fun loadUserData() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // 1. Saludo personalizado
                    val nombre = snapshot.child("nombre").value.toString()
                    tvGreeting.text = "¡Hola, $nombre!"

                    // 2. Lógica de Ruta Activa
                    val rutaActiva = snapshot.child("ruta_actual").exists()
                    updateRouteUI(rutaActiva, snapshot)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Error al conectar con la base de datos")
            }
        })
    }

    private fun updateRouteUI(isActive: Boolean, snapshot: DataSnapshot) {
        if (isActive) {
            val nombreRuta = snapshot.child("ruta_actual/nombre").value.toString()
            val progreso = snapshot.child("ruta_actual/puntos_completados").value.toString()
            val total = snapshot.child("ruta_actual/total_puntos").value.toString()

            tvRouteName.text = nombreRuta
            tvProgressLabel.text = "$progreso/$total"

            // Calcular porcentaje para la ProgressBar
            val p = progreso.toIntOrNull() ?: 0
            val t = total.toIntOrNull() ?: 1
            routeProgress.progress = (p * 100) / t

            badgeLive.visibility = View.VISIBLE
            btnFollowRoute.text = "Seguir Ruta"
        } else {
            // "MODO ZENDA": Motores apagados
            tvRouteName.text = "Motores apagados. El camino te espera, conductor."
            tvProgressLabel.text = "0/0"
            routeProgress.progress = 0
            badgeLive.visibility = View.GONE
            btnFollowRoute.text = "Iniciar Jornada"
        }
    }

    private fun setupBottomNav() {
        // 1. Forzamos que al abrir el Dashboard, el ícono resaltado sea Home
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Ya estás en Home, no hacemos nada o refrescamos
                    true
                }
                R.id.nav_routes -> {
                    // 2. Ir a la pantalla de gestión de rutas (ListaRutas)
                    val intent = Intent(this, ListaRutas::class.java)
                    startActivity(intent)
                    // Quitamos la animación para que parezca una sola app fluida
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_stats -> {
                    showToast("Estadísticas en desarrollo...")
                    true
                }
                R.id.nav_settings -> {
                    showToast("Abriendo Perfil...")
                    true
                }
                else -> false
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}