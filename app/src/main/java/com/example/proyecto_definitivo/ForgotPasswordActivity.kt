package com.example.proyecto_definitivo

import android.os.Bundle
import android.util.Patterns
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        // ─── Vinculación con los nuevos IDs del XML ───
        val etEmailRecover = findViewById<TextInputEditText>(R.id.etEmailRecover)
        val btnSend = findViewById<MaterialButton>(R.id.btnSendInstructions)
        val tvVolver = findViewById<TextView>(R.id.tvBackToLogin)
        val btnBackArrow = findViewById<ImageButton>(R.id.btnBackRecover) // La flechita

        // Lógica del botón principal
        btnSend.setOnClickListener {
            val email = etEmailRecover.text.toString().trim()
            validateAndRecover(email)
        }

        // Volver al login (Texto inferior)
        tvVolver.setOnClickListener {
            finish()
        }

        // Volver al login (Flechita superior)
        btnBackArrow.setOnClickListener {
            finish()
        }
    }

    private fun validateAndRecover(email: String) {
        if (email.isEmpty()) {
            showToast("Por favor, ingresa tu correo electrónico")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("El formato del correo no es válido")
            return
        }

        // Firebase: Envío de correo de recuperación
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("¡Listo! Revisa tu bandeja de entrada")
                    finish() // Cerramos la pantalla al tener éxito
                } else {
                    showToast("Error: ${task.exception?.message}")
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}