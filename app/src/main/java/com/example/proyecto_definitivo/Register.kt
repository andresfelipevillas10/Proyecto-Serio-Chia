package com.example.proyecto_definitivo

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Patterns
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    private lateinit var etNombre: TextInputEditText
    private lateinit var etApellido: TextInputEditText
    private lateinit var etTelefono: TextInputEditText
    private lateinit var etDireccion: TextInputEditText
    private lateinit var etEmailReg: TextInputEditText
    private lateinit var etPassReg: TextInputEditText
    private lateinit var etNoBus: TextInputEditText
    private lateinit var atvTurno: AutoCompleteTextView
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var btnPickPhoto: FloatingActionButton
    private lateinit var tvToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        initViews()
        setupTurnoDropdown()
        setupLoginLink()

        btnRegister.setOnClickListener { validateAndRegister() }
        btnBack.setOnClickListener { finish() }
        tvToLogin.setOnClickListener { finish() }

        btnPickPhoto.setOnClickListener {
            Toast.makeText(this, "Próximamente: Selector de fotos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initViews() {
        etNombre     = findViewById(R.id.etNombre)
        etApellido   = findViewById(R.id.etApellido)
        etTelefono   = findViewById(R.id.etTelefono)
        etDireccion  = findViewById(R.id.etDireccion)
        etEmailReg   = findViewById(R.id.etEmailReg)
        etPassReg    = findViewById(R.id.etPassReg)
        etNoBus      = findViewById(R.id.etNoBus)
        atvTurno     = findViewById(R.id.atvTurno)
        btnRegister  = findViewById(R.id.btnRegister)
        btnBack      = findViewById(R.id.btnBack)
        btnPickPhoto = findViewById(R.id.btnPickPhoto)
        tvToLogin    = findViewById(R.id.tvToLogin)
    }

    private fun setupTurnoDropdown() {
        val opciones = listOf("Mañana", "Tarde", "Noche")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, opciones)
        atvTurno.setAdapter(adapter)
        atvTurno.setText(opciones[0], false) // valor por defecto sin filtrar
    }

    // Colorea "Iniciar Sesión" en verde sin necesidad de HtmlCompat
    private fun setupLoginLink() {
        val full = "¿Ya tienes una cuenta? Iniciar Sesión"
        val spannable = SpannableString(full)
        val start = full.indexOf("Iniciar Sesión")
        val green = ContextCompat.getColor(this, R.color.primary_zenda)

        spannable.setSpan(ForegroundColorSpan(green), start, full.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(StyleSpan(Typeface.BOLD),    start, full.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvToLogin.text = spannable
    }

    private fun validateAndRegister() {
        val nom   = etNombre.text.toString().trim()
        val ape   = etApellido.text.toString().trim()
        val tel   = etTelefono.text.toString().trim()
        val dir   = etDireccion.text.toString().trim()
        val email = etEmailReg.text.toString().trim()
        val pass  = etPassReg.text.toString().trim()
        val bus   = etNoBus.text.toString().trim()
        val turno = atvTurno.text.toString()

        if (nom.isEmpty() || ape.isEmpty() || tel.isEmpty() || email.isEmpty() || pass.isEmpty() || bus.isEmpty()) {
            Toast.makeText(this, "Por favor, llena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email no válido", Toast.LENGTH_SHORT).show()
            return
        }
        if (pass.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        val userData = User(uid = null, nombre = nom, apellido = ape, telefono = tel,
            direccion = dir, email = email, noBus = bus, turno = turno)
        signUp(email, pass, userData)
    }

    private fun signUp(email: String, pass: String, userData: User) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    addUserDatabase(userId, userData.copy(uid = userId))
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun addUserDatabase(uid: String, user: User) {
        dbRef.child(uid).setValue(user)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Conductor registrado!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, Login::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
