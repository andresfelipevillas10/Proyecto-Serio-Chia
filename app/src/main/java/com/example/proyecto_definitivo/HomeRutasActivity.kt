package com.example.proyecto_definitivo

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla principal del conductor.
 *
 * Muestra la tarjeta de ruta activa, atajos de rutas frecuentes
 * (fijadas manualmente con ★ y automáticas por uso), estadísticas
 * diarias, y acciones rápidas (crear ruta, reportar incidente).
 *
 * Cuando hay una ruta activa y el conductor presiona "atrás",
 * NO sale de la app; muestra un Snackbar indicando que la ruta
 * sigue en curso.
 */
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

    // Frecuentes UI
    private lateinit var sectionFrecuentes: LinearLayout
    private lateinit var containerFrecuentes: LinearLayout
    private lateinit var tvFrecuentesHint: TextView

    // Estadísticas
    private lateinit var tvStat1Value: TextView
    private lateinit var tvStat2Value: TextView

    private var hasActiveRoute = false
    private var hasAnyRoutes = false

    // Datos de ruta activa (para el servicio flotante)
    private var activeRutaId = ""
    private var activeRutaNombre = ""
    private var activeRecorridoId = ""
    private var activeRutaRadio = 30f

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

    override fun onResume() {
        super.onResume()
        // Detener el servicio flotante cuando el Home vuelve a primer plano
        stopService(Intent(this, FloatingRouteService::class.java))
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

        bottomNav = findViewById(R.id.bottomNav)
        badgeLive = findViewById(R.id.badgeLive)

        // Frecuentes
        sectionFrecuentes = findViewById(R.id.sectionFrecuentes)
        containerFrecuentes = findViewById(R.id.containerFrecuentes)
        tvFrecuentesHint = findViewById(R.id.tvFrecuentesHint)

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

        // 2. Rutas (para frecuentes + estadística)
        FirebaseDatabase.getInstance().getReference("rutas").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val rutas = mutableListOf<Ruta>()
                    for (snap in snapshot.children) {
                        snap.getValue(Ruta::class.java)?.let { rutas.add(it) }
                    }
                    hasAnyRoutes = rutas.isNotEmpty()
                    tvStat2Value.text = rutas.size.toString()

                    // Renderizar frecuentes con datos frescos
                    renderFrecuentes(rutas)
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

    /**
     * Actualiza la tarjeta de ruta activa y activa la animación de pulso
     * en el badge "EN VIVO".
     */
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

            // Animación de pulso en el badge EN VIVO
            startPulseAnimation(badgeLive)

            // Guardar datos para servicio flotante
            activeRutaId = snapshot.child("ruta_actual/id").value?.toString() ?: ""
            activeRutaNombre = nombreRuta
            activeRecorridoId = snapshot.child("ruta_actual/recorridoId").value?.toString() ?: ""
            activeRutaRadio = snapshot.child("ruta_actual/radioDeteccion").getValue(Float::class.java) ?: 30f
        } else {
            cardActiveRoute.visibility = View.GONE
            badgeLive.visibility = View.GONE
        }
    }

    /**
     * Animación de pulso (alpha) en el badge "EN VIVO" para indicar visualmente
     * que la ruta está activa en tiempo real.
     */
    private fun startPulseAnimation(view: View) {
        val animator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.3f, 1f)
        animator.duration = 1500
        animator.repeatCount = ValueAnimator.INFINITE
        animator.start()
    }

    /**
     * Renderiza los atajos de rutas frecuentes en el HorizontalScrollView.
     * Combina las fijadas manualmente (★) con las automáticas por uso.
     *
     * @param allRoutes Lista completa de rutas del conductor desde Firebase.
     */
    private fun renderFrecuentes(allRoutes: List<Ruta>) {
        val shortcuts = FrecuentesManager.getShortcuts(this, allRoutes)

        containerFrecuentes.removeAllViews()

        if (shortcuts.isEmpty()) {
            sectionFrecuentes.visibility = View.GONE
            // Solo mostrar hint si hay rutas pero ningún atajo
            tvFrecuentesHint.visibility = if (hasAnyRoutes) View.VISIBLE else View.GONE
            return
        }

        sectionFrecuentes.visibility = View.VISIBLE
        tvFrecuentesHint.visibility = View.GONE

        val inflater = LayoutInflater.from(this)

        for (shortcut in shortcuts) {
            val chipView = inflater.inflate(R.layout.item_frecuente_chip, containerFrecuentes, false)

            val tvName = chipView.findViewById<TextView>(R.id.tvFrecuenteName)
            val btnRemove = chipView.findViewById<ImageButton>(R.id.btnRemoveFrecuente)
            val btnStart = chipView.findViewById<MaterialButton>(R.id.btnFrecuenteStart)
            val tvPinned = chipView.findViewById<TextView>(R.id.tvPinnedBadge)

            tvName.text = shortcut.ruta.nombre
            tvPinned.visibility = if (shortcut.isPinned) View.VISIBLE else View.GONE

            btnStart.setOnClickListener {
                val intent = Intent(this, PreRecorridoActivity::class.java).apply {
                    putExtra("rutaId", shortcut.ruta.id)
                    putExtra("rutaNombre", shortcut.ruta.nombre)
                    putExtra("rutaRadio", shortcut.ruta.radioDeteccion)
                }
                startActivity(intent)
            }

            btnRemove.setOnClickListener {
                if (shortcut.isPinned) {
                    FrecuentesManager.unpinRoute(this, shortcut.ruta.id)
                    renderFrecuentes(allRoutes) // Re-render
                    Toast.makeText(this, "Atajo eliminado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Los atajos automáticos se basan en el uso", Toast.LENGTH_SHORT).show()
                }
            }

            containerFrecuentes.addView(chipView)
        }
    }

    private fun checkEmptyState() {
        // Nada que hacer actualmente; la lógica de visibilidad está en updateRouteUI y renderFrecuentes
    }

    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_routes -> {
                    startActivity(Intent(this, ListaRutas::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
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

    /**
     * Intercepta el botón "atrás" cuando hay una ruta activa.
     * En lugar de cerrar la app, muestra un Snackbar y lanza el servicio flotante
     * para que el conductor siga viendo el progreso sobre otras apps.
     */
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (hasActiveRoute) {
            // Verificar permiso de overlay
            if (Settings.canDrawOverlays(this)) {
                launchFloatingService()
                Snackbar.make(
                    findViewById(android.R.id.content),
                    getString(R.string.back_with_active_route),
                    Snackbar.LENGTH_LONG
                ).setAction("ABRIR RUTA") {
                    btnFollowRoute.performClick()
                }.show()
            } else {
                // Pedir permiso de overlay
                requestOverlayPermission()
            }
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Lanza el servicio flotante con los datos de la ruta activa actual.
     */
    private fun launchFloatingService() {
        val serviceIntent = Intent(this, FloatingRouteService::class.java).apply {
            putExtra(FloatingRouteService.EXTRA_RUTA_ID, activeRutaId)
            putExtra(FloatingRouteService.EXTRA_RUTA_NOMBRE, activeRutaNombre)
            putExtra(FloatingRouteService.EXTRA_RECORRIDO_ID, activeRecorridoId)
            putExtra(FloatingRouteService.EXTRA_RUTA_RADIO, activeRutaRadio)
        }
        startForegroundService(serviceIntent)
    }

    /**
     * Solicita al usuario el permiso para mostrar sobre otras apps.
     */
    private fun requestOverlayPermission() {
        Toast.makeText(this, "Permite 'Mostrar sobre otras apps' para la miniatura de ruta", Toast.LENGTH_LONG).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}