package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CrearRuta : AppCompatActivity() {

    private lateinit var etNombreRuta: TextInputEditText
    private lateinit var etDescripcionRuta: TextInputEditText
    private lateinit var sbRadio: SeekBar
    private lateinit var tvRadioValor: TextView
    private lateinit var atvTurno: AutoCompleteTextView
    private lateinit var btnSiguiente: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var bottomNav: BottomNavigationView

    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_ruta_paso1)

        initViews()
        setupBottomNav()
        setupListeners()
    }

    private fun initViews() {
        etNombreRuta = findViewById(R.id.etNombreRuta)
        etDescripcionRuta = findViewById(R.id.etDescRuta)
        sbRadio = findViewById(R.id.sbRadio)
        tvRadioValor = findViewById(R.id.tvRadioValor)
        atvTurno = findViewById(R.id.atvTurno)
        btnSiguiente = findViewById(R.id.btnSiguiente)
        btnBack = findViewById(R.id.btnBack)
        bottomNav = findViewById(R.id.bottomNav)

        val opciones = arrayOf("Mañana 06:00 - 14:00", "Tarde 14:00 - 22:00", "Noche 22:00 - 06:00", "Personalizado")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, opciones)
        atvTurno.setAdapter(adapter)
    }

    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeRutasActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_routes -> {
                    startActivity(Intent(this, ListaRutas::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_stats -> {
                    Toast.makeText(this, getString(R.string.stats_development), Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, getString(R.string.opening_profile), Toast.LENGTH_SHORT).show()
                    false
                }
                else -> false
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        sbRadio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val valorReal = if (progress < 10) 10 else progress
                tvRadioValor.text = "${valorReal}m"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSiguiente.setOnClickListener {
            guardarRuta()
        }
    }

    private fun guardarRuta() {
        val nombre = etNombreRuta.text.toString().trim()
        val descripcion = etDescripcionRuta.text.toString().trim()

        // El radio ahora lo sacamos del SeekBar de forma 100% segura
        val radioProgreso = sbRadio.progress
        val radio = if (radioProgreso < 10) 10f else radioProgreso.toFloat()

        // Validaciones visuales
        if (nombre.isEmpty()) {
            etNombreRuta.error = "Ingrese el nombre de la ruta"
            etNombreRuta.requestFocus()
            return
        }

        if (descripcion.isEmpty()) {
            etDescripcionRuta.error = "Ingrese la descripción"
            etDescripcionRuta.requestFocus()
            return
        }

        val currentUserId = auth.currentUser?.uid ?: return

        // Bloqueamos el botón y damos feedback visual mientras Firebase trabaja
        btnSiguiente.isEnabled = false
        btnSiguiente.text = getString(R.string.saving)

        val rutaRef = db.child("rutas").child(currentUserId).push()
        val rutaId = rutaRef.key ?: ""

        // Tu data class original de Ruta
        val ruta = Ruta(
            id = rutaId,
            userId = currentUserId,
            nombre = nombre,
            descripcion = descripcion,
            activa = true,
            radioDeteccion = radio,
            creadaEn = System.currentTimeMillis()
        )

        rutaRef.setValue(ruta)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.route_created_step1), Toast.LENGTH_SHORT).show()

                // EL CAMBIO MAESTRO: Saltamos directo al Paso 2
                val intent = Intent(this, ConfigurarPuntosRuta::class.java)
                intent.putExtra("rutaId", rutaId)
                intent.putExtra("rutaNombre", nombre)
                intent.putExtra("rutaRadio", radio)
                startActivity(intent)

                // Cerramos esta pantalla para no dañar la pila de navegación
                finish()
            }
            .addOnFailureListener { e ->
                btnSiguiente.isEnabled = true
                btnSiguiente.text = "Siguiente Paso"
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}