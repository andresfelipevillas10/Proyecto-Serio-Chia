package com.example.proyecto_definitivo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PreRecorridoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val db = FirebaseDatabase.getInstance().reference

    // UI Elements
    private lateinit var tvPreRouteName: TextView
    private lateinit var tvPreRouteStops: TextView
    private lateinit var tvPreRouteRadius: TextView
    private lateinit var btnStartDrivingMode: MaterialButton
    private lateinit var btnBackPreRecorrido: ImageButton

    // Datos de la ruta
    private var rutaId: String = ""
    private var rutaNombre: String = ""
    private val listaPuntos = mutableListOf<PuntoRuta>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pre_recorrido)

        // Enlazar vistas
        tvPreRouteName = findViewById(R.id.tvPreRouteName)
        tvPreRouteStops = findViewById(R.id.tvPreRouteStops)
        tvPreRouteRadius = findViewById(R.id.tvPreRouteRadius)
        btnStartDrivingMode = findViewById(R.id.btnStartDrivingMode)
        btnBackPreRecorrido = findViewById(R.id.btnBackPreRecorrido)

        // Obtener los datos del Intent
        rutaId = intent.getStringExtra("rutaId") ?: ""
        rutaNombre = intent.getStringExtra("rutaNombre") ?: "Ruta Desconocida"
        val rutaRadio = intent.getFloatExtra("rutaRadio", 30f)

        // Llenar datos básicos en la tarjeta inferior
        tvPreRouteName.text = rutaNombre
        tvPreRouteRadius.text = "${rutaRadio.toInt()} m" // Ej: "30 m"

        // Botón Atrás
        btnBackPreRecorrido.setOnClickListener {
            finish()
        }

        // Iniciar el Mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapPreRecorrido) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // ¡EL GRAN BOTÓN INICIAR!
        btnStartDrivingMode.setOnClickListener {
            if (listaPuntos.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_points_configured), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. Preparamos el terreno en la Base de Datos
            val recorridosRef = db.child("recorridos").push()
            val nuevoRecorridoId = recorridosRef.key ?: ""

            // Obtenemos el ID del conductor actual (Si aún no tienes el Auth listo, le ponemos un dummy temporal)
            val conductorId = FirebaseAuth.getInstance().currentUser?.uid ?: "conductor_demo_123"

            // 2. Creamos el objeto con tu nueva clase
            val recorridoDeHoy = Recorrido(
                id = nuevoRecorridoId,
                rutaId = rutaId,
                rutaNombre = rutaNombre,
                usuarioId = conductorId,
                inicioTiempo = System.currentTimeMillis(), // Guarda la hora exacta en milisegundos
                estado = "en_proceso"
            )

            // 3. Lo guardamos en Firebase y SI ES EXITOSO, guardamos estado activo y abramos la Navegación
            recorridosRef.setValue(recorridoDeHoy).addOnSuccessListener {
                val rutaActualData = mapOf(
                    "recorridoId" to nuevoRecorridoId,
                    "rutaId" to rutaId,
                    "nombre" to rutaNombre,
                    "puntos_completados" to 0,
                    "total_puntos" to listaPuntos.size,
                    "radioDeteccion" to rutaRadio,
                    "indice_actual" to 0
                )
                db.child("users").child(conductorId).child("ruta_actual").setValue(rutaActualData)

                Toast.makeText(this, getString(R.string.route_started), Toast.LENGTH_SHORT).show()

                val intent = Intent(this, NavegacionActivaActivity::class.java).apply {
                    putExtra("rutaId", rutaId)
                    putExtra("rutaNombre", rutaNombre)
                    putExtra("rutaRadio", rutaRadio)
                    putExtra("recorridoId", nuevoRecorridoId)
                    putExtra("indice_actual", 0)
                }
                startActivity(intent)
                finish()

            }.addOnFailureListener {
                Toast.makeText(this, getString(R.string.network_error_start), Toast.LENGTH_SHORT).show()
            }
        }
    } // <-- Aquí termina correctamente el onCreate

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Quitar botones de zoom y compás para que el mapa se vea más limpio como "display"
        googleMap.uiSettings.isZoomControlsEnabled = false
        googleMap.uiSettings.isCompassEnabled = false

        // Cargar los puntos de esta ruta desde Firebase
        cargarPuntosDesdeFirebase()
    }

    private fun cargarPuntosDesdeFirebase() {
        if (rutaId.isEmpty()) return
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        db.child("rutas").child(currentUserId).child(rutaId).child("puntos")
            .get()
            .addOnSuccessListener { snapshot ->
                listaPuntos.clear()

                for (puntoSnap in snapshot.children) {
                    val punto = puntoSnap.getValue(PuntoRuta::class.java)
                    punto?.let { listaPuntos.add(it) }
                }

                // Ordenar para dibujar la línea correctamente
                listaPuntos.sortBy { it.orden }

                // Actualizar la estadística de paradas en el panel inferior
                tvPreRouteStops.text = listaPuntos.size.toString()

                // Dibujar en el mapa
                dibujarRutaEnMapa()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar los puntos de la ruta", Toast.LENGTH_SHORT).show()
            }
    }

    private fun dibujarRutaEnMapa() {
        if (listaPuntos.isEmpty()) return

        val polylineOptions = PolylineOptions().color(Color.parseColor("#006c49")).width(10f)
        val builder = LatLngBounds.Builder() // Esto nos ayuda a centrar la cámara

        for (punto in listaPuntos) {
            val posicion = LatLng(punto.latitud, punto.longitud)
            polylineOptions.add(posicion)
            builder.include(posicion) // Agregamos el punto a los límites de la cámara

            // Elegir color según el tipo (Origen verde, Fin rojo, Marca azul)
            val colorMarcador = when (punto.Tipo) {
                "origen" -> BitmapDescriptorFactory.HUE_GREEN
                "fin" -> BitmapDescriptorFactory.HUE_RED
                else -> BitmapDescriptorFactory.HUE_AZURE
            }

            // Añadir el pin al mapa
            googleMap.addMarker(
                MarkerOptions()
                    .position(posicion)
                    .title("${punto.orden}. ${punto.nombre}")
                    .icon(BitmapDescriptorFactory.defaultMarker(colorMarcador))
            )
        }

        // Trazar la línea
        googleMap.addPolyline(polylineOptions)

        // Hacer zoom mágico para que toda la ruta encaje perfectamente en la pantalla
        try {
            val bounds = builder.build()
            // El "100" es el padding (espacio en los bordes) para que no quede pegado a la pantalla
            val cu = CameraUpdateFactory.newLatLngBounds(bounds, 100)
            googleMap.animateCamera(cu)
        } catch (e: Exception) {
            // Por si solo hay 1 punto o falló el cálculo de los límites
            val primerPunto = LatLng(listaPuntos[0].latitud, listaPuntos[0].longitud)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(primerPunto, 15f))
        }
    }
}
