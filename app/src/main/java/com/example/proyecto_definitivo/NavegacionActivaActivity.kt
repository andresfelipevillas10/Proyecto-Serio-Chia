package com.example.proyecto_definitivo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase



class NavegacionActivaActivity : AppCompatActivity(), OnMapReadyCallback {

    // Vistas de la interfaz moderna
    private lateinit var mapNavegacion: GoogleMap
    private var pathPolyline: Polyline? = null
    private var plannedRoutePolyline: Polyline? = null
    private val visitedPoints = mutableListOf<LatLng>()
    private lateinit var tvActiveNextStop: TextView
    private lateinit var tvActiveDistance: TextView
    private lateinit var tvActiveTime: TextView
    private lateinit var tvActiveSpeed: TextView
    private lateinit var btnEndRoute: MaterialButton
    private lateinit var fabReportarNovedad: FloatingActionButton

    // Variables de Ruta
    private val db = FirebaseDatabase.getInstance().reference
    private var rutaId: String = ""
    private var recorridoId: String = "" // ¡IMPORTANTE! El que recibimos del Pre-Recorrido
    private var rutaRadio: Float = 30f // Metros para detectar llegada

    // Lógica de tracking
    private val listaPuntos = mutableListOf<PuntoRuta>()
    private var indicePuntoActual = 0
    private var tiempoUltimoPunto: Long = 0L

    // Google Location API
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navegacion_activa) // EL DISEÑO MODERNO

        // 1. Enlazar Vistas
        tvActiveNextStop = findViewById(R.id.tvActiveNextStop)
        tvActiveDistance = findViewById(R.id.tvActiveDistance)
        tvActiveTime = findViewById(R.id.tvActiveTime)
        tvActiveSpeed = findViewById(R.id.tvActiveSpeed)
        btnEndRoute = findViewById(R.id.btnEndRoute)
        fabReportarNovedad = findViewById(R.id.fabReportarNovedad)

        // 2. Recibir Datos
        rutaId = intent.getStringExtra("rutaId") ?: ""
        recorridoId = intent.getStringExtra("recorridoId") ?: ""
        rutaRadio = intent.getFloatExtra("rutaRadio", 30f)
        indicePuntoActual = intent.getIntExtra("indice_actual", 0)

        // 3. Configurar Mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapNavegacion) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 4. Configurar Botones
        btnEndRoute.setOnClickListener {
            solicitarClaveFinalizacion()
        }

        fabReportarNovedad.setOnClickListener {
            Toast.makeText(this, "Novedad registrada (En desarrollo)", Toast.LENGTH_SHORT).show()
        }

        // 5. Configurar GPS
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        configurarMotorUbicacion()


        tiempoUltimoPunto = System.currentTimeMillis() // Inicia el reloj
    }

    override fun onMapReady(map: GoogleMap) {
        mapNavegacion = map

        // Configuraciones de conducción
        mapNavegacion.uiSettings.isZoomControlsEnabled = false
        mapNavegacion.uiSettings.isCompassEnabled = true
        mapNavegacion.setPadding(0, 300, 0, 0) // Bajamos el centro del mapa para que no lo tape la tarjeta superior

        // Inicializar el Polyline del recorrido real
        pathPolyline = mapNavegacion.addPolyline(
            PolylineOptions()
                .color(Color.BLUE)
                .width(12f)
                .jointType(JointType.ROUND)
                .startCap(RoundCap())
                .endCap(RoundCap())
        )

        habilitarCapaUbicacion()
        cargarPuntosRuta()
    }

    private fun cargarPuntosRuta() {
        if (rutaId.isEmpty()) return
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.child("rutas").child(currentUserId).child(rutaId).child("puntos").get().addOnSuccessListener { snapshot ->
            listaPuntos.clear()
            for (puntoSnap in snapshot.children) {
                val punto = puntoSnap.getValue(PuntoRuta::class.java)
                punto?.let { listaPuntos.add(it) }
            }
            listaPuntos.sortBy { it.orden }

            dibujarPuntos()
            actualizarTarjetaProximaParada(null)
        }
    }

    private fun dibujarPuntos() {
        mapNavegacion.clear()

        // 1. Dibujar la línea de la ruta planeada
        val polylineOptions = PolylineOptions()
            .color(Color.GRAY)
            .width(10f)
            .pattern(listOf(Dash(20f), Gap(10f))) // Línea punteada para diferenciar

        for (punto in listaPuntos) {
            polylineOptions.add(LatLng(punto.latitud, punto.longitud))
        }
        plannedRoutePolyline = mapNavegacion.addPolyline(polylineOptions)

        // 2. Dibujar Marcadores
        for (punto in listaPuntos) {
            val posicion = LatLng(punto.latitud, punto.longitud)
            val colorMarcador = when (punto.Tipo) {
                "origen" -> BitmapDescriptorFactory.HUE_GREEN
                "fin" -> BitmapDescriptorFactory.HUE_RED
                else -> BitmapDescriptorFactory.HUE_AZURE
            }
            mapNavegacion.addMarker(MarkerOptions()
                .position(posicion)
                .title(punto.nombre)
                .icon(BitmapDescriptorFactory.defaultMarker(colorMarcador)))
        }

        // 3. Restaurar la línea de recorrido real (traveled path)
        pathPolyline = mapNavegacion.addPolyline(
            PolylineOptions()
                .addAll(visitedPoints)
                .color(Color.BLUE)
                .width(12f)
                .jointType(JointType.ROUND)
                .startCap(RoundCap())
                .endCap(RoundCap())
        )
    }

    private fun configurarMotorUbicacion() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).apply {
            setMinUpdateIntervalMillis(2000L)
            // --- ¡NUEVO! ---
            setMinUpdateDistanceMeters(3f) // Solo actualiza si me moví al menos 3 metros reales
        }.build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    procesarUbicacion(location)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun habilitarCapaUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mapNavegacion.isMyLocationEnabled = true
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    private fun procesarUbicacion(location: Location) {
        // --- ¡NUEVO FILTRO ANTI-SALTOS! ---
        // location.accuracy te dice el margen de error en metros.
        // Si el error es mayor a 20 metros, ignoramos este dato porque es "basura".
        if (location.hasAccuracy() && location.accuracy > 20f) {
            return // Abortar: no actualizamos el mapa ni la línea
        }
        // 1. Centrar cámara en el conductor
        val posicionActual = LatLng(location.latitude, location.longitude)
        mapNavegacion.animateCamera(CameraUpdateFactory.newLatLngZoom(posicionActual, 18f))

        // --- NUEVO: Actualizar la línea de recorrido ---
        visitedPoints.add(posicionActual)

        if (pathPolyline == null) {
            pathPolyline = mapNavegacion.addPolyline(
                PolylineOptions()
                    .addAll(visitedPoints)
                    .color(Color.BLUE)
                    .width(12f)
                    .jointType(JointType.ROUND)
                    .startCap(RoundCap())
                    .endCap(RoundCap())
            )
        } else {
            pathPolyline?.points = visitedPoints
        }

        // --- NUEVO: Actualizar posición en tiempo real en Firebase ---
        if (recorridoId.isNotEmpty()) {
            db.child("recorridos").child(recorridoId).updateChildren(mapOf(
                "latitudActual" to location.latitude,
                "longitudActual" to location.longitude
            ))
        }

        // 2. Actualizar Velocímetro
        val velocidadKmH = (location.speed * 3.6).toInt() // Convertir m/s a km/h
        tvActiveSpeed.text = velocidadKmH.toString()

        // 3. Lógica de Paradas
        if (indicePuntoActual >= listaPuntos.size) return

        val puntoEsperado = listaPuntos[indicePuntoActual]
        val resultados = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, puntoEsperado.latitud, puntoEsperado.longitud, resultados)
        val distanciaMetros = resultados[0]

        // Actualizar UI con la distancia restante cada que nos movemos
        actualizarTarjetaProximaParada(distanciaMetros)

        // 4. ¿Llegamos al punto?
        if (distanciaMetros <= rutaRadio) {
            registrarLlegadaEnFirebase(puntoEsperado)
            Toast.makeText(this, getString(R.string.reached_point, puntoEsperado.nombre), Toast.LENGTH_SHORT).show()
            indicePuntoActual++

            if (indicePuntoActual < listaPuntos.size) {
                actualizarTarjetaProximaParada(null) // Prepara el siguiente
            } else {
                tvActiveNextStop.text = "Ruta Completada"
                tvActiveDistance.text = "0m"
                finalizarRecorrido(esAutomatico = true)
            }
        }
    }

    private fun actualizarTarjetaProximaParada(distanciaAproximada: Float?) {
        if (indicePuntoActual < listaPuntos.size) {
            val siguientePunto = listaPuntos[indicePuntoActual]
            tvActiveNextStop.text = siguientePunto.nombre

            if (distanciaAproximada != null) {
                tvActiveDistance.text = "${distanciaAproximada.toInt()}m"
                // Calculo simple de tiempo: asumiendo 30km/h (8.3 m/s)
                val segundos = distanciaAproximada / 8.3f
                val minutos = (segundos / 60).toInt()
                tvActiveTime.text = if (minutos < 1) "aprox. 1 min" else "aprox. $minutos min"
            } else {
                tvActiveDistance.text = "--"
                tvActiveTime.text = "Calculando..."
            }
        }
    }

    private fun registrarLlegadaEnFirebase(punto: PuntoRuta) {
        if (recorridoId.isEmpty()) return
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val ahora = System.currentTimeMillis()
        val tiempoDesdeAnterior = ahora - tiempoUltimoPunto

        val puntoRegistrado = mapOf(
            "puntoId" to punto.id,
            "nombre" to punto.nombre,
            "tiempoLlegada" to ahora,
            "tiempoDesdeAnteriorMs" to tiempoDesdeAnterior
        )

        db.child("recorridos").child(recorridoId).child("puntosRegistrados").child(punto.id).setValue(puntoRegistrado)

        // Actualizar el progreso en users/{userId}/ruta_actual
        val updates = mapOf(
            "puntos_completados" to (indicePuntoActual + 1),
            "indice_actual" to (indicePuntoActual + 1)
        )
        db.child("users").child(currentUserId).child("ruta_actual").updateChildren(updates)

        tiempoUltimoPunto = ahora
    }

    private fun solicitarClaveFinalizacion() {
        val editText = EditText(this).apply { hint = "Ingrese la clave" }
        AlertDialog.Builder(this)
            .setTitle("Finalizar Ruta")
            .setMessage("Digite la clave maestra")
            .setView(editText)
            .setPositiveButton("Validar") { _, _ ->
                if (editText.text.toString().trim() == "1234") { // Usa tu Constante aquí
                    finalizarRecorrido(esAutomatico = false)
                } else {
                    Toast.makeText(this, "Clave incorrecta", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun finalizarRecorrido(esAutomatico: Boolean) {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (recorridoId.isNotEmpty()) {
            val estadoFinal = if (esAutomatico) "finalizado_automatico" else "finalizado_manual"
            val actualizacion = mapOf(
                "finTiempo" to System.currentTimeMillis(),
                "estado" to estadoFinal
            )
            db.child("recorridos").child(recorridoId).updateChildren(actualizacion).addOnSuccessListener {
                // Limpiar la ruta actual del usuario
                if (currentUserId.isNotEmpty()) {
                    db.child("users").child(currentUserId).child("ruta_actual").removeValue()
                }

                Toast.makeText(this, getString(R.string.route_finished), Toast.LENGTH_SHORT).show()

                val intent = Intent(this, ResumenRecorridoActivity::class.java)
                intent.putExtra("recorridoId", recorridoId)
                startActivity(intent)

                finish()
            }
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}