package com.example.proyecto_definitivo

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class CrearRuta : AppCompatActivity() {

    private lateinit var etNombreRuta: TextInputEditText
    private lateinit var etDescripcionRuta: TextInputEditText
    private lateinit var cgDias: ChipGroup
    private lateinit var sbRadio: SeekBar
    private lateinit var tvRadioValor: TextView
    private lateinit var etFechaSalida: TextInputEditText
    private lateinit var etHoraSalida: TextInputEditText
    private lateinit var etHoraLlegada: TextInputEditText
    private lateinit var etCupoSentados: TextInputEditText
    private lateinit var etCupoPie: TextInputEditText
    private lateinit var tvGuiaDescripcion: TextView
    private lateinit var ivGuiaIcon: ImageView
    private lateinit var btnSiguiente: MaterialButton
    private lateinit var btnBack: ImageButton

    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_ruta_paso1)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etNombreRuta = findViewById(R.id.etNombreRuta)
        etDescripcionRuta = findViewById(R.id.etDescRuta)
        cgDias = findViewById(R.id.cgDias)
        sbRadio = findViewById(R.id.sbRadio)
        tvRadioValor = findViewById(R.id.tvRadioValor)
        etFechaSalida = findViewById(R.id.etFechaSalida)
        etHoraSalida = findViewById(R.id.etHoraSalida)
        etHoraLlegada = findViewById(R.id.etHoraLlegada)
        etCupoSentados = findViewById(R.id.etCupoSentados)
        etCupoPie = findViewById(R.id.etCupoPie)
        tvGuiaDescripcion = findViewById(R.id.tvGuiaDescripcion)
        ivGuiaIcon = findViewById(R.id.ivGuiaIcon)
        btnSiguiente = findViewById(R.id.btnSiguiente)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        setupTimePickers()

        sbRadio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val valorReal = if (progress < 10) 10 else progress
                tvRadioValor.text = "${valorReal}m"

                when {
                    valorReal < 20 -> {
                        tvGuiaDescripcion.text = "ESTRICTO: Ideal para zonas de pruebas o paraderos muy exactos."
                        ivGuiaIcon.setImageResource(R.drawable.ic_radar)
                    }
                    valorReal in 20..50 -> {
                        tvGuiaDescripcion.text = "RECOMENDADO: Ideal para paraderos estándar y calles urbanas."
                        ivGuiaIcon.setImageResource(R.drawable.ic_check_circle)
                    }
                    else -> {
                        tvGuiaDescripcion.text = "AMPLIO: Útil en avenidas grandes o zonas con mala señal GPS."
                        ivGuiaIcon.setImageResource(R.drawable.ic_warning)
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSiguiente.setOnClickListener {
            guardarRuta()
        }
    }

    private fun setupTimePickers() {
        etFechaSalida.setOnClickListener { mostrarDatePicker() }
        etHoraSalida.setOnClickListener {
            mostrarTimePicker(etHoraSalida)
        }
        etHoraLlegada.setOnClickListener {
            mostrarTimePicker(etHoraLlegada)
        }
    }

    private fun collectSelectedDays(): List<String> {
        val selected = mutableListOf<String>()
        for (i in 0 until cgDias.childCount) {
            val chip = cgDias.getChildAt(i) as Chip
            if (chip.isChecked) {
                selected.add(chip.text.toString())
            }
        }
        return selected
    }

    private fun mostrarDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar Fecha")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            etFechaSalida.setText(sdf.format(Date(selection)))
        }
        picker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun mostrarTimePicker(editText: TextInputEditText) {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(hour)
            .setMinute(minute)
            .setTitleText("Ingrese Hora (HH:mm)")
            .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD) // ⌨️ FUERZA MODO TECLADO
            .build()

        picker.addOnPositiveButtonClickListener {
            val formattedTime = String.format("%02d:%02d", picker.hour, picker.minute)
            editText.setText(formattedTime)
        }

        picker.show(supportFragmentManager, "MATERIAL_TIME_PICKER")
    }

    private fun guardarRuta() {
        val nombre = etNombreRuta.text.toString().trim()
        val descripcion = etDescripcionRuta.text.toString().trim()
        val horaSalida = etHoraSalida.text.toString().trim()
        val horaLlegada = etHoraLlegada.text.toString().trim()

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

        if (horaSalida.isEmpty()) {
            etHoraSalida.error = "Seleccione la hora de salida"
            etHoraSalida.requestFocus()
            return
        }

        if (horaLlegada.isEmpty()) {
            etHoraLlegada.error = "Seleccione la hora de llegada"
            etHoraLlegada.requestFocus()
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
            creadaEn = System.currentTimeMillis(),
            horaSalida = horaSalida,
            horaLlegada = horaLlegada,
            diasOperacion = collectSelectedDays(),
            cupoSentados = etCupoSentados.text.toString().toIntOrNull() ?: 40,
            cupoPie = etCupoPie.text.toString().toIntOrNull() ?: 15
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