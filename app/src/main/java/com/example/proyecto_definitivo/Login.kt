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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase

class Login : AppCompatActivity() {
    companion object {
        private const val AUTH_PREFS = "auth_dev_prefs"
        // Bandera de desarrollo: fuerza mostrar Login y deshabilita auto-login.
        private const val FORCE_LOGIN_SCREEN = false
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    // Referencias de UI
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnIngresar: MaterialButton
    private lateinit var tvRegister: TextView
    private lateinit var tvOlvidoPass: TextView
    private lateinit var tvVerRutas: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupListeners()

        if (FORCE_LOGIN_SCREEN) {
            clearSessionState()
        } else if (auth.currentUser != null) {
            checkRoleAndNavigate()
        }
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnIngresar = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        tvOlvidoPass = findViewById(R.id.tvOlvidoPassword)
        tvVerRutas = findViewById(R.id.tvVerRutas)

        tvRegister.text = HtmlCompat.fromHtml(
            getString(R.string.new_user),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    private fun setupListeners() {
        btnIngresar.setOnClickListener { validateAndLogin() }

        tvRegister.setOnClickListener {
            navigateTo(SelectionActivity::class.java)
        }

        tvOlvidoPass.setOnClickListener {
            navigateTo(ForgotPasswordActivity::class.java)
        }

        tvVerRutas.setOnClickListener {
            val intent = Intent(this, PasajeroHomeActivity::class.java).apply {
                putExtra(PasajeroHomeActivity.EXTRA_GUEST_MODE, true)
            }
            startActivity(intent)
        }
    }

    private fun validateAndLogin() {
        val email = etEmail.text.toString().trim()
        val pass = etPassword.text.toString().trim()

        when {
            email.isEmpty() || pass.isEmpty() -> toast("Por favor, completa todos los campos")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> toast("El formato del correo no es válido")
            else -> login(email, pass)
        }
    }

    private fun login(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    checkRoleAndNavigate()
                } else {
                    toast("Error: ${task.exception?.message}")
                }
            }
    }

    private fun checkRoleAndNavigate() {
        val uid = auth.currentUser?.uid ?: run {
            return
        }

        db.child("users").child(uid).child("rol").get()
            .addOnSuccessListener { roleSnapshot ->
                val role = roleSnapshot.getValue(String::class.java)

                if (!role.isNullOrBlank()) {
                    navigateByRole(role)
                } else {
                    resolveLegacyRole(uid)
                }
            }
            .addOnFailureListener {
                resolveLegacyRole(uid)
            }
    }

    private fun resolveLegacyRole(uid: String) {
        db.child("conductores").child(uid).get()
            .addOnSuccessListener { conductorSnap ->
                if (conductorSnap.exists()) {
                    syncUserIndex(uid, conductorSnap, "CONDUCTOR")
                    navigateByRole("CONDUCTOR")
                } else {
                    db.child("pasajeros").child(uid).get()
                        .addOnSuccessListener { pasajeroSnap ->
                            if (pasajeroSnap.exists()) {
                                syncUserIndex(uid, pasajeroSnap, "PASAJERO")
                                navigateByRole("PASAJERO")
                            } else {
                                auth.signOut()
                                toast("No se encontró el perfil del usuario.")
                            }
                        }
                        .addOnFailureListener {
                            auth.signOut()
                            toast("No fue posible validar el perfil del pasajero.")
                        }
                }
            }
            .addOnFailureListener {
                auth.signOut()
                toast("No fue posible validar el perfil del conductor.")
            }
    }

    private fun syncUserIndex(uid: String, snapshot: DataSnapshot, role: String) {
        val updates = mutableMapOf<String, Any>(
            "uid" to uid,
            "rol" to role,
            "nombre" to (snapshot.child("nombre").getValue(String::class.java) ?: ""),
            "apellido" to (snapshot.child("apellido").getValue(String::class.java) ?: ""),
            "telefono" to (snapshot.child("telefono").getValue(String::class.java) ?: ""),
            "direccion" to (snapshot.child("direccion").getValue(String::class.java) ?: ""),
            "email" to (snapshot.child("email").getValue(String::class.java) ?: "")
        )

        db.child("users").child(uid).updateChildren(updates)
    }

    private fun navigateByRole(role: String) {
        val target = if (role.uppercase() == "CONDUCTOR") {
            HomeRutasActivity::class.java
        } else {
            PasajeroHomeActivity::class.java
        }

        val intent = Intent(this, target).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }

    private fun <T> navigateTo(clazz: Class<T>) {
        startActivity(Intent(this, clazz))
    }

    private fun clearSessionState() {
        // 1) Limpia cualquier preferencia local usada para sesión en desarrollo.
        getSharedPreferences(AUTH_PREFS, MODE_PRIVATE).edit().clear().apply()
        // 2) Invalida token/sesión de Firebase Authentication.
        auth.signOut()
        // 3) Limpia caché en memoria de credenciales mostradas en UI.
        etEmail.text?.clear()
        etPassword.text?.clear()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
