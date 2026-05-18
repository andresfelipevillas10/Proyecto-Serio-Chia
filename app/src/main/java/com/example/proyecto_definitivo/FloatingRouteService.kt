package com.example.proyecto_definitivo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Servicio en primer plano que muestra una ventana flotante (overlay) con el
 * estado de la ruta activa. Permite al conductor ver el progreso sin abrir la app.
 *
 * Requiere el permiso `SYSTEM_ALERT_WINDOW` (solicitado desde la actividad que lo lanza).
 *
 * La ventana flotante es draggable (se puede mover con el dedo) y contiene:
 * - Nombre de la ruta
 * - Barra de progreso
 * - Conteo de paradas
 * - Botón para reabrir la navegación completa
 * - Botón para cerrar la miniatura
 */
class FloatingRouteService : Service() {

    companion object {
        const val CHANNEL_ID = "floating_route_channel"
        const val NOTIFICATION_ID = 2001
        const val EXTRA_RUTA_ID = "rutaId"
        const val EXTRA_RUTA_NOMBRE = "rutaNombre"
        const val EXTRA_RECORRIDO_ID = "recorridoId"
        const val EXTRA_RUTA_RADIO = "rutaRadio"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private var firebaseListener: ValueEventListener? = null

    // Datos de la ruta pasados via Intent extras
    private var rutaId = ""
    private var rutaNombre = ""
    private var recorridoId = ""
    private var rutaRadio = 30f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Extraer datos del intent
        rutaId = intent?.getStringExtra(EXTRA_RUTA_ID) ?: ""
        rutaNombre = intent?.getStringExtra(EXTRA_RUTA_NOMBRE) ?: ""
        recorridoId = intent?.getStringExtra(EXTRA_RECORRIDO_ID) ?: ""
        rutaRadio = intent?.getFloatExtra(EXTRA_RUTA_RADIO, 30f) ?: 30f

        // Lanzar como foreground service con notificación
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Crear y mostrar la ventana flotante
        showFloatingWindow()

        // Escuchar cambios en Firebase para actualizar progreso
        listenToRouteProgress()

        return START_NOT_STICKY
    }

    /**
     * Crea y muestra la ventana flotante sobre otras aplicaciones.
     * La ventana es draggable: el usuario puede moverla con el dedo.
     */
    private fun showFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.layout_floating_route, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 200
        }

        // Configurar UI inicial
        val tvRouteName = floatingView.findViewById<TextView>(R.id.tvFloatingRouteName)
        val tvProgress = floatingView.findViewById<TextView>(R.id.tvFloatingProgress)
        val btnMaximize = floatingView.findViewById<MaterialButton>(R.id.btnFloatingMaximize)
        val btnClose = floatingView.findViewById<ImageButton>(R.id.btnFloatingClose)

        tvRouteName.text = rutaNombre
        tvProgress.text = "Cargando..."

        // Botón Maximizar → abre NavegacionActivaActivity
        btnMaximize.setOnClickListener {
            val navIntent = Intent(this, NavegacionActivaActivity::class.java).apply {
                putExtra("rutaId", rutaId)
                putExtra("rutaNombre", rutaNombre)
                putExtra("recorridoId", recorridoId)
                putExtra("rutaRadio", rutaRadio)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(navIntent)
            stopSelf()
        }

        // Botón Cerrar → cierra la miniatura (la ruta sigue activa en Firebase)
        btnClose.setOnClickListener {
            stopSelf()
        }

        // Hacer la ventana draggable
        setupDragBehavior(floatingView, params)

        windowManager.addView(floatingView, params)
    }

    /**
     * Permite mover la ventana flotante arrastrándola con el dedo.
     */
    private fun setupDragBehavior(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX - (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Escucha cambios en Firebase para actualizar el progreso en la ventana flotante
     * en tiempo real (puntos completados, total de puntos).
     */
    private fun listenToRouteProgress() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("users")
            .child(userId).child("ruta_actual")

        firebaseListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    stopSelf() // La ruta fue finalizada externamente
                    return
                }

                val completados = snapshot.child("puntos_completados").getValue(Int::class.java) ?: 0
                val total = snapshot.child("total_puntos").getValue(Int::class.java) ?: 1

                try {
                    val tvProgress = floatingView.findViewById<TextView>(R.id.tvFloatingProgress)
                    val progressBar = floatingView.findViewById<ProgressBar>(R.id.progressFloating)

                    tvProgress.text = "$completados/$total paradas"
                    progressBar.progress = if (total > 0) (completados * 100) / total else 0
                } catch (_: Exception) { }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        ref.addValueEventListener(firebaseListener!!)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ruta Activa",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Muestra el progreso de la ruta activa"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, HomeRutasActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ruta activa: $rutaNombre")
            .setContentText("Toca para abrir Zenda")
            .setSmallIcon(R.drawable.ic_navigation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remover la vista flotante
        try {
            if (::floatingView.isInitialized) {
                windowManager.removeView(floatingView)
            }
        } catch (_: Exception) { }

        // Remover listener de Firebase
        firebaseListener?.let {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseDatabase.getInstance().getReference("users")
                .child(userId).child("ruta_actual")
                .removeEventListener(it)
        }
    }
}
