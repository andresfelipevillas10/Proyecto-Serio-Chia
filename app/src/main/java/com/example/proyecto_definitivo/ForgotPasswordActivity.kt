package com.example.proyecto_definitivo

import android.os.Bundle
import android.util.Patterns
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

        val etEmailRecover = findViewById<TextInputEditText>(R.id.emailRecover)
        val btnRecover = findViewById<MaterialButton>(R.id.btnRecover)
        val tvVolver = findViewById<TextView>(R.id.tvVolverLogin)

        btnRecover.setOnClickListener {
            val email = etEmailRecover.text.toString().trim()
            validateAndRecover(email)
        }

        tvVolver.setOnClickListener {
            finish()
        }
    }

    private fun validateAndRecover(email: String) {
        if (email.isEmpty()) {
            showToast(getString(R.string.error_complete_fields))
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast(getString(R.string.error_invalid_email))
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast(getString(R.string.instructions_sent))
                    finish()
                } else {
                    showToast(getString(R.string.auth_failed, task.exception?.message))
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
