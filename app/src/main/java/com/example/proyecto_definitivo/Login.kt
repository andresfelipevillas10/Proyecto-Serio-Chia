package com.example.proyecto_definitivo

import android.content.Context
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
import com.google.firebase.database.FirebaseDatabase

class Login : AppCompatActivity() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // Referencias de UI
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnIngresar: MaterialButton
    private lateinit var tvRegister: TextView
    private lateinit var tvOlvidoPass: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnIngresar = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        tvOlvidoPass = findViewById(R.id.tvOlvidoPassword)

        tvRegister.text = HtmlCompat.fromHtml(
            getString(R.string.new_user),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    private fun setupListeners() {
        btnIngresar.setOnClickListener { validateAndLogin() }

        // REDIRECCIÓN OPTIMIZADA: Hacia SelectionActivity (Rol Selection)
        tvRegister.setOnClickListener {
            navigateTo(SelectionActivity::class.java)
        }

        tvOlvidoPass.setOnClickListener {
            navigateTo(ForgotPasswordActivity::class.java)
        }
    }

    private fun validateAndLogin() {
        val email = etEmail.text.toString().trim()
        val pass = etPassword.text.toString().trim()

        when {
            email.isEmpty() || pass.isEmpty() -> toast(getString(R.string.error_complete_fields))
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> toast(getString(R.string.error_invalid_format_email))
            else -> login(email, pass)
        }
    }

    private fun login(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    checkRoleAndNavigate()
                } else {
                    toast(getString(R.string.error_login_generic, task.exception?.message ?: ""))
                }
            }
    }

    private fun checkRoleAndNavigate() {
        val uid = auth.currentUser?.uid ?: run {
            navigateTo(HomeRutasActivity::class.java, finishCurrent = true)
            return
        }
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        usersRef.child("conductores").child(uid).get()
            .addOnSuccessListener { conductorSnap ->
                if (conductorSnap.exists()) {
                    navigateTo(HomeRutasActivity::class.java, finishCurrent = true)
                } else {
                    usersRef.child("pasajeros").child(uid).get()
                        .addOnSuccessListener { pasajeroSnap ->
                            if (pasajeroSnap.exists()) {
                                navigateTo(PasajeroHomeActivity::class.java, finishCurrent = true)
                            } else {
                                navigateTo(HomeRutasActivity::class.java, finishCurrent = true)
                            }
                        }
                        .addOnFailureListener {
                            navigateTo(HomeRutasActivity::class.java, finishCurrent = true)
                        }
                }
            }
            .addOnFailureListener {
                navigateTo(HomeRutasActivity::class.java, finishCurrent = true)
            }
    }

    // Funciones de utilidad idiomáticas
    private fun <T> navigateTo(clazz: Class<T>, finishCurrent: Boolean = false) {
        startActivity(Intent(this, clazz))
        if (finishCurrent) finish()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}