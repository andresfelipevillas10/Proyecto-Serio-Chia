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

    private lateinit var tvGreeting: TextView
    private lateinit var tvRouteName: TextView
    private lateinit var tvProgressLabel: TextView
    private lateinit var routeProgress: ProgressBar
    private lateinit var cardActiveRoute: MaterialCardView
    private lateinit var btnFollowRoute: MaterialButton
    private lateinit var btnNewRoute: MaterialCardView
    private lateinit var btnReportIncident: MaterialCardView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var badgeLive: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_rutas)

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: ""
        dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        initViews()
        setupFirebaseListeners()
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
            val userId = auth.currentUser?.uid ?: ""
            FirebaseDatabase.getInstance().getReference("users").child(userId).child("ruta_actual")
                .get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        showToast(getString(R.string.gps_nav_start))
                    } else {
                        startActivity(Intent(this, ListaRutas::class.java))
                    }
                }
        }

        btnNewRoute.setOnClickListener {
            startActivity(Intent(this, CrearRuta::class.java))
        }

        btnReportIncident.setOnClickListener {
            showToast("Reporte de incidente abierto")
        }
    }

    private fun setupFirebaseListeners() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val nombre = snapshot.child("nombre").value.toString()
                    tvGreeting.text = getString(R.string.hello_user, nombre)

                    val rutaActiva = snapshot.child("ruta_actual").exists()
                    updateRouteUI(rutaActiva, snapshot)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast(getString(R.string.db_error, error.message))
            }
        })
    }

    private fun updateRouteUI(isActive: Boolean, snapshot: DataSnapshot) {
        if (isActive) {
            cardActiveRoute.visibility = View.VISIBLE
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
            // El usuario pidió que la tarjeta principal no esté informada si no hay rutas
            cardActiveRoute.visibility = View.GONE
        }
    }

    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_routes -> {
                    startActivity(Intent(this, ListaRutas::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_stats -> {
                    showToast(getString(R.string.stats_development))
                    false
                }
                R.id.nav_settings -> {
                    showToast(getString(R.string.opening_profile))
                    false
                }
                else -> false
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}