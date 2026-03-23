package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnIngresar: MaterialButton
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvOlvidoPass: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        etEmail = findViewById(R.id.email)
        etPassword = findViewById(R.id.password)
        btnIngresar = findViewById(R.id.ingresar)
        btnRegister = findViewById(R.id.registrar)
        tvOlvidoPass = findViewById(R.id.tvOlvidoPassword)

        btnIngresar.setOnClickListener { validateAndLogin() }
        btnRegister.setOnClickListener {
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
            showToast(getString(R.string.error_complete_fields))
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast(getString(R.string.error_invalid_email))
            return
        }

        login(email, pass)
    }

    private fun login(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, HomeRutasActivity::class.java))
                    finish()
                } else {
                    showToast(getString(R.string.login_failed))
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}