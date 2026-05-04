package com.example.proyecto_definitivo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*

class SeguimientoBusActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val db = FirebaseDatabase.getInstance().reference

    private lateinit var tvRutaNombre: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnComoLlegar: MaterialButton

    private var recorridoId = ""
    private var rutaId = ""
    private var conductorId = ""
    private var rutaNombre = ""

    private val listaPuntos = mutableListOf<PuntoRuta>()
    private var busMarker: Marker? = null
    private var indicePuntoActual = 0

    private var recorridoListener: ValueEventListener? = null
    private var nearestStopLat = 0.0
    private var nearestStopLng = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seguimiento_bus)

        recorridoId = intent.getStringExtra("recorridoId") ?: ""
        rutaId = intent.getStringExtra("rutaId") ?: ""
        conductorId = intent.getStringExtra("conductorId") ?: ""
        rutaNombre = intent.getStringExtra("rutaNombre") ?: "Ruta"

        bindViews()
        tvRutaNombre.text = rutaNombre

        btnBack.setOnClickListener { finish() }
        btnComoLlegar.setOnClickListener { abrirNavegacion() }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapSeguimiento) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun bindViews() {
        tvRutaNombre = findViewById(R.id.tvSeguimientoRutaNombre)
        btnBack = findViewById(R.id.btnBackSeguimiento)
        btnComoLlegar = findViewById(R.id.btnComoLlegar)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = false
        googleMap.uiSettings.isCompassEnabled = true

        cargarPuntosRuta()
        escucharUbicacionBus()
    }

    private fun cargarPuntosRuta() {
        if (rutaId.isEmpty() || conductorId.isEmpty()) return

        db.child("rutas").child(conductorId).child(rutaId).child("puntos")
            .get()
            .addOnSuccessListener { snapshot ->
                listaPuntos.clear()
                for (puntoSnap in snapshot.children) {
                    val punto = puntoSnap.getValue(PuntoRuta::class.java)
                    punto?.let { listaPuntos.add(it) }
                }
                listaPuntos.sortBy { it.orden }

                dibujarRutaEnMapa()
                actualizarPanelParadas()

                if (listaPuntos.isNotEmpty()) {
                    nearestStopLat = listaPuntos[0].latitud
                    nearestStopLng = listaPuntos[0].longitud
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar la ruta", Toast.LENGTH_SHORT).show()
            }
    }

    private fun dibujarRutaEnMapa() {
        if (listaPuntos.isEmpty()) return

        val polylineOptions = PolylineOptions()
            .color(android.graphics.Color.parseColor("#1A6B3A"))
            .width(10f)

        val builder = LatLngBounds.Builder()

        for (punto in listaPuntos) {
            val pos = LatLng(punto.latitud, punto.longitud)
            polylineOptions.add(pos)
            builder.include(pos)

            val color = when (punto.Tipo) {
                "origen" -> BitmapDescriptorFactory.HUE_GREEN
                "fin" -> BitmapDescriptorFactory.HUE_RED
                else -> BitmapDescriptorFactory.HUE_AZURE
            }
            googleMap.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(punto.nombre)
                    .icon(BitmapDescriptorFactory.defaultMarker(color))
                    .alpha(0.85f)
            )
        }

        googleMap.addPolyline(polylineOptions)

        try {
            val bounds = builder.build()
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        } catch (e: Exception) {
            val first = LatLng(listaPuntos[0].latitud, listaPuntos[0].longitud)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(first, 14f))
        }
    }

    private fun escucharUbicacionBus() {
        if (recorridoId.isEmpty()) return

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitudActual").getValue(Double::class.java) ?: return
                val lng = snapshot.child("longitudActual").getValue(Double::class.java) ?: return

                if (lat == 0.0 && lng == 0.0) return

                val posicion = LatLng(lat, lng)

                if (busMarker == null) {
                    busMarker = googleMap.addMarker(
                        MarkerOptions()
                            .position(posicion)
                            .title("Bus")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                            .zIndex(1f)
                    )
                } else {
                    busMarker?.position = posicion
                }

                val puntosCompletados = snapshot.child("puntosRegistrados").childrenCount.toInt()
                indicePuntoActual = puntosCompletados.coerceAtMost((listaPuntos.size - 1).coerceAtLeast(0))
                actualizarPanelParadas()

                if (indicePuntoActual < listaPuntos.size) {
                    nearestStopLat = listaPuntos[indicePuntoActual].latitud
                    nearestStopLng = listaPuntos[indicePuntoActual].longitud
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        db.child("recorridos").child(conductorId).child(recorridoId).addValueEventListener(listener)
        recorridoListener = listener
    }

    private fun actualizarPanelParadas() {
        if (listaPuntos.isEmpty()) return
        // UI removed in current layout
    }

    private fun abrirNavegacion() {
        if (nearestStopLat == 0.0 && nearestStopLng == 0.0) {
            Toast.makeText(this, getString(R.string.loading_route_info), Toast.LENGTH_SHORT).show()
            return
        }

        val uri = Uri.parse("google.navigation:q=$nearestStopLat,$nearestStopLng&mode=w")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }

        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            val browserUri = Uri.parse("https://maps.google.com/?daddr=$nearestStopLat,$nearestStopLng&dirflg=w")
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recorridoListener?.let {
            db.child("recorridos").child(conductorId).child(recorridoId).removeEventListener(it)
        }
    }
}
