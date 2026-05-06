package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
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
import java.util.concurrent.TimeUnit

class ListaRutas : AppCompatActivity(), RutaSyncManager.SyncCallback {

    private lateinit var rvConfigRutas: RecyclerView
    private lateinit var fabAddRoute: ExtendedFloatingActionButton
    private lateinit var btnBackConfig: ImageButton
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var progressSync: ProgressBar
    private lateinit var tvSyncStatus: TextView

    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private val listaRutas = mutableListOf<Ruta>()
    private lateinit var rutaAdapter: RutaAdapter
    private lateinit var syncManager: RutaSyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configurar_rutas)

        syncManager = RutaSyncManager(this)
        syncManager.addSyncCallback(this)

        initViews()
        setupBottomNav()
        setupRecyclerView()
        setupSyncUI()
        
        // Cargar datos cacheados primero para experiencia offline
        loadCachedRoutes()
        
        // Iniciar sincronización en tiempo real
        syncManager.startRealtimeSync()
    }

    private fun initViews() {
        rvConfigRutas = findViewById(R.id.rvConfigRutas)
        fabAddRoute = findViewById(R.id.fabAddRoute)
        btnBackConfig = findViewById(R.id.btnBackConfig)
        bottomNav = findViewById(R.id.bottomNav)
        progressSync = findViewById(R.id.progressSync)
        tvSyncStatus = findViewById(R.id.tvSyncStatus)

        btnBackConfig.setOnClickListener {
            syncManager.cleanup()
            startActivity(Intent(this, HomeRutasActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            })
            overridePendingTransition(0, 0)
            finish()
        }

        fabAddRoute.setOnClickListener {
            startActivity(Intent(this, CrearRuta::class.java))
        }
    }

    private fun setupSyncUI() {
        progressSync.visibility = android.view.View.GONE
        tvSyncStatus.text = getString(R.string.sync_status_idle)
    }

    private fun loadCachedRoutes() {
        val cachedRoutes = syncManager.getCachedRoutes()
        if (cachedRoutes.isNotEmpty()) {
            listaRutas.clear()
            listaRutas.addAll(cachedRoutes)
            listaRutas.sortByDescending { it.creadaEn }
            rutaAdapter.notifyDataSetChanged()
            
            val lastSync = syncManager.getLastSyncTime()
            if (lastSync > 0) {
                val timeAgo = System.currentTimeMillis() - lastSync
                val minutesAgo = TimeUnit.MILLISECONDS.toMinutes(timeAgo)
                tvSyncStatus.text = getString(R.string.sync_status_cached, minutesAgo)
            }
        }
    }

    // Implementación de SyncCallback
    override fun onSyncStarted() {
        runOnUiThread {
            progressSync.visibility = android.view.View.VISIBLE
            tvSyncStatus.text = getString(R.string.sync_status_syncing)
        }
    }

    override fun onSyncCompleted(routes: List<Ruta>) {
        runOnUiThread {
            progressSync.visibility = android.view.View.GONE
            tvSyncStatus.text = getString(R.string.sync_status_success)
            
            listaRutas.clear()
            listaRutas.addAll(routes)
            listaRutas.sortByDescending { it.creadaEn }
            rutaAdapter.notifyDataSetChanged()
            
            if (routes.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_routes_yet), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSyncFailed(error: String) {
        runOnUiThread {
            progressSync.visibility = android.view.View.GONE
            tvSyncStatus.text = getString(R.string.sync_status_failed)
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
    }

    override fun onSyncStatusChanged(status: RutaSyncManager.SyncStatus) {
        runOnUiThread {
            when (status) {
                RutaSyncManager.SyncStatus.OFFLINE -> {
                    tvSyncStatus.text = getString(R.string.sync_status_offline)
                }
                RutaSyncManager.SyncStatus.SUCCESS -> {
                    tvSyncStatus.text = getString(R.string.sync_status_success)
                }
                RutaSyncManager.SyncStatus.FAILED -> {
                    tvSyncStatus.text = getString(R.string.sync_status_failed)
                }
                else -> {}
            }
        }
    }

    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.nav_routes
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeRutasActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_routes -> true
                R.id.nav_stats -> {
                    Toast.makeText(this, getString(R.string.stats_development), Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, getString(R.string.opening_profile), Toast.LENGTH_SHORT).show()
                    false
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
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
            },
            onPinClick = { ruta ->
                val isPinned = FrecuentesManager.isPinned(this, ruta.id)
                if (isPinned) {
                    FrecuentesManager.unpinRoute(this, ruta.id)
                    Toast.makeText(this, "★ Ruta desfijada", Toast.LENGTH_SHORT).show()
                } else {
                    val success = FrecuentesManager.pinRoute(this, ruta)
                    if (success) {
                        Toast.makeText(this, "★ Ruta fijada como frecuente", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Máximo 3 rutas fijadas", Toast.LENGTH_SHORT).show()
                    }
                }
                rutaAdapter.notifyDataSetChanged()
            }
        )
        rvConfigRutas.adapter = rutaAdapter
    }

    private fun setupFirebaseListeners() {
        val currentUserId = auth.currentUser?.uid ?: return

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