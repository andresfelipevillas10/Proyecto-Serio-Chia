package com.example.proyecto_definitivo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.CameraPerspective
import com.google.android.libraries.navigation.ArrivalEvent
import com.google.android.libraries.navigation.DisplayOptions
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.RoutingOptions
import com.google.android.libraries.navigation.SupportNavigationFragment
import com.google.android.libraries.navigation.Waypoint

import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * Pantalla de Navegación Profesional usando Google Maps Navigation SDK.
 *
 * Proporciona la experiencia completa de Google Maps (vista 3D, recálculo automático,
 * guía paso a paso) integrada directamente en la aplicación.
 */
class NavegacionActivaActivity : AppCompatActivity() {

    private val TAG = "NavegacionPro"

    // Vistas
    private lateinit var navFragment: SupportNavigationFragment
    private var googleMap: GoogleMap? = null
    private var navigator: Navigator? = null

    private lateinit var tvActiveNextStop: TextView
    private lateinit var tvActiveDistance: TextView
    private lateinit var tvActiveTime: TextView
    private lateinit var btnEndRoute: MaterialButton
    private lateinit var btnSafetyExit: MaterialButton
    private lateinit var fabReportarNovedad: FloatingActionButton
    private lateinit var fabPausarRuta: FloatingActionButton

    // Variables de Ruta
    private val db = FirebaseDatabase.getInstance().reference
    private var rutaId: String = ""
    private var recorridoId: String = ""
    private var rutaRadio: Float = 30f

    // Lógica de tracking
    private val listaPuntos = mutableListOf<PuntoRuta>()
    private var indicePuntoActual = 0
    private var tiempoInicioRuta: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navegacion_activa)

        initViews()
        receiveIntentData()
        setupNavigationSDK()
        setupBackPressHandler()

        tiempoInicioRuta = System.currentTimeMillis()
    }

    private fun initViews() {
        tvActiveNextStop = findViewById(R.id.tvActiveNextStop)
        tvActiveDistance = findViewById(R.id.tvActiveDistance)
        tvActiveTime = findViewById(R.id.tvActiveTime)
        btnEndRoute = findViewById(R.id.btnEndRoute)
        btnSafetyExit = findViewById(R.id.btnSafetyExit)
        fabReportarNovedad = findViewById(R.id.fabReportarNovedad)
        fabPausarRuta = findViewById(R.id.fabPausarRuta)

        btnEndRoute.setOnClickListener { solicitarMotivoFinalizacion(false) }
        btnSafetyExit.setOnClickListener { mostrarAlertaSeguridad() }
        fabPausarRuta.setOnClickListener { togglePausa() }
    }

    private fun receiveIntentData() {
        rutaId = intent.getStringExtra("rutaId") ?: ""
        recorridoId = intent.getStringExtra("recorridoId") ?: ""
        rutaRadio = intent.getFloatExtra("rutaRadio", 30f)
    }

    /**
     * Inicializa el Navigation SDK de Google Maps.
     */
    private fun setupNavigationSDK() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        // 1. Obtener el fragmento de navegación
        navFragment = supportFragmentManager.findFragmentById(R.id.mapNavegacion) as SupportNavigationFragment

        // 2. Mostrar T&C requeridos por Google
        NavigationApi.showTermsAndConditionsDialog(this, "my_company", "Zenda") { accepted ->
            if (accepted) {
                initializeNavigationApi()
            } else {
                Toast.makeText(this, "Debes aceptar los términos para navegar", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initializeNavigationApi() {
        NavigationApi.getNavigator(this, object : NavigationApi.NavigatorListener {
            override fun onNavigatorReady(nav: Navigator) {
                navigator = nav
                setupNavigator()

                navFragment.getMapAsync { map ->
                    googleMap = map
                    setupMapUI()
                    cargarPuntosRutaYEmpezar()
                }
            }

            override fun onError(@NavigationApi.ErrorCode errorCode: Int) {
                Log.e(TAG, "Error inicializando Navigator: $errorCode")
                Toast.makeText(this@NavegacionActivaActivity, "Error de Navegación: $errorCode", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setupNavigator() {
        navigator?.let { nav ->
            nav.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE)

            // Listener para cuando llegamos a un waypoint (parada)
            nav.addArrivalListener(object : Navigator.ArrivalListener {
                override fun onArrival(event: ArrivalEvent) {
                    val waypointTitle = event.waypoint?.title ?: return
                    val punto = listaPuntos.find { it.id == waypointTitle }
                    punto?.let { registrarLlegadaEnFirebase(it) }

                    indicePuntoActual++
                    if (indicePuntoActual < listaPuntos.size) {
                        actualizarUIProximaParada()
                        // Continuar al siguiente punto
                        nav.continueToNextDestination()
                    } else {
                        tvActiveNextStop.text = "Ruta completada"
                        solicitarMotivoFinalizacion(true)
                    }
                }
            })

            nav.addRemainingTimeOrDistanceChangedListener(30, 100, object : Navigator.RemainingTimeOrDistanceChangedListener {
                override fun onRemainingTimeOrDistanceChanged() {
                    // Actualizar UI extra si es necesario
                }
            })
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupMapUI() {
        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = false
            setPadding(0, 0, 0, 600)
            followMyLocation(CameraPerspective.TILTED)
        }
    }

    private fun cargarPuntosRutaYEmpezar() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.child("rutas").child(currentUserId).child(rutaId).child("puntos").get().addOnSuccessListener { snapshot ->
            listaPuntos.clear()
            val waypoints = mutableListOf<Waypoint>()
            
            for (puntoSnap in snapshot.children) {
                val punto = puntoSnap.getValue(PuntoRuta::class.java)
                punto?.let { 
                    listaPuntos.add(it)
                    val wp = Waypoint.builder()
                        .setLatLng(it.latitud, it.longitud)
                        .setTitle(it.id) // Usamos el ID para identificarlo
                        .build()
                    waypoints.add(wp)
                }
            }
            listaPuntos.sortBy { it.orden }
            
            if (waypoints.isNotEmpty()) {
                iniciarNavegacionGoogle(waypoints)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun iniciarNavegacionGoogle(waypoints: List<Waypoint>) {
        navigator?.let { nav ->
            val routingOptions = RoutingOptions().apply {
                travelMode(RoutingOptions.TravelMode.DRIVING)
            }

            nav.setDestinations(waypoints, routingOptions, DisplayOptions())
                .setOnResultListener { routeStatus ->
                    if (routeStatus == Navigator.RouteStatus.OK) {
                        nav.startGuidance()
                        actualizarUIProximaParada()
                    } else {
                        Log.e(TAG, "Error al establecer destinos: $routeStatus")
                    }
                }
        }
    }

    private fun actualizarUIProximaParada() {
        if (indicePuntoActual < listaPuntos.size) {
            val siguiente = listaPuntos[indicePuntoActual]
            tvActiveNextStop.text = siguiente.nombre
        }
    }

    private fun registrarLlegadaEnFirebase(punto: PuntoRuta) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ahora = System.currentTimeMillis()
        
        val puntoRegistrado = mapOf(
            "puntoId" to punto.id,
            "nombre" to punto.nombre,
            "tiempoLlegada" to ahora,
            "estado" to "completado"
        )

        db.child("recorridos").child(currentUserId).child(recorridoId).child("puntosRegistrados").child(punto.id).setValue(puntoRegistrado)
        db.child("users").child(currentUserId).child("ruta_actual").child("puntos_completados").setValue(indicePuntoActual + 1)
        
        Toast.makeText(this, "Llegada a: ${punto.nombre}", Toast.LENGTH_SHORT).show()
    }

    private fun togglePausa() {
        val isNavigating = navigator?.isGuidanceRunning ?: false
        if (isNavigating) {
            navigator?.stopGuidance()
            fabPausarRuta.setImageResource(android.R.drawable.ic_media_play)
            Toast.makeText(this, "Navegación en pausa", Toast.LENGTH_SHORT).show()
        } else {
            navigator?.startGuidance()
            fabPausarRuta.setImageResource(android.R.drawable.ic_media_pause)
            Toast.makeText(this, "Navegación reanudada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun solicitarMotivoFinalizacion(esAutomatico: Boolean) {
        val editText = EditText(this).apply { hint = "Justificación" }
        AlertDialog.Builder(this)
            .setTitle("Finalizar Ruta")
            .setView(editText)
            .setPositiveButton("Terminar") { _, _ ->
                finalizarRuta(esAutomatico, editText.text.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun finalizarRuta(esAutomatico: Boolean, motivo: String) {
        navigator?.stopGuidance()
        navigator?.clearDestinations()
        
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val actualizacion = mapOf(
            "finTiempo" to System.currentTimeMillis(),
            "estado" to if (esAutomatico) "finalizado_auto" else "finalizado_manual",
            "motivoTermino" to motivo
        )
        
        db.child("recorridos").child(currentUserId).child(recorridoId).updateChildren(actualizacion)
        db.child("users").child(currentUserId).child("ruta_actual").removeValue()
        
        startActivity(Intent(this, ResumenRecorridoActivity::class.java).apply {
            putExtra("recorridoId", recorridoId)
        })
        finish()
    }

    private fun mostrarAlertaSeguridad() {
        AlertDialog.Builder(this)
            .setTitle("Salir al Hub")
            .setMessage("¿Deseas minimizar la navegación?")
            .setPositiveButton("Sí, Salir") { _, _ -> finish() }
            .setNegativeButton("Volver", null)
            .show()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (Settings.canDrawOverlays(this@NavegacionActivaActivity)) {
                    val serviceIntent = Intent(this@NavegacionActivaActivity, FloatingRouteService::class.java).apply {
                        putExtra("rutaId", rutaId)
                        putExtra("rutaNombre", intent.getStringExtra("rutaNombre"))
                        putExtra("recorridoId", recorridoId)
                    }
                    startForegroundService(serviceIntent)
                    finish()
                } else {
                    mostrarAlertaSeguridad()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        navigator?.cleanup()
    }
}