package com.example.proyecto_definitivo

import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage


class Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var inputNombre: TextInputLayout
    private lateinit var inputApellido: TextInputLayout
    private lateinit var inputEmail: TextInputLayout
    private lateinit var inputPassword: TextInputLayout
    private lateinit var inputTelefono: TextInputLayout
    private lateinit var inputDireccion: TextInputLayout
    private lateinit var inputBus: TextInputLayout
    private lateinit var inputTurno: TextInputLayout

    private lateinit var imgProfile: View // Changed to View because of FrameLayout ID usage or we need to find it by child
    private var imageUri: Uri? = null

    private var role: String = "PASAJERO"

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri = it
                // It's a FrameLayout in XML, first child is a View, second is ImageView
                val iv = (imgProfile as FrameLayout).getChildAt(1) as ImageView
                iv.setImageURI(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // 🔥 Obtener rol desde SelectionActivity
        role = intent.getStringExtra("USER_ROLE") ?: "PASAJERO"

        auth = FirebaseAuth.getInstance()

        // IDs in layout are mostly TextInputEditText, but we want TextInputLayout for error setting
        // Actually, looking at layout, some don't have IDs on TextInputLayout but only on TextInputEditText
        // Let's find the parents of the EditTexts

        inputNombre = (findViewById<EditText>(R.id.etNombre).parent.parent as View).parent as TextInputLayout
        inputApellido = (findViewById<EditText>(R.id.etApellido).parent.parent as View).parent as TextInputLayout
        inputEmail = (findViewById<EditText>(R.id.etEmailReg).parent.parent as View).parent as TextInputLayout
        inputPassword = findViewById(R.id.inputPassword)
        inputTelefono = (findViewById<EditText>(R.id.etTelefono).parent.parent as View).parent as TextInputLayout
        inputDireccion = (findViewById<EditText>(R.id.etDireccion).parent.parent as View).parent as TextInputLayout
        inputBus = (findViewById<EditText>(R.id.etNoBus).parent.parent as View).parent as TextInputLayout
        inputTurno = findViewById(R.id.inputTurno)

        imgProfile = findViewById(R.id.profileContainer)

        findViewById<Button>(R.id.btnRegister).setOnClickListener { validate() }
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnPickPhoto)
            .setOnClickListener { pickImage.launch("image/*") }

        configurarVistaSegunRol()
    }

    // 🔥 Oculta campos si es pasajero
    private fun configurarVistaSegunRol() {
        if (role == "PASAJERO") {
            inputBus.visibility = LinearLayout.GONE
            inputTurno.visibility = LinearLayout.GONE
        }
    }

    private fun validate() {
        clearErrors()

        val nombre = findViewById<EditText>(R.id.etNombre).text.toString()
        val apellido = findViewById<EditText>(R.id.etApellido).text.toString()
        val email = findViewById<EditText>(R.id.etEmailReg).text.toString()
        val pass = findViewById<EditText>(R.id.etPassReg).text.toString()

        val telefono = findViewById<EditText>(R.id.etTelefono).text.toString()
        val direccion = findViewById<EditText>(R.id.etDireccion).text.toString()
        val bus = findViewById<EditText>(R.id.etNoBus).text.toString()
        val turno = findViewById<AutoCompleteTextView>(R.id.atvTurno).text.toString()

        var valid = true

        if (nombre.length < 2) {
            inputNombre.error = "Nombre inválido"
            valid = false
        }

        if (apellido.length < 2) {
            inputApellido.error = "Apellido inválido"
            valid = false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.error = "Email inválido"
            valid = false
        }

        if (pass.length < 6) {
            inputPassword.error = "Mínimo 6 caracteres"
            valid = false
        }

        if (telefono.length < 7) {
            inputTelefono.error = "Teléfono inválido"
            valid = false
        }

        if (direccion.isEmpty()) {
            inputDireccion.error = "Dirección requerida"
            valid = false
        }

        // 🔥 Validación solo para conductores
        if (role == "CONDUCTOR") {
            if (bus.isEmpty()) {
                inputBus.error = "Número de bus requerido"
                valid = false
            }

            if (turno.isEmpty()) {
                inputTurno.error = "Turno requerido"
                valid = false
            }
        }

        if (imageUri == null) {
            Toast.makeText(this, "Sube una foto", Toast.LENGTH_SHORT).show()
            valid = false
        }

        if (!valid) return

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                val uid = auth.currentUser!!.uid

                uploadImage(
                    uid,
                    nombre,
                    apellido,
                    telefono,
                    direccion,
                    email,
                    bus,
                    turno
                )
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImage(
        uid: String,
        nombre: String,
        apellido: String,
        telefono: String,
        direccion: String,
        email: String,
        bus: String,
        turno: String
    ) {
        val ref = FirebaseStorage.getInstance().reference.child("profile/$uid.jpg")

        ref.putFile(imageUri!!)
            .continueWithTask { task -> ref.downloadUrl }
            .addOnSuccessListener { url ->

                val user = User(
                    uid = uid,
                    nombre = nombre,
                    apellido = apellido,
                    telefono = telefono,
                    direccion = direccion,
                    email = email,
                    noBus = if (role == "CONDUCTOR") bus else "",
                    turno = if (role == "CONDUCTOR") turno else "",
                    rol = role
                )

                FirebaseDatabase.getInstance().reference
                    .child("users")
                    .child(uid)
                    .setValue(user)

                Toast.makeText(this, "Registrado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al subir imagen", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearErrors() {
        inputNombre.error = null
        inputApellido.error = null
        inputEmail.error = null
        inputPassword.error = null
        inputTelefono.error = null
        inputDireccion.error = null
        inputBus.error = null
        inputTurno.error = null
    }
}


