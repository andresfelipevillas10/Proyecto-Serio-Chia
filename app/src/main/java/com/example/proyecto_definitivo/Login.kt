package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var ingresar: Button
    private lateinit var register: Button
    private lateinit var olvidoPass: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        ingresar = findViewById(R.id.ingresar)
        register = findViewById(R.id.registrar)
        olvidoPass = findViewById(R.id.tvOlvidoPassword)

        ingresar.setOnClickListener {
            val emailTxt = email.text.toString().trim()
            val passTxt = password.text.toString().trim()

            if (emailTxt.isNotEmpty() && passTxt.isNotEmpty()) {
                login(emailTxt, passTxt)
            } else {
                Toast.makeText(this, "Por favor, llene los campos", Toast.LENGTH_SHORT).show()
            }
        }

        register.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        olvidoPass.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun login(emailTxt: String, passTxt: String) {
        auth.signInWithEmailAndPassword(emailTxt, passTxt)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this@Login, HomeRutasActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@Login, "Error, usuario no encontrado", Toast.LENGTH_SHORT).show()
                }
            }
    }
}