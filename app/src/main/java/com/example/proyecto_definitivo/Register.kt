package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
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
    private lateinit var btnFinalizar: MaterialButton
    private lateinit var btnBack: ImageView
    private lateinit var tvLoginRedirect: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        etNombre = findViewById(R.id.nombre)
        etApellido = findViewById(R.id.apellido)
        etTelefono = findViewById(R.id.telefono)
        etDireccion = findViewById(R.id.direccion)
        etEmailReg = findViewById(R.id.emailregister)
        etPassReg = findViewById(R.id.passwordregister)
        btnFinalizar = findViewById(R.id.btnDoRegister)
        btnBack = findViewById(R.id.btnBack)
        tvLoginRedirect = findViewById(R.id.tvLoginRedirect)

        btnFinalizar.setOnClickListener { validateAndRegister() }
        btnBack.setOnClickListener { finish() }
        tvLoginRedirect.setOnClickListener { finish() }
    }

    private fun validateAndRegister() {
        val nom = etNombre.text.toString().trim()
        val ape = etApellido.text.toString().trim()
        val tel = etTelefono.text.toString().trim()
        val dir = etDireccion.text.toString().trim()
        val email = etEmailReg.text.toString().trim()
        val pass = etPassReg.text.toString().trim()

        if (nom.isEmpty() || ape.isEmpty() || tel.isEmpty() || dir.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            showToast(getString(R.string.error_complete_fields))
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast(getString(R.string.error_invalid_email))
            return
        }

        if (pass.length < 6) {
            showToast(getString(R.string.error_short_password))
            return
        }

        val userData = User(null, nom, ape, tel, dir, email)
        signUp(email, pass, userData)
    }

    private fun signUp(email: String, pass: String, userData: User) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    addUserDatabase(userId, userData.copy(uid = userId))
                } else {
                    showToast(getString(R.string.auth_failed, task.exception?.message))
                }
            }
    }

    private fun addUserDatabase(uid: String, user: User) {
        dbRef.child(uid).setValue(user)
            .addOnSuccessListener {
                auth.signOut()
                showToast(getString(R.string.registration_complete))
                startActivity(Intent(this, Login::class.java))
                finish()
            }
            .addOnFailureListener {
                showToast(getString(R.string.db_error, it.message))
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
