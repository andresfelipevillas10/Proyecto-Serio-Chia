package com.example.proyecto_definitivo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ConfigurarPuntosRuta : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    // Nuevas vistas de tu XML Paso 2
    private lateinit var btnRecargarPuntos: MaterialButton
    private lateinit var btnBackPaso2: ImageButton
    private lateinit var btnGuardarConfiguracion: MaterialButton
    private lateinit var tvContadorPuntos: TextView

    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private var rutaId: String = ""
    private var rutaNombre: String = ""
    private var rutaRadio: Float = 30f

    private val listaPuntos = mutableListOf<PuntoRuta>()
    private lateinit var puntoAdapter: PuntoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. APUNTAMOS AL NUEVO DISEÑO
        setContentView(R.layout.activity_crear_ruta_paso2)

        // 2. ENLAZAMOS LOS NUEVOS IDs
        btnRecargarPuntos = findViewById(R.id.btnRecargarPuntos)
        btnBackPaso2 = findViewById(R.id.btnBackPaso2)
        btnGuardarConfiguracion = findViewById(R.id.btnGuardarConfiguracion)
        tvContadorPuntos = findViewById(R.id.tvContadorPuntos)

        rutaId = intent.getStringExtra("rutaId") ?: ""
        rutaNombre = intent.getStringExtra("rutaNombre") ?: ""
        rutaRadio = intent.getFloatExtra("rutaRadio", 30f)

        // Botones de navegación
        btnBackPaso2.setOnClickListener { finish() }
        btnGuardarConfiguracion.setOnClickListener {
            Toast.makeText(this, getString(R.string.route_configured_success), Toast.LENGTH_SHORT).show()
            finish() // Cierra y vuelve al menú
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapConfigurarPuntos) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnRecargarPuntos.setOnClickListener { cargarPuntosRuta() }

        val recyclerPuntos = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerPuntosConfiguracion)
        puntoAdapter = PuntoAdapter(
            listaPuntos,
            onModificarClick = { punto -> mostrarDialogoAgregarPunto(LatLng(punto.latitud, punto.longitud), punto) },
            onEliminarClick = { punto -> eliminarPuntoFirebase(punto) }
        )
        recyclerPuntos.adapter = puntoAdapter
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = true

        // Esto empuja los controles de Google (botón de ubicación, logo de Google)
// hacia adentro para que no se solapen con tu UI.
        googleMap.setPadding(0, 150, 0, 0) // (izquierda, arriba, derecha, abajo) en píxeles

        habilitarMiUbicacion()
        googleMap.uiSettings.isMyLocationButtonEnabled = true

        val ubicacionInicial = LatLng(4.8615, -74.0510) // Centro de Chía
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionInicial, 14f))

        googleMap.setOnMapClickListener { latLng ->
            mostrarDialogoAgregarPunto(latLng)
        }

        cargarPuntosRuta()
    }

    private fun habilitarMiUbicacion() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // SI TENEMOS PERMISO: Se activa el punto azul Y el botón
            googleMap.isMyLocationEnabled = true
        } else {
            // SI NO TENEMOS PERMISO: Hay que pedirlo
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1000
            )
        }
    }
    // --- LA MAGIA NUEVA DEL DIÁLOGO ---
    private fun mostrarDialogoAgregarPunto(latLng: LatLng, puntoExistente: PuntoRuta? = null) {
        // Inflamos tu nuevo diseño con bordes redondeados
        val vista = layoutInflater.inflate(R.layout.dialog_nuevo_punto, null)

        val etNombre = vista.findViewById<EditText>(R.id.etNombrePunto)
        val etOrden = vista.findViewById<EditText>(R.id.etOrdenPunto)
        val etLat = vista.findViewById<EditText>(R.id.etLatitudPunto)
        val etLng = vista.findViewById<EditText>(R.id.etLongitudPunto)
        val spTipo = vista.findViewById<Spinner>(R.id.spTipoPunto)

        // Botones del nuevo diálogo
        val btnCerrar = vista.findViewById<ImageButton>(R.id.btnCerrarDialog)
        val btnGuardar = vista.findViewById<MaterialButton>(R.id.btnGuardarPunto)
        val btnActualizarGps = vista.findViewById<View>(R.id.btnActualizarUbicacion) // El LinearLayout

        val tipos = listOf("origen", "marca", "fin")
        spTipo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tipos)

        val esEdicion = puntoExistente != null
        etLat.isEnabled = esEdicion
        etLng.isEnabled = esEdicion

        etNombre.setText(puntoExistente?.nombre ?: "")
        etOrden.setText(puntoExistente?.orden?.toString() ?: "")
        etLat.setText(puntoExistente?.latitud?.toString() ?: latLng.latitude.toString())
        etLng.setText(puntoExistente?.longitud?.toString() ?: latLng.longitude.toString())
        puntoExistente?.let { spTipo.setSelection(tipos.indexOf(it.Tipo)) }

        // Creamos el diálogo
        val dialog = AlertDialog.Builder(this)
            .setView(vista)
            .create()

        // TRUCO DE DISEÑADOR: Hacemos el fondo transparente para que se vea el MaterialCardView curvo
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCerrar.setOnClickListener { dialog.dismiss() }

        btnActualizarGps.setOnClickListener {
            Toast.makeText(this, getString(R.string.move_marker_toast), Toast.LENGTH_LONG).show()
        }

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val orden = etOrden.text.toString().toIntOrNull() ?: 0
            val tipo = spTipo.selectedItem.toString()
            val lat = etLat.text.toString().toDoubleOrNull() ?: latLng.latitude
            val lng = etLng.text.toString().toDoubleOrNull() ?: latLng.longitude

            if (nombre.isEmpty()) { etNombre.error = "Falta nombre"; return@setOnClickListener }
            if (orden <= 0) { etOrden.error = "Orden inválido"; return@setOnClickListener }

            validarYGuardarPunto(nombre, lat, lng, orden, tipo, dialog, puntoExistente?.id)
        }

        dialog.show()
    }

    // Tu validación intacta
    private fun validarYGuardarPunto(nombre: String, lat: Double, lng: Double, orden: Int, tipo: String, dialog: AlertDialog, idParaEditar: String?) {
        val currentUserId = auth.currentUser?.uid ?: return
        val otrosPuntos = listaPuntos.filter { it.id != idParaEditar }

        if (otrosPuntos.any { it.orden == orden }) { Toast.makeText(this, getString(R.string.order_exists, orden), Toast.LENGTH_SHORT).show(); return }
        if (tipo == "origen" && otrosPuntos.any { it.Tipo == "origen" }) { Toast.makeText(this, getString(R.string.origin_exists), Toast.LENGTH_SHORT).show(); return }
        if (tipo == "fin" && otrosPuntos.any { it.Tipo == "fin" }) { Toast.makeText(this, getString(R.string.destination_exists), Toast.LENGTH_SHORT).show(); return }

        val puntoRef = if (idParaEditar != null) {
            db.child("rutas").child(currentUserId).child(rutaId).child("puntos").child(idParaEditar)
        } else {
            db.child("rutas").child(currentUserId).child(rutaId).child("puntos").push()
        }

        val finalId = idParaEditar ?: puntoRef.key ?: ""

        val punto = PuntoRuta(id = finalId, nombre = nombre, latitud = lat, longitud = lng, orden = orden, Tipo = tipo)

        puntoRef.setValue(punto).addOnSuccessListener {
            dialog.dismiss()
            cargarPuntosRuta()
        }
    }

    private fun cargarPuntosRuta() {
        if (rutaId.isEmpty()) return
        val currentUserId = auth.currentUser?.uid ?: return

        db.child("rutas").child(currentUserId).child(rutaId).child("puntos").get().addOnSuccessListener { snapshot ->
            listaPuntos.clear()

            for (puntoSnap in snapshot.children) {
                val punto = puntoSnap.getValue(PuntoRuta::class.java)
                punto?.let { listaPuntos.add(it) }
            }

            listaPuntos.sortBy { it.orden }

            // ACTUALIZAMOS EL CONTADOR DE TU NUEVO DISEÑO
            tvContadorPuntos.text = "${listaPuntos.size} PUNTOS"

            val recycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerPuntosConfiguracion)
            if (recycler.layoutManager == null) recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

            puntoAdapter.notifyDataSetChanged()
            dibujarPuntosEnMapa()
        }
    }

    private fun dibujarPuntosEnMapa() {
        googleMap.clear()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        }

        if (listaPuntos.isEmpty()) return

        val polylineOptions = PolylineOptions().color(Color.parseColor("#006c49")).width(8f)

        for (punto in listaPuntos) {
            val posicion = LatLng(punto.latitud, punto.longitud)
            polylineOptions.add(posicion)

            val colorMarcador = when (punto.Tipo) {
                "origen" -> BitmapDescriptorFactory.HUE_GREEN
                "fin" -> BitmapDescriptorFactory.HUE_RED
                else -> BitmapDescriptorFactory.HUE_AZURE
            }

            googleMap.addMarker(MarkerOptions().position(posicion).title("${punto.orden}. ${punto.nombre}").snippet("Tipo: ${punto.Tipo}").icon(BitmapDescriptorFactory.defaultMarker(colorMarcador)))
        }

        googleMap.addPolyline(polylineOptions)
    }

    private fun eliminarPuntoFirebase(punto: PuntoRuta) {
        val currentUserId = auth.currentUser?.uid ?: return
        db.child("rutas").child(currentUserId).child(rutaId).child("puntos").child(punto.id).removeValue().addOnSuccessListener {
            Toast.makeText(this, getString(R.string.point_deleted), Toast.LENGTH_SHORT).show()
            cargarPuntosRuta()
        }
    }
}