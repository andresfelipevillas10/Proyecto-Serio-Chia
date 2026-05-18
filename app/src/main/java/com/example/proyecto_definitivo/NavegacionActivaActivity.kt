package com.example.proyecto_definitivo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.CameraPerspective
import com.google.android.libraries.navigation.ArrivalEvent
import com.google.android.libraries.navigation.DisplayOptions
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.RoutingOptions
import com.google.android.libraries.navigation.SupportNavigationFragment
import com.google.android.libraries.navigation.Waypoint
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Pantalla de Navegación Profesional usando Google Maps Navigation SDK.
 * Integrada con sistema de reportes de emergencia y UI Predictiva de Paraderos.
 */
class NavegacionActivaActivity : AppCompatActivity() {

    private val TAG = "NavegacionPro"

    // Vistas
    private lateinit var navFragment: SupportNavigationFragment
    private var googleMap: GoogleMap? = null
    private var navigator: Navigator? = null
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient

    private lateinit var tvActiveNextStop: TextView
    private lateinit var tvActiveDistance: TextView
    private lateinit var tvActiveTime: TextView
    private lateinit var btnEndRoute: MaterialButton
    private lateinit var btnSafetyExit: MaterialButton
    private lateinit var fabReportarNovedad: FloatingActionButton
    private lateinit var fabPausarRuta: FloatingActionButton

    // 🔥 FASE 2: Vistas de la Tarjeta Mágica Predictiva
    private lateinit var cardParaderoMagico: View
    private lateinit var tvNombreParaderoProximo: TextView
    private lateinit var tvPasajerosSuben: TextView
    private lateinit var tvPasajerosBajan: TextView
    private var tarjetaVisible = false
    
    // Aforo (Fase 9)
    private lateinit var tvAforoActual: TextView
    private lateinit var btnSumarPasajero: TextView
    private lateinit var btnRestarPasajero: TextView
    private var pasajerosActuales = 0
    private var cupoSentados = 40

    // 🔥 FASE 3: Variables del Guardián
    private var isDeviating = false
    private var rutaInicializada = false

    // Pantalla de Carga
    private lateinit var loadingOverlay: View
    private lateinit var tvLoadingStatus: TextView

    // Voz de Dios
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech

    // Variables de Ruta
    private val db = FirebaseDatabase.getInstance().reference
    private var rutaId: String = ""
    private var recorridoId: String = ""
    private var rutaRadio: Float = 30f

    // Lógica de tracking
    private val listaPuntos = mutableListOf<PuntoRuta>()
    private var indicePuntoActual = 0
    private var tiempoInicioRuta: Long = 0L

    // Chip de pasajeros esperando en la próxima parada
    private lateinit var layoutPasajerosEsperando: LinearLayout
    private lateinit var tvPasajerosEsperando: TextView
    private var paraderoListener: ValueEventListener? = null
    private var paraderoListenerRef: DatabaseReference? = null
    private var currentParaderoId = ""

    // STEP 6: Variables Temporales para Evidencia
    private var pendingIncidentType: IncidentType? = null
    private var pendingPriority: Priority? = null
    private var pendingDesc: String = ""

    // STEP 6: El Atrapador de la Cámara
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photoPath = result.data?.getStringExtra("PHOTO_PATH")
            if (pendingIncidentType != null && pendingPriority != null) {
                capturarIncidenteYGuardar(pendingIncidentType!!, pendingPriority!!, pendingDesc, photoPath)
            }
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            if (pendingIncidentType != null) {
                val intent = Intent(this, CameraEvidenceActivity::class.java)
                cameraLauncher.launch(intent)
            }
        } else {
            Toast.makeText(this, "Permiso de cámara denegado. No se puede tomar evidencia.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navegacion_activa)

        initViews()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        receiveIntentData()
        setupVoiceLogic()
        setupNavigationSDK()
        setupBackPressHandler()

        tiempoInicioRuta = System.currentTimeMillis()

        if (androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1002)
        }

        iniciarLatidoActividad()
    }

    private fun iniciarLatidoActividad() {
        lifecycleScope.launch {
            while (true) {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId != null && rutaId.isNotEmpty()) {
                    db.child("users").child(currentUserId).child("ruta_actual")
                        .child("ultimaActividad").setValue(System.currentTimeMillis())
                }
                delay(30000) // Cada 30 segundos
            }
        }
    }

    private fun initViews() {
        tvActiveNextStop = findViewById(R.id.tvActiveNextStop)
        tvActiveDistance = findViewById(R.id.tvActiveDistance)
        tvActiveTime = findViewById(R.id.tvActiveTime)
        btnEndRoute = findViewById(R.id.btnEndRoute)
        btnSafetyExit = findViewById(R.id.btnSafetyExit)
        fabReportarNovedad = findViewById(R.id.fabReportarNovedad)
        fabPausarRuta = findViewById(R.id.fabPausarRuta)

        // 🔥 FASE 2: Inicializar vistas de la tarjeta mágica
        cardParaderoMagico = findViewById(R.id.cardParaderoMagico)
        tvNombreParaderoProximo = findViewById(R.id.tvNombreParaderoProximo)
        tvPasajerosSuben = findViewById(R.id.tvPasajerosSuben)
        tvPasajerosBajan = findViewById(R.id.tvPasajerosBajan)

        loadingOverlay = findViewById(R.id.loadingOverlay)
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus)
        
        tvAforoActual = findViewById(R.id.tvAforoActual)
        btnSumarPasajero = findViewById(R.id.btnSumarPasajero)
        btnRestarPasajero = findViewById(R.id.btnRestarPasajero)

        btnSumarPasajero.setOnClickListener { cambiarAforo(1) }
        btnRestarPasajero.setOnClickListener { cambiarAforo(-1) }

        findViewById<View>(R.id.cardAforoControl).setOnClickListener {
            val intent = Intent(this, AsientosActivity::class.java).apply {
                putExtra("rutaId", rutaId)
                putExtra("conductorId", FirebaseAuth.getInstance().currentUser?.uid)
            }
            startActivity(intent)
        }

        layoutPasajerosEsperando = findViewById(R.id.layoutPasajerosEsperando)
        tvPasajerosEsperando = findViewById(R.id.tvPasajerosEsperando)

        btnEndRoute.setOnClickListener { solicitarMotivoFinalizacion(false) }
        btnSafetyExit.setOnClickListener { mostrarAlertaSeguridad() }
        fabPausarRuta.setOnClickListener { togglePausa() }

        fabReportarNovedad.setOnClickListener {
            mostrarPanelMetal()
        }

        fabReportarNovedad.setOnLongClickListener {
            val mensaje = "Comando de voz activado. Por favor indique cuál."
            hablar(mensaje)

            lifecycleScope.launch {
                delay(4000)
                val intentVoz = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CO")
                }
                speechRecognizer.startListening(intentVoz)
            }
            true
        }
    }

    private fun receiveIntentData() {
        rutaId = intent.getStringExtra("rutaId") ?: ""
        recorridoId = intent.getStringExtra("recorridoId") ?: ""
        rutaRadio = intent.getFloatExtra("rutaRadio", 30f)
    }

    private fun setupNavigationSDK() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        navFragment = supportFragmentManager.findFragmentById(R.id.mapNavegacion) as SupportNavigationFragment

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
        tvLoadingStatus.text = "Iniciando SDK de Navegación..."
        loadingOverlay.visibility = View.VISIBLE
        
        NavigationApi.getNavigator(this, object : NavigationApi.NavigatorListener {
            override fun onNavigatorReady(nav: Navigator) {
                tvLoadingStatus.text = "Configurando el mapa..."
                navigator = nav
                setupNavigator()

                navFragment.getMapAsync { map ->
                    googleMap = map
                    setupMapUI()
                    cargarPuntosRutaYEmpezar()
                    cargarDatosCapacidad()
                }
            }

            override fun onError(@NavigationApi.ErrorCode errorCode: Int) {
                loadingOverlay.visibility = View.GONE
                val errorMsg = when (errorCode) {
                    NavigationApi.ErrorCode.NOT_AUTHORIZED -> "API Key no autorizada o falta SHA-1 válido."
                    NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED -> "Términos no aceptados."
                    NavigationApi.ErrorCode.NETWORK_ERROR -> "Sin conexión a Internet."
                    NavigationApi.ErrorCode.LOCATION_PERMISSION_MISSING -> "Faltan permisos de ubicación."
                    else -> "Error desconocido ($errorCode)"
                }
                Log.e(TAG, "Navigation SDK Error: $errorMsg")
                Toast.makeText(this@NavegacionActivaActivity, "Error SDK: $errorMsg", Toast.LENGTH_LONG).show()
                hablar("Error al iniciar el mapa. Verifica tu conexión o los permisos de Google.")
            }
        })
    }

    private fun setupNavigator() {
        navigator?.let { nav ->
            nav.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE)

            // Listener de llegadas a paraderos
            nav.addArrivalListener { event ->
                val waypointTitle = event.waypoint?.title ?: return@addArrivalListener
                val punto = listaPuntos.find { it.id == waypointTitle }
                punto?.let { registrarLlegadaEnFirebase(it) }

                indicePuntoActual++
                if (indicePuntoActual < listaPuntos.size) {
                    actualizarUIProximaParada()
                    nav.continueToNextDestination()
                } else {
                    tvActiveNextStop.text = "Ruta completada"
                    solicitarMotivoFinalizacion(true)
                }
            }

            // 🔥 FASE 2: Radar de proximidad predictivo (10m de distancia o 5s de tiempo)
            nav.addRemainingTimeOrDistanceChangedListener(10, 5, object : Navigator.RemainingTimeOrDistanceChangedListener {
                override fun onRemainingTimeOrDistanceChanged() {
                    val distanceToNextStop = nav.currentTimeAndDistance?.meters ?: Int.MAX_VALUE

                    // Si entramos en la zona de aterrizaje (< 300 metros)
                    if (distanceToNextStop <= 300) {
                        mostrarTarjetaParadero()
                    } else {
                        ocultarTarjetaParadero()
                    }
                }
            })

            // 🔥 FASE 3: El Perro Guardián (Tolerancia Cero)
            nav.addRouteChangedListener {
                // Si la ruta ya estaba inicializada y no estamos ya en proceso de desvío
                if (rutaInicializada && !isDeviating) {
                    dispararAlarmaDesvio()
                }
            }
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
        tvLoadingStatus.text = "Descargando ruta del búnker..."
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
                        .setTitle(it.id)
                        .build()
                    waypoints.add(wp)
                }
            }
            listaPuntos.sortBy { it.orden }
            if (waypoints.isNotEmpty()) iniciarNavegacionGoogle(waypoints)
        }
    }

    private fun cargarDatosCapacidad() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.child("rutas").child(uid).child(rutaId).get().addOnSuccessListener { snapshot ->
            cupoSentados = snapshot.child("cupoSentados").getValue(Int::class.java) ?: 40
            pasajerosActuales = snapshot.child("pasajerosActuales").getValue(Int::class.java) ?: 0
            actualizarUIAforo()
        }
    }

    private fun cambiarAforo(delta: Int) {
        val nuevoTotal = pasajerosActuales + delta
        if (nuevoTotal in 0..(cupoSentados + 20)) { // 20 de margen para gente de pie
            pasajerosActuales = nuevoTotal
            actualizarUIAforo()
            
            // Sincronizar con Firebase (User node y Rutas node)
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            db.child("users").child(uid).child("ruta_actual").child("pasajerosActuales").setValue(pasajerosActuales)
            db.child("rutas").child(uid).child(rutaId).child("pasajerosActuales").setValue(pasajerosActuales)
        }
    }

    private fun actualizarUIAforo() {
        tvAforoActual.text = "$pasajerosActuales/$cupoSentados"
        if (pasajerosActuales >= cupoSentados) {
            tvAforoActual.setTextColor(android.graphics.Color.parseColor("#ba1a1a"))
        } else {
            tvAforoActual.setTextColor(android.graphics.Color.parseColor("#001e40"))
        }
    }

    @SuppressLint("MissingPermission")
    private fun iniciarNavegacionGoogle(waypoints: List<Waypoint>) {
        tvLoadingStatus.text = "Calculando ruta estricta..."
        navigator?.let { nav ->
            val routingOptions = RoutingOptions().apply { travelMode(RoutingOptions.TravelMode.DRIVING) }
            nav.setDestinations(waypoints, routingOptions, DisplayOptions())
                .setOnResultListener { routeStatus ->
                    if (routeStatus == Navigator.RouteStatus.OK) {
                        loadingOverlay.visibility = View.GONE
                        nav.startGuidance()
                        actualizarUIProximaParada()
                        
                        // 🔥 FASE 3: Darle 3 segundos al sistema antes de armar al Guardián
                        lifecycleScope.launch {
                            kotlinx.coroutines.delay(3000)
                            rutaInicializada = true
                        }
                    } else {
                        loadingOverlay.visibility = View.GONE
                        Toast.makeText(this, "No se pudo trazar la ruta: $routeStatus", Toast.LENGTH_LONG).show()
                        hablar("Error al trazar la ruta. Reinicia el recorrido.")
                    }
                }
        }
    }

    // 🔥 FASE 3: Lógica del Guardián de Desvíos
    private fun dispararAlarmaDesvio() {
        isDeviating = true
        hablar("Alerta. Desvío no autorizado detectado. Ingrese código de autorización para continuar.")
        
        // Pausamos la navegación visual para no seguir guiando por el desvío sin permiso
        navigator?.stopGuidance()

        val input = EditText(this).apply {
            hint = "Contraseña (zenda123)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("🚨 DESVÍO DETECTADO 🚨")
            .setMessage("Has abandonado la ruta estricta. Ingrese la contraseña de administrador para autorizar el recálculo.")
            .setView(input)
            .setCancelable(false) // In-cancelable
            .setPositiveButton("Autorizar", null) // Lo sobreescribimos abajo para evitar cierre por defecto
            .create()

        dialog.show()

        // Evitar que el diálogo se cierre si la contraseña es incorrecta
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val pass = input.text.toString()
            if (pass == "zenda123") {
                Toast.makeText(this, "Desvío Autorizado", Toast.LENGTH_SHORT).show()
                isDeviating = false
                navigator?.startGuidance() // Retomamos la navegación con la nueva ruta recalculada
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                hablar("Contraseña incorrecta.")
                input.text.clear()
            }
        }
    }

    // 🔥 FASE 2: Lógica de la Tarjeta Mágica
    private fun mostrarTarjetaParadero() {
        if (!tarjetaVisible && indicePuntoActual < listaPuntos.size) {
            tarjetaVisible = true
            val siguienteParadero = listaPuntos[indicePuntoActual]

            tvNombreParaderoProximo.text = "Siguiente: ${siguienteParadero.nombre}"

            // MOCK DATA: Simulación de pasajeros con datos random
            tvPasajerosSuben.text = (1..6).random().toString()
            tvPasajerosBajan.text = (0..4).random().toString()

            // Animación fluida desde arriba hacia abajo
            cardParaderoMagico.visibility = View.VISIBLE
            cardParaderoMagico.translationY = -200f
            cardParaderoMagico.alpha = 0f
            cardParaderoMagico.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(400)
                .start()
        }
    }

    private fun ocultarTarjetaParadero() {
        if (tarjetaVisible) {
            tarjetaVisible = false
            // Animación de repliegue
            cardParaderoMagico.animate()
                .translationY(-200f)
                .alpha(0f)
                .setDuration(300)
                .withEndAction { cardParaderoMagico.visibility = View.GONE }
                .start()
        }
    }

    private fun preguntarPorEvidencia(tipo: IncidentType, prioridad: Priority, descBase: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Evidencia del Reporte")
        builder.setMessage("¿Deseas adjuntar una fotografía del incidente o enviarlo inmediatamente?")

        builder.setPositiveButton("📷 TOMAR FOTO") { dialog, _ ->
            pendingIncidentType = tipo
            pendingPriority = prioridad
            pendingDesc = descBase

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } else {
                val intent = Intent(this, CameraEvidenceActivity::class.java)
                cameraLauncher.launch(intent)
            }
            dialog.dismiss()
        }

        builder.setNeutralButton("📝 SOLO ENVIAR") { dialog, _ ->
            capturarIncidenteYGuardar(tipo, prioridad, descBase, null)
            dialog.dismiss()
        }

        builder.show()
    }

    @SuppressLint("MissingPermission")
    private fun capturarIncidenteYGuardar(tipo: IncidentType, prioridad: Priority, desc: String, photoPath: String? = null) {
        Toast.makeText(this, "Capturando ubicación...", Toast.LENGTH_SHORT).show()

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val nuevoReporte = IncidentReport(
                    driverId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonimo",
                    latitude = location.latitude,
                    longitude = location.longitude,
                    type = tipo,
                    priority = prioridad,
                    description = desc,
                    photoUrl = photoPath
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    val dao = ZendaDatabase.getDatabase(applicationContext).incidentDao()
                    dao.insertReport(nuevoReporte)
                    programarSincronizacion()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@NavegacionActivaActivity, "Reporte enviado al Búnker 🤘", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun programarSincronizacion() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequestBuilder<IncidentSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(syncRequest)
    }

    private fun mostrarPanelMetal() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_incident, null)
        bottomSheetDialog.setContentView(view)

        view.findViewById<MaterialButton>(R.id.btnFuelAccident).setOnClickListener {
            preguntarPorEvidencia(IncidentType.ACCIDENTE, Priority.ALTA, "Choque en ruta")
            bottomSheetDialog.dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btnLightningMech).setOnClickListener {
            preguntarPorEvidencia(IncidentType.MECANICO, Priority.MEDIA, "Falla de motor/llanta")
            bottomSheetDialog.dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btnSeekDestroy).setOnClickListener {
            preguntarPorEvidencia(IncidentType.MECANICO, Priority.ALTA, "SEEK & DESTROY: Bus inoperativo, requiere grúa")
            bottomSheetDialog.dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btnAbort).setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        view.findViewById<MaterialButton>(R.id.btnSOS).setOnClickListener {
            val callIntent = Intent(Intent.ACTION_DIAL).apply {
                data = android.net.Uri.parse("tel:112")
            }
            startActivity(callIntent)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun procesarComandoDeVoz(comando: String) {
        when {
            comando.contains("accidente") || comando.contains("choque") || comando.contains("emergencia") -> {
                hablar("Recibido. Reportando accidente grave al sistema. Mantenga la calma.")
                capturarIncidenteYGuardar(IncidentType.ACCIDENTE, Priority.ALTA, "Reporte por VOZ: Accidente Grave", null)
            }
            comando.contains("falla") || comando.contains("mecánica") || comando.contains("varado") -> {
                hablar("Copiado. Reportando falla mecánica.")
                capturarIncidenteYGuardar(IncidentType.MECANICO, Priority.MEDIA, "Reporte por VOZ: Falla Mecánica", null)
            }
            comando.contains("reemplazo") || comando.contains("grúa") -> {
                hablar("Seek and destroy activado. Solicitando grúa y bus de reemplazo.")
                capturarIncidenteYGuardar(IncidentType.MECANICO, Priority.ALTA, "Reporte por VOZ: SEEK & DESTROY (Reemplazo urgente)", null)
            }
            else -> {
                hablar("Comando no reconocido. Diga accidente, falla mecánica, o reemplazo.")
            }
        }
    }

    private fun setupVoiceLogic() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale("es", "CO")
            }
        }

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    hablar("No copié, intenta de nuevo.")
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        procesarComandoDeVoz(matches[0].lowercase())
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun hablar(mensaje: String) {
        if (::textToSpeech.isInitialized) {
            textToSpeech.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    private fun actualizarUIProximaParada() {
        if (indicePuntoActual < listaPuntos.size) {
            val siguiente = listaPuntos[indicePuntoActual]
            tvActiveNextStop.text = siguiente.nombre
            escucharPasajerosEnParada(siguiente.id)
        }
    }

    private fun escucharPasajerosEnParada(puntoId: String) {
        if (puntoId == currentParaderoId || recorridoId.isEmpty()) return
        paraderoListener?.let { paraderoListenerRef?.removeEventListener(it) }
        currentParaderoId = puntoId
        paraderoListenerRef = db.child("recorridos").child(recorridoId).child("paraderos").child(puntoId)
        paraderoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.child("pasajerosEsperando").getValue(Int::class.java) ?: 0
                if (count > 0) {
                    layoutPasajerosEsperando.visibility = View.VISIBLE
                    tvPasajerosEsperando.text = if (count == 1) "1 persona esperando" else "$count personas esperando"
                } else {
                    layoutPasajerosEsperando.visibility = View.GONE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        paraderoListenerRef!!.addValueEventListener(paraderoListener!!)
    }

    private fun registrarLlegadaEnFirebase(punto: PuntoRuta) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val puntoRegistrado = mapOf(
            "puntoId" to punto.id,
            "nombre" to punto.nombre,
            "tiempoLlegada" to System.currentTimeMillis(),
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
        } else {
            navigator?.startGuidance()
            fabPausarRuta.setImageResource(android.R.drawable.ic_media_pause)
        }
    }

    private fun solicitarMotivoFinalizacion(esAutomatico: Boolean) {
        val editText = EditText(this).apply { hint = "Justificación" }
        AlertDialog.Builder(this)
            .setTitle("Finalizar Ruta")
            .setView(editText)
            .setPositiveButton("Terminar") { _, _ -> finalizarRuta(esAutomatico, editText.text.toString()) }
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
        startActivity(Intent(this, ResumenRecorridoActivity::class.java).apply { putExtra("recorridoId", recorridoId) })
        finish()
    }

    private fun mostrarAlertaSeguridad() {
        AlertDialog.Builder(this)
            .setTitle("Salir al Hub")
            .setMessage("¿Deseas minimizar la navegación? El GPS seguirá activo para que los pasajeros vean la posición del bus.")
            .setPositiveButton("Sí, Salir") { _, _ ->
                // BusLocationService mantiene el GPS activo aunque la app esté en segundo plano
                if (recorridoId.isNotEmpty()) {
                    val svc = Intent(this, BusLocationService::class.java).apply {
                        putExtra(BusLocationService.EXTRA_RECORRIDO_ID, recorridoId)
                    }
                    startForegroundService(svc)
                }
                finish()
            }
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
        if (::speechRecognizer.isInitialized) speechRecognizer.destroy()
        if (::textToSpeech.isInitialized) textToSpeech.shutdown()
        paraderoListener?.let { paraderoListenerRef?.removeEventListener(it) }
    }
}