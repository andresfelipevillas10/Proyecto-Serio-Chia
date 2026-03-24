package com.example.proyecto_definitivo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.FirebaseDatabase



class NavegacionActivaActivity : AppCompatActivity(), OnMapReadyCallback {

    // Vistas de la interfaz moderna
    private lateinit var mapNavegacion: GoogleMap
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
        recorridoId = intent.getStringExtra("recorridoId") ?: "" // Recibimos el ID del registro en BD

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

        // 6. Cargar los puntos para saber a dónde vamos
        cargarPuntosRuta()

        tiempoUltimoPunto = System.currentTimeMillis() // Inicia el reloj
    }

    override fun onMapReady(map: GoogleMap) {
        mapNavegacion = map

        // Configuraciones de conducción
        mapNavegacion.uiSettings.isZoomControlsEnabled = false
        mapNavegacion.uiSettings.isCompassEnabled = true
        mapNavegacion.setPadding(0, 300, 0, 0) // Bajamos el centro del mapa para que no lo tape la tarjeta superior

        habilitarCapaUbicacion()
    }

    private fun cargarPuntosRuta() {
        if (rutaId.isEmpty()) return

        db.child("rutas").child(rutaId).child("puntos").get().addOnSuccessListener { snapshot ->
            listaPuntos.clear()
            for (puntoSnap in snapshot.children) {
                val punto = puntoSnap.getValue(PuntoRuta::class.java)
                punto?.let { listaPuntos.add(it) }
            }
            listaPuntos.sortBy { it.orden }

            dibujarPuntos()
            actualizarTarjetaProximaParada(null) // Inicializa la tarjeta con el primer punto
        }
    }

    private fun dibujarPuntos() {
        mapNavegacion.clear()
        for (punto in listaPuntos) {
            val posicion = LatLng(punto.latitud, punto.longitud)
            val color = if (punto.Tipo == "origen") BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_AZURE
            mapNavegacion.addMarker(MarkerOptions().position(posicion).title(punto.nombre).icon(BitmapDescriptorFactory.defaultMarker(color)))
        }
    }

    private fun configurarMotorUbicacion() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).apply {
            setMinUpdateIntervalMillis(2000L)
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
        // 1. Centrar cámara en el conductor
        val posicionActual = LatLng(location.latitude, location.longitude)
        mapNavegacion.animateCamera(CameraUpdateFactory.newLatLngZoom(posicionActual, 18f))

        // 2. Actualizar Velocímetro
        val velocidadKmH = (location.speed * 3.6).toInt() // Convertir m/s a km/h
        tvActiveSpeed.text = velocidadKmH.toString()

        // 3. Lógica de Paradas
        if (indicePuntoActual >= listaPuntos.size) return

        val puntoEsperado = listaPuntos[indicePuntoActual]
        val resultados = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, puntoEsperado.latitud, puntoEsperado.longitud, resultados)
        val distanciaMetros = resultados[0]

        // Actualizar UI con la distancia restante
        actualizarTarjetaProximaParada(distanciaMetros)

        // 4. ¿Llegamos al punto?
        if (distanciaMetros <= rutaRadio) {
            registrarLlegadaEnFirebase(puntoEsperado)
            indicePuntoActual++

            if (indicePuntoActual < listaPuntos.size) {
                Toast.makeText(this, "¡Llegaste a ${puntoEsperado.nombre}!", Toast.LENGTH_SHORT).show()
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
            }
        }
    }

    private fun registrarLlegadaEnFirebase(punto: PuntoRuta) {
        if (recorridoId.isEmpty()) return

        val ahora = System.currentTimeMillis()
        val tiempoDesdeAnterior = ahora - tiempoUltimoPunto

        // Guardamos el registro del punto (Usando la misma lógica que tenías)
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
        fusedLocationClient.removeLocationUpdates(locationCallback) // Apagar GPS

        if (recorridoId.isNotEmpty()) {
            val estadoFinal = if (esAutomatico) "finalizado_automatico" else "finalizado_manual"
            val actualizacion = mapOf(
                "finTiempo" to System.currentTimeMillis(),
                "estado" to estadoFinal
            )
            db.child("recorridos").child(recorridoId).updateChildren(actualizacion).addOnSuccessListener {

                // ¡EL CAMBIO ESTÁ AQUÍ!
                Toast.makeText(this, "Recorrido finalizado", Toast.LENGTH_SHORT).show()

                // Lanzamos la pantalla de Resumen Final pasándole el ID del recorrido
                val intent = Intent(this, ResumenRecorridoActivity::class.java)
                intent.putExtra("recorridoId", recorridoId)
                startActivity(intent)

                finish() // Ahora sí cerramos la navegación
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