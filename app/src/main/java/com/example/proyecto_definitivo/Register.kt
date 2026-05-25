package com.example.proyecto_definitivo

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class Register : AppCompatActivity() {
    companion object {
        private const val AUTH_PREFS = "auth_dev_prefs"
    }

    private lateinit var auth: FirebaseAuth

    private lateinit var inputNombre: TextInputLayout
    private lateinit var inputApellido: TextInputLayout
    private lateinit var inputEmail: TextInputLayout
    private lateinit var inputPassword: TextInputLayout
    private lateinit var inputTelefono: TextInputLayout
    private lateinit var inputDireccion: TextInputLayout
    private lateinit var inputBus: TextInputLayout
    private lateinit var inputTurno: TextInputLayout
    private lateinit var inputPlaca: TextInputLayout
    private lateinit var inputNumLicencia: TextInputLayout
    private lateinit var inputVencimientoLicencia: TextInputLayout
    private lateinit var inputEmpresaTransporte: TextInputLayout

    private var role: String = "PASAJERO"
    private var vencimientoLicenciaRaw: String = "" // formato YYYY-MM-DD para Firebase



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // 🔥 Obtener rol desde SelectionActivity
        role = intent.getStringExtra("USER_ROLE") ?: "PASAJERO"

        auth = FirebaseAuth.getInstance()

        inputNombre = findViewById(R.id.tilNombre)
        inputApellido = findViewById(R.id.tilApellido)
        inputEmail = findViewById(R.id.tilEmail)
        inputPassword = findViewById(R.id.inputPassword)
        inputTelefono = findViewById(R.id.tilTelefono)
        inputDireccion = findViewById(R.id.tilDireccion)
        inputBus = findViewById(R.id.tilNoBus)
        inputTurno = findViewById(R.id.inputTurno)
        inputPlaca = findViewById(R.id.tilPlaca)
        inputNumLicencia = findViewById(R.id.tilNumLicencia)
        inputVencimientoLicencia = findViewById(R.id.tilVencimientoLicencia)
        inputEmpresaTransporte = findViewById(R.id.tilEmpresaTransporte)
        findViewById<Button>(R.id.btnRegister).setOnClickListener { validate() }
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvToLogin).setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        configurarDatePickerLicencia()
        limpiarCampos()
        configurarTurnos()
        configurarVistaSegunRol()
        configurarValidacionesTiempoReal()
    }

    private fun configurarTurnos() {
        val atvTurno = findViewById<AutoCompleteTextView>(R.id.atvTurno)
        val turnos = resources.getStringArray(R.array.turnos_conductor).toList()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, turnos)
        atvTurno.setAdapter(adapter)

        atvTurno.setOnClickListener { atvTurno.showDropDown() }
        atvTurno.setOnItemClickListener { _, _, _, _ ->
            inputTurno.error = null
        }
    }

    private fun configurarValidacionesTiempoReal() {
        findViewById<EditText>(R.id.etNombre).setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && findViewById<EditText>(R.id.etNombre).text.length < 2) inputNombre.error = "Nombre inválido" else inputNombre.error = null
        }
        findViewById<EditText>(R.id.etApellido).setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && findViewById<EditText>(R.id.etApellido).text.length < 2) inputApellido.error = "Apellido inválido" else inputApellido.error = null
        }
        findViewById<EditText>(R.id.etEmailReg).setOnFocusChangeListener { _, hasFocus ->
            val email = findViewById<EditText>(R.id.etEmailReg).text.toString()
            if (!hasFocus && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) inputEmail.error = "Email inválido" else inputEmail.error = null
        }
        findViewById<EditText>(R.id.etPassReg).setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && findViewById<EditText>(R.id.etPassReg).text.length < 6) inputPassword.error = "Mínimo 6 caracteres" else inputPassword.error = null
        }
    }

    private fun limpiarCampos() {
        findViewById<EditText>(R.id.etNombre).text.clear()
        findViewById<EditText>(R.id.etApellido).text.clear()
        findViewById<EditText>(R.id.etEmailReg).text.clear()
        findViewById<EditText>(R.id.etPassReg).text.clear()
        findViewById<EditText>(R.id.etTelefono).text.clear()
        findViewById<EditText>(R.id.etDireccion).text.clear()
        findViewById<EditText>(R.id.etNoBus).text.clear()
        findViewById<AutoCompleteTextView>(R.id.atvTurno).text.clear()
        findViewById<EditText>(R.id.etPlaca).text.clear()
        findViewById<EditText>(R.id.etNumLicencia).text.clear()
        findViewById<EditText>(R.id.etVencimientoLicencia).text.clear()
        findViewById<EditText>(R.id.etEmpresaTransporte).text.clear()
        vencimientoLicenciaRaw = ""
        clearErrors()
    }

    private fun configurarVistaSegunRol() {
        val subtitle = findViewById<TextView>(R.id.tvRegSubtitle)
        val lblPlaca = findViewById<TextView>(R.id.lblPlaca)
        val lblNumLicencia = findViewById<TextView>(R.id.lblNumLicencia)
        val lblVencimiento = findViewById<TextView>(R.id.lblVencimientoLicencia)
        val lblEmpresa = findViewById<TextView>(R.id.lblEmpresaTransporte)

        if (role == "PASAJERO") {
            inputBus.visibility = View.GONE
            inputTurno.visibility = View.GONE
            inputPlaca.visibility = View.GONE
            inputNumLicencia.visibility = View.GONE
            inputVencimientoLicencia.visibility = View.GONE
            inputEmpresaTransporte.visibility = View.GONE
            lblPlaca.visibility = View.GONE
            lblNumLicencia.visibility = View.GONE
            lblVencimiento.visibility = View.GONE
            lblEmpresa.visibility = View.GONE
            subtitle.text = "Completa tus datos para ingresar como pasajero."
        } else {
            inputBus.visibility = View.VISIBLE
            inputTurno.visibility = View.VISIBLE
            inputPlaca.visibility = View.VISIBLE
            inputNumLicencia.visibility = View.VISIBLE
            inputVencimientoLicencia.visibility = View.VISIBLE
            inputEmpresaTransporte.visibility = View.VISIBLE
            lblPlaca.visibility = View.VISIBLE
            lblNumLicencia.visibility = View.VISIBLE
            lblVencimiento.visibility = View.VISIBLE
            lblEmpresa.visibility = View.VISIBLE
            subtitle.text = "Completa tus datos para ingresar como conductor."
        }
    }

    private fun configurarDatePickerLicencia() {
        val etVencimiento = findViewById<EditText>(R.id.etVencimientoLicencia)
        val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val showPicker = {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selected = Calendar.getInstance().apply { set(year, month, day) }
                    etVencimiento.setText(displayFormat.format(selected.time))
                    vencimientoLicenciaRaw = String.format("%04d-%02d-%02d", year, month + 1, day)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.minDate = System.currentTimeMillis() // solo fechas futuras
            }.show()
        }

        etVencimiento.setOnClickListener { showPicker() }
        inputVencimientoLicencia.setEndIconOnClickListener { showPicker() }
    }

    private fun validate() {
        clearErrors()

        val nombre = findViewById<EditText>(R.id.etNombre).text.toString().trim()
        val apellido = findViewById<EditText>(R.id.etApellido).text.toString().trim()
        val email = findViewById<EditText>(R.id.etEmailReg).text.toString().trim()
        val pass = findViewById<EditText>(R.id.etPassReg).text.toString().trim()

        val telefono = findViewById<EditText>(R.id.etTelefono).text.toString().trim()
        val direccion = findViewById<EditText>(R.id.etDireccion).text.toString().trim()
        val bus = findViewById<EditText>(R.id.etNoBus).text.toString().trim()
        val turno = findViewById<AutoCompleteTextView>(R.id.atvTurno).text.toString().trim()

        var valid = true

        if (nombre.length < 2) {
            inputNombre.error = "Nombre inválido"
            valid = false
        }

        if (apellido.length < 2) {
            inputApellido.error = "Apellido inválido"
            valid = false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.error = "Email inválido"
            valid = false
        }

        if (pass.length < 6) {
            inputPassword.error = "Mínimo 6 caracteres"
            valid = false
        }

        if (telefono.length < 7) {
            inputTelefono.error = "Teléfono inválido"
            valid = false
        }

        if (direccion.isEmpty()) {
            inputDireccion.error = "Dirección requerida"
            valid = false
        }

        if (role == "CONDUCTOR") {
            val placa = findViewById<EditText>(R.id.etPlaca).text.toString().trim().uppercase()
            val numLicencia = findViewById<EditText>(R.id.etNumLicencia).text.toString().trim()
            val empresaTransporte = findViewById<EditText>(R.id.etEmpresaTransporte).text.toString().trim()

            if (bus.isEmpty()) {
                inputBus.error = "Número de bus requerido"
                valid = false
            }

            if (turno.isEmpty()) {
                inputTurno.error = "Turno requerido"
                valid = false
            }

            val placaRegex = Regex("^[A-Z]{3}[0-9]{2,3}[A-Z]?$")
            if (!placaRegex.matches(placa)) {
                inputPlaca.error = "Placa inválida. Use formato AAA123 o AAA12B"
                valid = false
            } else {
                inputPlaca.error = null
            }

            if (numLicencia.length < 5) {
                inputNumLicencia.error = "Número de licencia inválido"
                valid = false
            }

            if (vencimientoLicenciaRaw.isEmpty()) {
                inputVencimientoLicencia.error = "Seleccione la fecha de vencimiento"
                valid = false
            }

            if (empresaTransporte.length < 3) {
                inputEmpresaTransporte.error = "Ingrese el nombre de la empresa"
                valid = false
            }
        }
        if (!valid) return

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                val uid = auth.currentUser!!.uid
                Log.d("RegisterAuth", "Cuenta de $role creada exitosamente en Firebase Auth con UID: $uid")

                val placa = if (role == "CONDUCTOR")
                    findViewById<EditText>(R.id.etPlaca).text.toString().trim().uppercase() else ""
                val numLicencia = if (role == "CONDUCTOR")
                    findViewById<EditText>(R.id.etNumLicencia).text.toString().trim() else ""
                val empresaTransporte = if (role == "CONDUCTOR")
                    findViewById<EditText>(R.id.etEmpresaTransporte).text.toString().trim() else ""

                val user = User(
                    uid = uid,
                    nombre = nombre,
                    apellido = apellido,
                    telefono = telefono,
                    direccion = direccion,
                    email = email,
                    noBus = if (role == "CONDUCTOR") bus else "",
                    turno = if (role == "CONDUCTOR") turno else "",
                    rol = role,
                    placa = placa,
                    numLicencia = numLicencia,
                    vencimientoLicencia = if (role == "CONDUCTOR") vencimientoLicenciaRaw else "",
                    empresaTransporte = empresaTransporte
                )

                val nodeName = if (role == "CONDUCTOR") "conductores" else "pasajeros"
                val updates = hashMapOf<String, Any>(
                    "users/$uid" to createUserIndexMap(user),
                    "$nodeName/$uid" to createRoleProfileMap(user)
                )

                FirebaseDatabase.getInstance().reference
                    .updateChildren(updates)
                    .addOnSuccessListener {
                        Log.d("RegisterDB", "Datos de $role guardados correctamente en 'users/$uid' y '$nodeName/$uid'")
                        Toast.makeText(this, "Registrado correctamente", Toast.LENGTH_SHORT).show()
                        // En desarrollo, evitamos mantener sesión activa tras registro.
                        clearSessionState()
                        startActivity(Intent(this, Login::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        Log.e("RegisterDB", "Fallo al guardar datos de $role en el nodo '$nodeName': ${it.message}")
                        auth.currentUser?.delete()
                        clearSessionState()
                        Toast.makeText(this, "Error al guardar datos. Intenta registrarte nuevamente.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Log.e("RegisterAuth", "Fallo al crear cuenta de $role en Firebase Auth: ${it.message}")
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createUserIndexMap(user: User): Map<String, Any> = mapOf(
        "uid" to user.uid,
        "nombre" to user.nombre,
        "apellido" to user.apellido,
        "telefono" to user.telefono,
        "direccion" to user.direccion,
        "email" to user.email,
        "rol" to user.rol
    )

    private fun createRoleProfileMap(user: User): Map<String, Any> {
        val profile = mutableMapOf<String, Any>(
            "uid" to user.uid,
            "nombre" to user.nombre,
            "apellido" to user.apellido,
            "telefono" to user.telefono,
            "direccion" to user.direccion,
            "email" to user.email,
            "rol" to user.rol
        )

        if (user.rol == "CONDUCTOR") {
            profile["noBus"] = user.noBus
            profile["turno"] = user.turno
            profile["placa"] = user.placa
            profile["numLicencia"] = user.numLicencia
            profile["vencimientoLicencia"] = user.vencimientoLicencia
            profile["empresaTransporte"] = user.empresaTransporte
        }

        return profile
    }

    private fun clearSessionState() {
        getSharedPreferences(AUTH_PREFS, MODE_PRIVATE).edit().clear().apply()
        auth.signOut()
    }

    private fun clearErrors() {
        inputNombre.error = null
        inputApellido.error = null
        inputEmail.error = null
        inputPassword.error = null
        inputTelefono.error = null
        inputDireccion.error = null
        inputBus.error = null
        inputTurno.error = null
        inputPlaca.error = null
        inputNumLicencia.error = null
        inputVencimientoLicencia.error = null
        inputEmpresaTransporte.error = null
    }
}


