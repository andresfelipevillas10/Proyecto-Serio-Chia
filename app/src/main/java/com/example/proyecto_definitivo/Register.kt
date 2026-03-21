package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    private lateinit var nombre: EditText
    private lateinit var apellido: EditText
    private lateinit var telefono: EditText
    private lateinit var direccion: EditText
    private lateinit var emailReg: EditText
    private lateinit var passReg: EditText
    private lateinit var btnFinalizar: Button
    private lateinit var btnBack: ImageView
    private lateinit var tvLoginRedirect: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        nombre = findViewById(R.id.nombre)
        apellido = findViewById(R.id.apellido)
        telefono = findViewById(R.id.telefono)
        direccion = findViewById(R.id.direccion)
        emailReg = findViewById(R.id.emailregister)
        passReg = findViewById(R.id.passwordregister)
        btnFinalizar = findViewById(R.id.btnDoRegister)
        btnBack = findViewById(R.id.btnBack)
        tvLoginRedirect = findViewById(R.id.tvLoginRedirect)

        btnFinalizar.setOnClickListener {
            val emailTxt = emailReg.text.toString().trim()
            val passTxt = passReg.text.toString().trim()

            if (emailTxt.isNotEmpty() && passTxt.length >= 6) {
                signUp(emailTxt, passTxt)
            } else {
                Toast.makeText(this, "Datos inválidos (Contraseña mín. 6 caracteres)", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }

        tvLoginRedirect.setOnClickListener {
            finish()
        }
    }

    private fun signUp(email: String, pass: String) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    addUserDatabase(
                        userId,
                        nombre.text.toString(),
                        apellido.text.toString(),
                        telefono.text.toString(),
                        direccion.text.toString(),
                        email
                    )
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun addUserDatabase(
        uid: String,
        nom: String,
        ape: String,
        tel: String,
        dir: String,
        mail: String
    ) {
        val user = User(uid, nom, ape, tel, dir, mail)

        dbRef.child(uid).setValue(user)
            .addOnSuccessListener {
                auth.signOut()
                Toast.makeText(this, "Registro completo", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, Login::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error en DB: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
