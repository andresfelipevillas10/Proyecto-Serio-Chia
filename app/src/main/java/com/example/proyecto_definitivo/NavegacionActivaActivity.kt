package com.example.proyecto_definitivo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
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
import com.google.firebase.database.*

class NavegacionActivaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapNavegacion: GoogleMap
    private var pathPolyline: Polyline? = null
    private var plannedRoutePolyline: Polyline? = null
    private val visitedPoints = mutableListOf<LatLng>()
    private lateinit var tvActiveNextStop: TextView
    private lateinit var tvActiveDistance: TextView
    private lateinit var tvActiveTime: TextView
    private lateinit var tvActiveSpeed: TextView
    private lateinit var btnEndRoute: MaterialButton
    private lateinit var btnSafetyExit: MaterialButton
    private lateinit var fabReportarNovedad: FloatingActionButton

    // Chip de pasajeros esperando
    private lateinit var layoutPasajerosEsperando: LinearLayout
    private lateinit var tvPasajerosEsperando: TextView

    private val db = FirebaseDatabase.getInstance().reference
    private var rutaId: String = ""
    private var recorridoId: String = ""
    private var rutaRadio: Float = 30f

    private val listaPuntos = mutableListOf<PuntoRuta>()
    private var indicePuntoActual = 0
    private var tiempoUltimoPunto: Long = 0L

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // Listener para pasajeros esperando en la próxima parada
    private var paraderoListener: ValueEventListener? = null
    private var paraderoListenerRef: DatabaseReference? = null
    private var currentParaderoId = ""

    // Controla si la ruta terminó para no detener el servicio accidentalmente
    private var routeFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navegacion_activa)

        tvActiveNextStop = findViewById(R.id.tvActiveNextStop)
        tvActiveDistance = findViewById(R.id.tvActiveDistance)
        tvActiveTime = findViewById(R.id.tvActiveTime)
        tvActiveSpeed = findViewById(R.id.tvActiveSpeed)
        btnEndRoute = findViewById(R.id.btnEndRoute)
        btnSafetyExit = findViewById(R.id.btnSafetyExit)
        fabReportarNovedad = findViewById(R.id.fabReportarNovedad)
        layoutPasajerosEsperando = findViewById(R.id.layoutPasajerosEsperando)
        tvPasajerosEsperando = findViewById(R.id.tvPasajerosEsperando)

        rutaId = intent.getStringExtra("rutaId") ?: ""
        recorridoId = intent.getStringExtra("recorridoId") ?: ""
        rutaRadio = intent.getFloatExtra("rutaRadio", 30f)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapNavegacion) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnEndRoute.setOnClickListener { solicitarClaveFinalizacion() }
        btnSafetyExit.setOnClickListener { mostrarAlertaSeguridad() }
        fabReportarNovedad.setOnClickListener {
            Toast.makeText(this, "Novedad registrada (En desarrollo)", Toast.LENGTH_SHORT).show()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        configurarMotorUbicacion()
        tiempoUltimoPunto = System.currentTimeMillis()

        // Iniciar servicio en segundo plano para mantener GPS activo si el conductor sale
        iniciarServicioSegundoPlano()
    }

    private fun iniciarServicioSegundoPlano() {
        if (recorridoId.isEmpty()) return
        val serviceIntent = Intent(this, BusLocationService::class.java).apply {
            putExtra(BusLocationService.EXTRA_RECORRIDO_ID, recorridoId)
        }
        startForegroundService(serviceIntent)
    }

    private fun detenerServicioSegundoPlano() {
        stopService(Intent(this, BusLocationService::class.java))
    }

    override fun onMapReady(map: GoogleMap) {
        mapNavegacion = map
        mapNavegacion.uiSettings.isZoomControlsEnabled = false
        mapNavegacion.uiSettings.isCompassEnabled = true
        mapNavegacion.setPadding(0, 300, 0, 0)

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

        val polylineOptions = PolylineOptions()
            .color(Color.GRAY)
            .width(10f)
            .pattern(listOf(Dash(20f), Gap(10f)))

        for (punto in listaPuntos) {
            polylineOptions.add(LatLng(punto.latitud, punto.longitud))
        }
        plannedRoutePolyline = mapNavegacion.addPolyline(polylineOptions)

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
            setMinUpdateDistanceMeters(3f)
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
        if (location.hasAccuracy() && location.accuracy > 20f) return

        val posicionActual = LatLng(location.latitude, location.longitude)
        mapNavegacion.animateCamera(CameraUpdateFactory.newLatLngZoom(posicionActual, 18f))

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

        if (recorridoId.isNotEmpty()) {
            db.child("recorridos").child(recorridoId).updateChildren(mapOf(
                "latitudActual" to location.latitude,
                "longitudActual" to location.longitude
            ))
        }

        val velocidadKmH = (location.speed * 3.6).toInt()
        tvActiveSpeed.text = velocidadKmH.toString()

        if (indicePuntoActual >= listaPuntos.size) return

        val puntoEsperado = listaPuntos[indicePuntoActual]
        val resultados = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, puntoEsperado.latitud, puntoEsperado.longitud, resultados)
        val distanciaMetros = resultados[0]

        actualizarTarjetaProximaParada(distanciaMetros)

        if (distanciaMetros <= rutaRadio) {
            registrarLlegadaEnFirebase(puntoEsperado)
            Toast.makeText(this, "¡Llegaste a ${puntoEsperado.nombre}!", Toast.LENGTH_SHORT).show()
            indicePuntoActual++

            if (indicePuntoActual < listaPuntos.size) {
                actualizarTarjetaProximaParada(null)
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

            // Escuchar pasajeros esperando en esta parada
            escucharPasajerosEnParada(siguientePunto.id)

            if (distanciaAproximada != null) {
                tvActiveDistance.text = "${distanciaAproximada.toInt()}m"
                val segundos = distanciaAproximada / 8.3f
                val minutos = (segundos / 60).toInt()
                tvActiveTime.text = if (minutos < 1) "aprox. 1 min" else "aprox. $minutos min"
            } else {
                tvActiveDistance.text = "--"
                tvActiveTime.text = "Calculando..."
            }
        }
    }

    private fun escucharPasajerosEnParada(puntoId: String) {
        if (puntoId == currentParaderoId || recorridoId.isEmpty()) return

        // Quitar listener anterior
        paraderoListener?.let { paraderoListenerRef?.removeEventListener(it) }

        currentParaderoId = puntoId
        paraderoListenerRef = db.child("recorridos").child(recorridoId).child("paraderos").child(puntoId)

        paraderoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.child("pasajerosEsperando").getValue(Int::class.java) ?: 0
                actualizarChipPasajeros(count)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        paraderoListenerRef!!.addValueEventListener(paraderoListener!!)
    }

    private fun actualizarChipPasajeros(count: Int) {
        if (count > 0) {
            layoutPasajerosEsperando.visibility = View.VISIBLE
            tvPasajerosEsperando.text = if (count == 1) "1 persona esperando" else "$count personas esperando"
        } else {
            layoutPasajerosEsperando.visibility = View.GONE
        }
    }

    private fun registrarLlegadaEnFirebase(punto: PuntoRuta) {
        if (recorridoId.isEmpty()) return

        val ahora = System.currentTimeMillis()
        val tiempoDesdeAnterior = ahora - tiempoUltimoPunto

        val puntoRegistrado = mapOf(
            "puntoId" to punto.id,
            "nombre" to punto.nombre,
            "tiempoLlegada" to ahora,
            "tiempoDesdeAnteriorMs" to tiempoDesdeAnterior
        )

        db.child("recorridos").child(recorridoId).child("puntosRegistrados").child(punto.id).setValue(puntoRegistrado)
        tiempoUltimoPunto = ahora
    }

    private fun solicitarClaveFinalizacion() {
        val editText = EditText(this).apply { hint = "Ingrese la clave" }
        AlertDialog.Builder(this)
            .setTitle("Finalizar Ruta")
            .setMessage("Digite la clave maestra")
            .setView(editText)
            .setPositiveButton("Validar") { _, _ ->
                if (editText.text.toString().trim() == "1234") {
                    finalizarRecorrido(esAutomatico = false)
                } else {
                    Toast.makeText(this, "Clave incorrecta", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarAlertaSeguridad() {
        AlertDialog.Builder(this)
            .setTitle("ATENCION - SEGURIDAD PRIMERO")
            .setMessage("¿Desea salir al Hub principal?\n\nLa ruta seguirá activa y el GPS continuará compartiendo su posición con los pasajeros.\n\nRecuerde mantener las MANOS AL VOLANTE.")
            .setPositiveButton("SÍ, SALIR AL HUB") { _, _ -> salirSeguridad() }
            .setNegativeButton("VOLVER A LA RUTA", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun salirSeguridad() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        // El servicio en segundo plano CONTINÚA actualizando la posición
        Toast.makeText(this, "GPS activo en segundo plano - los pasajeros siguen viendo su posición", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun finalizarRecorrido(esAutomatico: Boolean) {
        routeFinished = true
        fusedLocationClient.removeLocationUpdates(locationCallback)
        detenerServicioSegundoPlano()

        if (recorridoId.isNotEmpty()) {
            val estadoFinal = if (esAutomatico) "finalizado_automatico" else "finalizado_manual"
            val actualizacion = mapOf(
                "finTiempo" to System.currentTimeMillis(),
                "estado" to estadoFinal
            )
            db.child("recorridos").child(recorridoId).updateChildren(actualizacion).addOnSuccessListener {
                Toast.makeText(this, "Recorrido finalizado", Toast.LENGTH_SHORT).show()
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
        paraderoListener?.let { paraderoListenerRef?.removeEventListener(it) }
    }
}
