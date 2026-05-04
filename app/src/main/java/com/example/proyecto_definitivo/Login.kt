package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // Inputs
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText

    // Layouts (para errores)
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout

    // UI
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvRegister: TextView
    private lateinit var tvOlvidoPass: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)

        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        tvOlvidoPass = findViewById(R.id.tvOlvidoPassword)
    }

    private fun setupListeners() {

        btnLogin.setOnClickListener {
            validateAndLogin()
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, SelectionActivity::class.java))
        }

        tvOlvidoPass.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    // 🔥 VALIDACIÓN PRO (sin Toasts innecesarios)
    private fun validateAndLogin() {

        clearErrors()

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        var isValid = true

        if (email.isEmpty()) {
            tilEmail.error = "Campo requerido"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Correo inválido"
            isValid = false
        }

        if (password.isEmpty()) {
            tilPassword.error = "Campo requerido"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Mínimo 6 caracteres"
            isValid = false
        }

        if (!isValid) return

        loginUser(email, password)
    }

    private fun clearErrors() {
        tilEmail.error = null
        tilPassword.error = null
    }

    // 🔐 LOGIN FIREBASE
    private fun loginUser(email: String, password: String) {

        btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                btnLogin.isEnabled = true

                if (task.isSuccessful) {
                    checkUserRole()
                } else {
                    Toast.makeText(
                        this,
                        "Error: ${task.exception?.message ?: "No se pudo iniciar sesión"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // 🎯 REDIRECCIÓN SEGÚN ROL
    private fun checkUserRole() {

        val uid = auth.currentUser?.uid

        if (uid == null) {
            goTo(HomeRutasActivity::class.java)
            return
        }

        val db = FirebaseDatabase.getInstance().reference.child("users")

        db.child("conductores").child(uid).get()
            .addOnSuccessListener { conductorSnap ->

                if (conductorSnap.exists()) {
                    goTo(HomeRutasActivity::class.java, true)
                } else {

                    db.child("pasajeros").child(uid).get()
                        .addOnSuccessListener { pasajeroSnap ->

                            if (pasajeroSnap.exists()) {
                                goTo(PasajeroHomeActivity::class.java, true)
                            } else {
                                goTo(HomeRutasActivity::class.java, true)
                            }

                        }
                        .addOnFailureListener {
                            goTo(HomeRutasActivity::class.java, true)
                        }
                }

            }
            .addOnFailureListener {
                goTo(HomeRutasActivity::class.java, true)
            }
    }

    // 🚀 NAV HELPER
    private fun goTo(destination: Class<*>, finishCurrent: Boolean = false) {
        startActivity(Intent(this, destination))
        if (finishCurrent) finish()
    }
}