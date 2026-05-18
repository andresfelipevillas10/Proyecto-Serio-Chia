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
            val sorted = cachedRoutes.sortedByDescending { it.creadaEn }
            rutaAdapter.actualizarRutas(sorted)
            
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
            
            val sorted = routes.sortedByDescending { it.creadaEn }
            rutaAdapter.actualizarRutas(sorted)
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
                // En vez de recargar todo, notifyDataSetChanged está bien para el pin si no queremos pasar toda la lista
                // Pero como ya implementamos DiffUtil, podemos pasar la misma lista o solo notificar cambio
                rutaAdapter.notifyDataSetChanged() // o usar DiffUtil si quisieramos
            }
        )
        rvConfigRutas.adapter = rutaAdapter
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
        FrecuentesManager.unpinRoute(this, ruta.id)
        db.child("rutas").child(currentUserId).child(ruta.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.route_deleted), Toast.LENGTH_SHORT).show()
            }
    }
}