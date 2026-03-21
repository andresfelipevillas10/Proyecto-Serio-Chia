package com.example.proyecto_definitivo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        val emailInput = findViewById<EditText>(R.id.emailRecover)
        val btnRecover = findViewById<Button>(R.id.btnRecover)
        val tvVolver = findViewById<TextView>(R.id.tvVolverLogin)

        btnRecover.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Instrucciones enviadas al correo", Toast.LENGTH_LONG).show()
                            finish() // Cierra la pantalla y vuelve al Login
                        } else {
                            Toast.makeText(this, "Error: Verifique el correo ingresado", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Por favor, ingrese un correo válido", Toast.LENGTH_SHORT).show()
            }
        }

        tvVolver.setOnClickListener {
            finish() // Destruye esta activity y regresa al Login
        }
    }
}