package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

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
    private lateinit var btnBusInfo: MaterialCardView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var badgeLive: TextView

    // Nuevos elementos
    private lateinit var tvEmptyState: TextView
    private lateinit var layoutFrecuentes: LinearLayout
    private lateinit var containerFrecuentes: LinearLayout
    private lateinit var tvStat1Value: TextView
    private lateinit var tvStat2Value: TextView

    private var hasActiveRoute = false
    private var hasAnyRoutes = false

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

        btnNewRoute = findViewById(R.id.btnNewRoute)
        btnReportIncident = findViewById(R.id.btnReportIncident)
        btnBusInfo = findViewById(R.id.btnBusInfo)

        bottomNav = findViewById(R.id.bottomNav)
        badgeLive = findViewById(R.id.badgeLive)

        // Inicializar nuevos
        tvEmptyState = findViewById(R.id.tvEmptyState)
        layoutFrecuentes = findViewById(R.id.layoutFrecuentes)
        containerFrecuentes = findViewById(R.id.containerFrecuentes)

        // Estadísticas
        val statCard1 = findViewById<MaterialCardView>(R.id.statCard1)
        val statCard2 = findViewById<MaterialCardView>(R.id.statCard2)
        tvStat1Value = statCard1.findViewById(R.id.tvStatValue)
        tvStat2Value = statCard2.findViewById(R.id.tvStatValue)

        val tvStat1Label = statCard1.findViewById<TextView>(R.id.tvStatLabel)
        val tvStat2Label = statCard2.findViewById<TextView>(R.id.tvStatLabel)
        tvStat1Label.text = "VIAJES HOY"
        tvStat2Label.text = "RUTAS TOTAL"

        btnFollowRoute.setOnClickListener {
            val userId = auth.currentUser?.uid ?: ""
            FirebaseDatabase.getInstance().getReference("users").child(userId).child("ruta_actual")
                .get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val rId = snapshot.child("id").value.toString()
                        val rNombre = snapshot.child("nombre").value.toString()
                        val recId = snapshot.child("recorridoId").value.toString()
                        val rRadio = snapshot.child("radioDeteccion").getValue(Float::class.java) ?: 30f

                        val intent = Intent(this, NavegacionActivaActivity::class.java).apply {
                            putExtra("rutaId", rId)
                            putExtra("rutaNombre", rNombre)
                            putExtra("recorridoId", recId)
                            putExtra("rutaRadio", rRadio)
                        }
                        startActivity(intent)
                    } else {
                        startActivity(Intent(this, ListaRutas::class.java))
                    }
                }
        }

        btnNewRoute.setOnClickListener {
            startActivity(Intent(this, CrearRuta::class.java))
        }

        btnReportIncident.setOnClickListener {
            showToast(getString(R.string.incident_report_open))
        }
    }

    private fun setupFirebaseListeners() {
        val userId = auth.currentUser?.uid ?: return

        // 1. Datos del usuario (Saludo y Ruta Actual)
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val nombre = snapshot.child("nombre").value.toString()
                    tvGreeting.text = getString(R.string.hello_user, nombre)

                    hasActiveRoute = snapshot.child("ruta_actual").exists()
                    updateRouteUI(hasActiveRoute, snapshot)
                    checkEmptyState()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. Rutas Frecuentes (Top 2 mas usadas)
        FirebaseDatabase.getInstance().getReference("rutas").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val rutas = mutableListOf<Ruta>()
                    for (snap in snapshot.children) {
                        snap.getValue(Ruta::class.java)?.let { rutas.add(it) }
                    }
                    hasAnyRoutes = rutas.isNotEmpty()
                    tvStat2Value.text = rutas.size.toString()
                    
                    updateFrecuentesUI(rutas.sortedByDescending { it.usoCount }.take(2))
                    checkEmptyState()
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // 3. Viajes de hoy
        FirebaseDatabase.getInstance().getReference("recorridos").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var viajesHoy = 0
                    val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                    
                    for (snap in snapshot.children) {
                        val inicio = snap.child("inicioTiempo").getValue(Long::class.java) ?: 0L
                        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(inicio))
                        if (dateStr == today) {
                            viajesHoy++
                        }
                    }
                    tvStat1Value.text = if (viajesHoy > 0) viajesHoy.toString() else "0"
                }
                override fun onCancelled(error: DatabaseError) {}
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

            val p = progreso.toIntOrNull() ?: 0
            val t = total.toIntOrNull() ?: 1
            routeProgress.progress = (p * 100) / t

            badgeLive.visibility = View.VISIBLE
            btnFollowRoute.text = "Seguir Ruta"
        } else {
            cardActiveRoute.visibility = View.GONE
            badgeLive.visibility = View.GONE
        }
    }

    private fun updateFrecuentesUI(topRutas: List<Ruta>) {
        containerFrecuentes.removeAllViews()
        if (topRutas.isEmpty() || hasActiveRoute) {
            layoutFrecuentes.visibility = View.GONE
            return
        }

        layoutFrecuentes.visibility = View.VISIBLE
        val inflater = LayoutInflater.from(this)

        for (ruta in topRutas) {
            val card = inflater.inflate(R.layout.item_route_card, containerFrecuentes, false)
            card.findViewById<TextView>(R.id.tvRouteName).text = ruta.nombre
            card.findViewById<TextView>(R.id.tvRouteDetails).text = "${ruta.horaSalida} - ${ruta.horaLlegada}"
            
            // Si tiene mas de 5 usos, mostrar badge de FRECUENTE
            if (ruta.usoCount > 5) {
                card.findViewById<TextView>(R.id.tvRouteBadge).visibility = View.VISIBLE
            }

            card.findViewById<MaterialButton>(R.id.btnStartRoute).alpha = 1.0f
            card.findViewById<MaterialButton>(R.id.btnStartRoute).setOnClickListener {
                val intent = Intent(this, PreRecorridoActivity::class.java).apply {
                    putExtra("rutaId", ruta.id)
                    putExtra("rutaNombre", ruta.nombre)
                    putExtra("rutaRadio", ruta.radioDeteccion)
                }
                startActivity(intent)
            }
            
            card.findViewById<View>(R.id.btnRemoveRoute).visibility = View.GONE

            containerFrecuentes.addView(card)
        }
    }

    private fun checkEmptyState() {
        if (!hasActiveRoute && !hasAnyRoutes) {
            tvEmptyState.visibility = View.VISIBLE
            layoutFrecuentes.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_routes -> {
                    startActivity(Intent(this, ListaRutas::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
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