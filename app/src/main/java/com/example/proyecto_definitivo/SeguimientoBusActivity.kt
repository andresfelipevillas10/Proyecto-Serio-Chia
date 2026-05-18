package com.example.proyecto_definitivo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SeguimientoBusActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val db = FirebaseDatabase.getInstance().reference

    private lateinit var tvRutaNombre: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnComoLlegar: MaterialButton

    // Asientos y estado pasajero
    private lateinit var tvAsientosInfo: TextView
    private lateinit var btnVerAsientos: MaterialButton
    private lateinit var btnEsperandoParadero: MaterialButton
    private lateinit var layoutEstadoEsperando: LinearLayout
    private lateinit var tvParaderoEsperando: TextView
    private lateinit var btnYaSubiAlBus: MaterialButton
    private lateinit var btnCancelarEspera: MaterialButton
    private lateinit var btnReportarBusLleno: MaterialButton

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

    // Estado del pasajero en esta sesión
    private enum class EstadoPasajero { LIBRE, ESPERANDO, A_BORDO }
    private var estadoPasajero = EstadoPasajero.LIBRE
    private var paraderoEsperandoId = ""
    private var paraderoEsperandoNombre = ""
    private var capacidadBus = 30
    private var asientosOcupados = 0

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
        btnVerAsientos.setOnClickListener { mostrarDiagramaAsientos() }
        btnYaSubiAlBus.setOnClickListener { marcarSubidoAlBus() }
        btnCancelarEspera.setOnClickListener { cancelarEspera() }
        btnReportarBusLleno.setOnClickListener { confirmarReporteBusLleno() }

        actualizarEstadoUI()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapSeguimiento) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun bindViews() {
        tvRutaNombre = findViewById(R.id.tvSeguimientoRutaNombre)
        btnBack = findViewById(R.id.btnBackSeguimiento)
        btnComoLlegar = findViewById(R.id.btnComoLlegar)
        tvAsientosInfo = findViewById(R.id.tvAsientosInfo)
        btnVerAsientos = findViewById(R.id.btnVerAsientos)
        btnEsperandoParadero = findViewById(R.id.btnEsperandoParadero)
        layoutEstadoEsperando = findViewById(R.id.layoutEstadoEsperando)
        tvParaderoEsperando = findViewById(R.id.tvParaderoEsperando)
        btnYaSubiAlBus = findViewById(R.id.btnYaSubiAlBus)
        btnCancelarEspera = findViewById(R.id.btnCancelarEspera)
        btnReportarBusLleno = findViewById(R.id.btnReportarBusLleno)
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

                // Leer capacidad del bus en tiempo real
                capacidadBus = snapshot.child("capacidadTotal").getValue(Int::class.java) ?: 30
                asientosOcupados = snapshot.child("asientosOcupados").getValue(Int::class.java) ?: 0
                actualizarInfoAsientos()
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

    // ── Asientos ──────────────────────────────────────────────────────────────────

    private fun actualizarInfoAsientos() {
        val disponibles = capacidadBus - asientosOcupados
        tvAsientosInfo.text = "$disponibles de $capacidadBus puestos disponibles"
    }

    private fun mostrarDiagramaAsientos() {
        val dp = resources.displayMetrics.density
        val disponibles = capacidadBus - asientosOcupados

        val scrollView = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * dp).toInt(), (20 * dp).toInt(), (20 * dp).toInt(), (20 * dp).toInt())
        }

        // Título
        container.addView(TextView(this).apply {
            text = "Asientos del bus"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF1C1C1A.toInt())
        })

        // Disponibilidad
        container.addView(TextView(this).apply {
            text = "$disponibles de $capacidadBus puestos disponibles"
            textSize = 14f
            setTextColor(if (disponibles > 5) 0xFF1A6B3A.toInt() else 0xFFBA1A1A.toInt())
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            p.topMargin = (4 * dp).toInt()
            p.bottomMargin = (12 * dp).toInt()
            layoutParams = p
        })

        // Leyenda
        val legend = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            p.bottomMargin = (12 * dp).toInt()
            layoutParams = p
        }
        legend.addView(View(this).apply {
            setBackgroundResource(R.drawable.bg_seat_free)
            layoutParams = LinearLayout.LayoutParams((16 * dp).toInt(), (16 * dp).toInt())
        })
        legend.addView(TextView(this).apply { text = "  Libre   "; textSize = 12f; setTextColor(0xFF6B6B66.toInt()) })
        legend.addView(View(this).apply {
            setBackgroundResource(R.drawable.bg_seat_occupied)
            layoutParams = LinearLayout.LayoutParams((16 * dp).toInt(), (16 * dp).toInt())
        })
        legend.addView(TextView(this).apply { text = "  Ocupado"; textSize = 12f; setTextColor(0xFF6B6B66.toInt()) })
        container.addView(legend)

        // Divisor
        container.addView(View(this).apply {
            setBackgroundColor(0xFFD4D4CC.toInt())
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt())
            p.bottomMargin = (12 * dp).toInt()
            layoutParams = p
        })

        // Fila del conductor
        container.addView(TextView(this).apply {
            text = "[ CONDUCTOR ]"
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF6B6B66.toInt())
            setBackgroundColor(0xFFEBEBE5.toInt())
            setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (8 * dp).toInt())
            gravity = android.view.Gravity.CENTER
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            p.bottomMargin = (8 * dp).toInt()
            layoutParams = p
        })

        // Filas de asientos (2 a cada lado del pasillo)
        val seatSize = (42 * dp).toInt()
        val seatMargin = (3 * dp).toInt()
        val aisleGap = (14 * dp).toInt()
        var seatNumber = 1

        while (seatNumber <= capacidadBus) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER
                val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                p.bottomMargin = seatMargin
                layoutParams = p
            }
            // Lado izquierdo: 2 asientos
            for (i in 0..1) {
                if (seatNumber <= capacidadBus) {
                    row.addView(crearVistaAsiento(seatNumber, seatNumber <= asientosOcupados, seatSize, seatMargin))
                    seatNumber++
                }
            }
            // Pasillo
            row.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(aisleGap, seatSize)
            })
            // Lado derecho: 2 asientos
            for (i in 0..1) {
                if (seatNumber <= capacidadBus) {
                    row.addView(crearVistaAsiento(seatNumber, seatNumber <= asientosOcupados, seatSize, seatMargin))
                    seatNumber++
                }
            }
            container.addView(row)
        }

        scrollView.addView(container)

        AlertDialog.Builder(this)
            .setView(scrollView)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun crearVistaAsiento(numero: Int, ocupado: Boolean, size: Int, margin: Int): TextView {
        return TextView(this).apply {
            text = numero.toString()
            textSize = 11f
            gravity = android.view.Gravity.CENTER
            setTextColor(if (ocupado) 0xFFFFFFFF.toInt() else 0xFF1A6B3A.toInt())
            setBackgroundResource(if (ocupado) R.drawable.bg_seat_occupied else R.drawable.bg_seat_free)
            val p = LinearLayout.LayoutParams(size, size)
            p.setMargins(margin, 0, margin, 0)
            layoutParams = p
        }
    }

    // ── Esperando / Abordo ────────────────────────────────────────────────────────

    private fun mostrarDialogoSeleccionParadero() {
        if (listaPuntos.isEmpty()) {
            Toast.makeText(this, "Cargando paradas, intente de nuevo", Toast.LENGTH_SHORT).show()
            return
        }
        val proximasParadas = listaPuntos.drop(indicePuntoActual).take(6)
        val nombres = proximasParadas.map { it.nombre }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("¿En qué paradero estás esperando?")
            .setItems(nombres) { _, which ->
                val paradero = proximasParadas[which]
                marcarEsperandoEnParadero(paradero.id, paradero.nombre)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun marcarEsperandoEnParadero(puntoId: String, nombre: String) {
        paraderoEsperandoId = puntoId
        paraderoEsperandoNombre = nombre
        estadoPasajero = EstadoPasajero.ESPERANDO
        incrementarEsperando(puntoId)
        actualizarEstadoUI()
    }

    private fun marcarSubidoAlBus() {
        if (paraderoEsperandoId.isNotEmpty()) {
            decrementarEsperando(paraderoEsperandoId)
        }
        incrementarOcupados()
        estadoPasajero = EstadoPasajero.A_BORDO
        paraderoEsperandoId = ""
        paraderoEsperandoNombre = ""
        actualizarEstadoUI()
        Toast.makeText(this, "¡Bienvenido a bordo! Buen viaje.", Toast.LENGTH_SHORT).show()
    }

    private fun cancelarEspera() {
        if (paraderoEsperandoId.isNotEmpty()) {
            decrementarEsperando(paraderoEsperandoId)
        }
        estadoPasajero = EstadoPasajero.LIBRE
        paraderoEsperandoId = ""
        paraderoEsperandoNombre = ""
        actualizarEstadoUI()
    }

    private fun bajarseDeBus() {
        decrementarOcupados()
        estadoPasajero = EstadoPasajero.LIBRE
        actualizarEstadoUI()
    }

    private fun actualizarEstadoUI() {
        when (estadoPasajero) {
            EstadoPasajero.LIBRE -> {
                btnEsperandoParadero.visibility = View.VISIBLE
                btnEsperandoParadero.text = "Estoy esperando en un paradero"
                btnEsperandoParadero.setOnClickListener { mostrarDialogoSeleccionParadero() }
                layoutEstadoEsperando.visibility = View.GONE
            }
            EstadoPasajero.ESPERANDO -> {
                btnEsperandoParadero.visibility = View.GONE
                layoutEstadoEsperando.visibility = View.VISIBLE
                tvParaderoEsperando.text = "Esperando en: $paraderoEsperandoNombre"
            }
            EstadoPasajero.A_BORDO -> {
                btnEsperandoParadero.visibility = View.VISIBLE
                btnEsperandoParadero.text = "Bajarme del bus"
                btnEsperandoParadero.setOnClickListener { bajarseDeBus() }
                layoutEstadoEsperando.visibility = View.GONE
            }
        }
    }

    // ── Reportar bus lleno ────────────────────────────────────────────────────────

    private fun confirmarReporteBusLleno() {
        AlertDialog.Builder(this)
            .setTitle("Reportar bus lleno")
            .setMessage("¿Deseas reportar que el bus viene demasiado lleno? Esto ayuda a coordinar el envío de otro vehículo.")
            .setPositiveButton("Sí, reportar") { _, _ -> reportarBusLleno() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun reportarBusLleno() {
        if (recorridoId.isEmpty()) return
        db.child("recorridos").child(recorridoId).child("reporteFullBus").setValue(true)
        Toast.makeText(this, "Reporte enviado. Gracias por informar.", Toast.LENGTH_LONG).show()
        btnReportarBusLleno.isEnabled = false
        btnReportarBusLleno.text = "Reporte ya enviado"
    }

    // ── Firebase Transactions ─────────────────────────────────────────────────────

    private fun incrementarEsperando(puntoId: String) {
        db.child("recorridos").child(recorridoId).child("paraderos").child(puntoId)
            .child("pasajerosEsperando")
            .runTransaction(object : Transaction.Handler {
                override fun doTransaction(d: MutableData): Transaction.Result {
                    d.value = (d.getValue(Int::class.java) ?: 0) + 1
                    return Transaction.success(d)
                }
                override fun onComplete(e: DatabaseError?, c: Boolean, s: DataSnapshot?) {}
            })
    }

    private fun decrementarEsperando(puntoId: String) {
        db.child("recorridos").child(recorridoId).child("paraderos").child(puntoId)
            .child("pasajerosEsperando")
            .runTransaction(object : Transaction.Handler {
                override fun doTransaction(d: MutableData): Transaction.Result {
                    d.value = maxOf(0, (d.getValue(Int::class.java) ?: 0) - 1)
                    return Transaction.success(d)
                }
                override fun onComplete(e: DatabaseError?, c: Boolean, s: DataSnapshot?) {}
            })
    }

    private fun incrementarOcupados() {
        db.child("recorridos").child(recorridoId).child("asientosOcupados")
            .runTransaction(object : Transaction.Handler {
                override fun doTransaction(d: MutableData): Transaction.Result {
                    d.value = (d.getValue(Int::class.java) ?: 0) + 1
                    return Transaction.success(d)
                }
                override fun onComplete(e: DatabaseError?, c: Boolean, s: DataSnapshot?) {}
            })
    }

    private fun decrementarOcupados() {
        db.child("recorridos").child(recorridoId).child("asientosOcupados")
            .runTransaction(object : Transaction.Handler {
                override fun doTransaction(d: MutableData): Transaction.Result {
                    d.value = maxOf(0, (d.getValue(Int::class.java) ?: 0) - 1)
                    return Transaction.success(d)
                }
                override fun onComplete(e: DatabaseError?, c: Boolean, s: DataSnapshot?) {}
            })
    }

    // ─────────────────────────────────────────────────────────────────────────────

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
        // Si el pasajero cerraba la app estando en espera, limpiamos su registro
        if (estadoPasajero == EstadoPasajero.ESPERANDO && paraderoEsperandoId.isNotEmpty()) {
            decrementarEsperando(paraderoEsperandoId)
        }
        recorridoListener?.let {
            db.child("recorridos").child(conductorId).child(recorridoId).removeEventListener(it)
        }
    }
}
