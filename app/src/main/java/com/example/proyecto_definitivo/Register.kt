package com.example.proyecto_definitivo

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Register : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val dbRef: DatabaseReference by lazy { FirebaseDatabase.getInstance().getReference("users") }

    private var userRole: String = "PASAJERO" // Valor por defecto

    // UI References
    private lateinit var etNombre: TextInputEditText
    private lateinit var etApellido: TextInputEditText
    private lateinit var etTelefono: TextInputEditText
    private lateinit var etEmailReg: TextInputEditText
    private lateinit var etPassReg: TextInputEditText
    private lateinit var etNoBus: TextInputEditText
    private lateinit var atvTurno: AutoCompleteTextView
    private lateinit var layoutConductorFields: LinearLayout
    private lateinit var tvRegSubtitle: TextView
    private lateinit var btnRegister: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // 1. CAPTURA DE ROL
        userRole = intent.getStringExtra("USER_ROLE") ?: "PASAJERO"

        initViews()
        applyRoleLogic()
        setupTurnoDropdown()
        setupLoginLink()

        btnRegister.setOnClickListener { validateAndRegister() }
    }

    private fun initViews() {
        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etTelefono = findViewById(R.id.etTelefono)
        etEmailReg = findViewById(R.id.etEmailReg)
        etPassReg = findViewById(R.id.etPassReg)
        etNoBus = findViewById(R.id.etNoBus)
        atvTurno = findViewById(R.id.atvTurno)
        layoutConductorFields = findViewById(R.id.layoutConductorFields)
        tvRegSubtitle = findViewById(R.id.tvRegSubtitle)
        btnRegister = findViewById(R.id.btnRegister)

    }

    // Colorea "Iniciar Sesión" en verde sin necesidad de HtmlCompat
    private fun setupLoginLink() {
        val full = "¿Ya tienes una cuenta? Iniciar Sesión"
        val spannable = SpannableString(full)
        val start = full.indexOf("Iniciar Sesión")
        val green = ContextCompat.getColor(this, R.color.primary_zenda)

        spannable.setSpan(ForegroundColorSpan(green), start, full.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(StyleSpan(Typeface.BOLD),    start, full.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    }

    private fun applyRoleLogic() {
        if (userRole == "PASAJERO") {
            // Ocultar campos de conductor
            layoutConductorFields.visibility = View.GONE
            tvRegSubtitle.text = "Únete al camino de la tranquilidad, pasajero."
        } else {
            layoutConductorFields.visibility = View.VISIBLE
            tvRegSubtitle.text = "Únete al camino de la tranquilidad, conductor."
        }
    }

    private fun validateAndRegister() {
        val email = etEmailReg.text.toString().trim()
        val pass = etPassReg.text.toString().trim()
        val nom = etNombre.text.toString().trim()
        val bus = etNoBus.text.toString().trim()

        // Validación base
        if (nom.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            toast("Completa los campos obligatorios")
            return
        }

        // Validación específica de rol (Encapsulamiento lógico)
        if (userRole == "CONDUCTOR" && bus.isEmpty()) {
            toast("El número de bus es obligatorio para conductores")
            return
        }

        // Proceder con el registro
        val userData = User(
            uid = null,
            nombre = nom,
            email = email,
            noBus = if (userRole == "CONDUCTOR") bus else null,
            turno = if (userRole == "CONDUCTOR") atvTurno.text.toString() else null,
            rol = userRole // Asegúrate de añadir el campo 'rol' en tu clase User.kt
        )

        signUp(email, pass, userData)
    }

    private fun signUp(email: String, pass: String, userData: User) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                saveInDatabase(userId, userData.copy(uid = userId))
            } else {
                toast("Error: ${task.exception?.message}")
            }
        }
    }

    private fun saveInDatabase(uid: String, user: User) {
        // Guardar bajo el nodo de su respectivo rol para Clean Data
        val path = if (userRole == "CONDUCTOR") "conductores" else "pasajeros"

        dbRef.child(path).child(uid).setValue(user).addOnSuccessListener {
            toast("¡$userRole registrado exitosamente!")
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun setupTurnoDropdown() {
        val opciones = listOf("Mañana", "Tarde", "Noche")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, opciones)
        atvTurno.setAdapter(adapter)
        atvTurno.setText(opciones[0], false) // valor por defecto sin filtrar
    }


}


