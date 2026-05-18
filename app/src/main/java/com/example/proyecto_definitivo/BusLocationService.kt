package com.example.proyecto_definitivo

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase

class BusLocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val db = FirebaseDatabase.getInstance().reference
    private var recorridoId = ""
    private var locationUpdatesActive = false

    companion object {
        const val CHANNEL_ID = "bus_location_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_RECORRIDO_ID = "recorridoId"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        crearCanalNotificacion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        recorridoId = intent?.getStringExtra(EXTRA_RECORRIDO_ID) ?: recorridoId
        startForeground(NOTIFICATION_ID, construirNotificacion())
        iniciarActualizacionUbicacion()
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun iniciarActualizacionUbicacion() {
        if (locationUpdatesActive) return
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000L)
            .setMinUpdateIntervalMillis(3000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                if (recorridoId.isNotEmpty()) {
                    db.child("recorridos").child(recorridoId).updateChildren(
                        mapOf(
                            "latitudActual" to location.latitude,
                            "longitudActual" to location.longitude
                        )
                    )
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            locationUpdatesActive = true
        }
    }

    private fun crearCanalNotificacion() {
        val canal = NotificationChannel(
            CHANNEL_ID,
            "GPS del Bus en Ruta",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mantiene el GPS activo mientras el conductor tiene la app en segundo plano"
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(canal)
    }

    private fun construirNotificacion(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, NavegacionActivaActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ruta en curso")
            .setContentText("GPS activo — los pasajeros pueden ver su posicion")
            .setSmallIcon(R.drawable.ic_bus)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
