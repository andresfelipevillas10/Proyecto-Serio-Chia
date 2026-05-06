package com.example.proyecto_definitivo

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class RutaSyncManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RutaSyncManager"
        private const val SYNC_PREFS = "ruta_sync_prefs"
        private const val LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val CACHED_ROUTES_KEY = "cached_routes"
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val INITIAL_RETRY_DELAY_MS = 1000L
    }
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
    
    private var currentListener: ValueEventListener? = null
    private var isSyncing = false
    private var syncCallbacks: MutableList<SyncCallback> = mutableListOf()
    
    interface SyncCallback {
        fun onSyncStarted()
        fun onSyncCompleted(routes: List<Ruta>)
        fun onSyncFailed(error: String)
        fun onSyncStatusChanged(status: SyncStatus)
    }
    
    enum class SyncStatus {
        IDLE, SYNCING, SUCCESS, FAILED, OFFLINE
    }
    
    fun addSyncCallback(callback: SyncCallback) {
        syncCallbacks.add(callback)
    }
    
    fun removeSyncCallback(callback: SyncCallback) {
        syncCallbacks.remove(callback)
    }
    
    fun startRealtimeSync() {
        val userId = auth.currentUser?.uid ?: run {
            notifySyncFailed("Usuario no autenticado")
            return
        }
        
        stopRealtimeSync()
        
        currentListener = db.child("rutas").child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    processSnapshot(snapshot)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Firebase sync cancelled: ${error.message}")
                notifySyncFailed("Error de conexión: ${error.message}")
                attemptRetrySync()
            }
        })
        
        Log.d(TAG, "Iniciada sincronización en tiempo real")
        notifySyncStatusChanged(SyncStatus.SYNCING)
    }
    
    fun stopRealtimeSync() {
        currentListener?.let {
            db.child("rutas").child(auth.currentUser?.uid ?: "").removeEventListener(it)
            currentListener = null
        }
        notifySyncStatusChanged(SyncStatus.IDLE)
    }
    
    suspend fun forceSync(): Result<List<Ruta>> = withContext(Dispatchers.IO) {
        if (isSyncing) {
            return@withContext Result.failure(Exception("Sincronización ya en progreso"))
        }
        
        isSyncing = true
        notifySyncStarted()
        
        try {
            val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
            val snapshot = db.child("rutas").child(userId).get().await()
            val routes = processSnapshot(snapshot)
            
            notifySyncCompleted(routes)
            Result.success(routes)
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización forzada: ${e.message}", e)
            notifySyncFailed("Error de sincronización: ${e.message}")
            Result.failure(e)
        } finally {
            isSyncing = false
        }
    }
    
    private suspend fun processSnapshot(snapshot: DataSnapshot): List<Ruta> {
        val routes = mutableListOf<Ruta>()
        
        for (rutaSnap in snapshot.children) {
            try {
                val ruta = rutaSnap.getValue(Ruta::class.java)
                if (ruta != null && validateRoute(ruta)) {
                    routes.add(ruta)
                    Log.d(TAG, "Ruta procesada: ${ruta.nombre} (ID: ${ruta.id})")
                } else {
                    Log.w(TAG, "Ruta inválida o nula encontrada: ${rutaSnap.key}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando ruta ${rutaSnap.key}: ${e.message}")
            }
        }
        
        routes.sortByDescending { it.creadaEn }
        
        if (routes.isNotEmpty()) {
            cacheRoutes(routes)
            prefs.edit { putLong(LAST_SYNC_TIMESTAMP, System.currentTimeMillis()) }
            Log.i(TAG, "Sincronización completada: ${routes.size} rutas procesadas")
        }
        
        return routes
    }
    
    private fun validateRoute(route: Ruta): Boolean {
        return when {
            route.id.isBlank() -> {
                Log.w(TAG, "Ruta sin ID: ${route.nombre}")
                false
            }
            route.nombre.isBlank() -> {
                Log.w(TAG, "Ruta sin nombre: ${route.id}")
                false
            }
            route.radioDeteccion <= 0 -> {
                Log.w(TAG, "Radio de detección inválido en ruta: ${route.nombre}")
                false
            }
            else -> true
        }
    }
    
    private fun cacheRoutes(routes: List<Ruta>) {
        val json = routes.joinToString("|") { route ->
            "${route.id},${route.nombre},${route.descripcion},${route.activa},${route.radioDeteccion},${route.creadaEn}"
        }
        prefs.edit { putString(CACHED_ROUTES_KEY, json) }
    }
    
    fun getCachedRoutes(): List<Ruta> {
        val cachedJson = prefs.getString(CACHED_ROUTES_KEY, null) ?: return emptyList()
        
        return cachedJson.split("|").mapNotNull { routeStr ->
            val parts = routeStr.split(",")
            if (parts.size >= 6) {
                Ruta(
                    id = parts[0],
                    nombre = parts[1],
                    descripcion = parts[2],
                    activa = parts[3].toBoolean(),
                    radioDeteccion = parts[4].toFloat(),
                    creadaEn = parts[5].toLong()
                )
            } else {
                null
            }
        }
    }
    
    fun getLastSyncTime(): Long {
        return prefs.getLong(LAST_SYNC_TIMESTAMP, 0)
    }
    
    fun isOnline(): Boolean {
        return try {
            Runtime.getRuntime().exec("ping -c 1 google.com").waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
    
    private fun attemptRetrySync(attempt: Int = 1) {
        if (attempt > MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "Máximo de intentos de reconexión alcanzado")
            notifySyncStatusChanged(SyncStatus.FAILED)
            return
        }
        
        val delayMs = INITIAL_RETRY_DELAY_MS * (1 shl (attempt - 1))
        
        scope.launch {
            delay(delayMs)
            
            if (isOnline()) {
                Log.i(TAG, "Reintentando sincronización (intento $attempt/$MAX_RETRY_ATTEMPTS)")
                startRealtimeSync()
            } else {
                Log.w(TAG, "Sin conexión, reintentando en ${TimeUnit.MILLISECONDS.toSeconds(delayMs)} segundos")
                notifySyncStatusChanged(SyncStatus.OFFLINE)
                attemptRetrySync(attempt + 1)
            }
        }
    }
    
    private fun notifySyncStarted() {
        syncCallbacks.forEach { it.onSyncStarted() }
        notifySyncStatusChanged(SyncStatus.SYNCING)
    }
    
    private fun notifySyncCompleted(routes: List<Ruta>) {
        syncCallbacks.forEach { it.onSyncCompleted(routes) }
        notifySyncStatusChanged(SyncStatus.SUCCESS)
    }
    
    private fun notifySyncFailed(error: String) {
        syncCallbacks.forEach { it.onSyncFailed(error) }
        notifySyncStatusChanged(SyncStatus.FAILED)
    }
    
    private fun notifySyncStatusChanged(status: SyncStatus) {
        syncCallbacks.forEach { it.onSyncStatusChanged(status) }
    }
    
    fun cleanup() {
        stopRealtimeSync()
        scope.cancel()
        syncCallbacks.clear()
    }
}