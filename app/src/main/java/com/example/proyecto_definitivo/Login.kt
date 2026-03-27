package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnIngresar: MaterialButton
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
        btnIngresar = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        tvOlvidoPass = findViewById(R.id.tvOlvidoPassword)

        val textoRegistro = getString(R.string.new_user)
        tvRegister.text = HtmlCompat.fromHtml(textoRegistro, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    private fun setupListeners() {
        btnIngresar.setOnClickListener { validateAndLogin() }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        tvOlvidoPass.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun validateAndLogin() {
        val email = etEmail.text.toString().trim()
        val pass = etPassword.text.toString().trim()

        if (email.isEmpty() || pass.isEmpty()) {
            showToast("Por favor, completa todos los campos")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("El formato del correo no es válido")
            return
        }

        login(email, pass)
    }

    private fun login(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, HomeRutasActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    finish()
                } else {
                    // Muestra el error real de Firebase para debuguear rápido
                    showToast("Error: ${task.exception?.message}")
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}