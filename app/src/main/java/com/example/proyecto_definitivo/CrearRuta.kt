package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.database.FirebaseDatabase

class CrearRuta : AppCompatActivity() {

    // 1. Apuntamos a los IDs del nuevo diseño XML
    private lateinit var etNombreRuta: EditText
    private lateinit var etDescripcionRuta: EditText
    private lateinit var etRadioDeteccion: EditText
    private lateinit var btnSiguientePuntos: MaterialButton
    private lateinit var btnBackCrear: ImageButton
    private lateinit var progressRoute: LinearProgressIndicator // La barrita de progreso visual

    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // MAGIA: Le decimos que use el XML del Paso 1 (ajusta el nombre si lo guardaste diferente)
        setContentView(R.layout.activity_crear_ruta_paso1)

        // 2. Enlazamos la vista
        etNombreRuta = findViewById(R.id.etRouteName)
        etDescripcionRuta = findViewById(R.id.etShortDescription)
        etRadioDeteccion = findViewById(R.id.etDetectionRadius)
        btnSiguientePuntos = findViewById(R.id.btnNextStepMap)
        btnBackCrear = findViewById(R.id.btnBackCrear)
        progressRoute = findViewById(R.id.progressRoute)

        // El botón de atrás simplemente cierra esta ventana
        btnBackCrear.setOnClickListener { finish() }

        // El botón Siguiente (que ahora hace de "Guardar")
        btnSiguientePuntos.setOnClickListener {
            guardarRuta()
        }
    }

    private fun guardarRuta() {
        val nombre = etNombreRuta.text.toString().trim()
        val descripcion = etDescripcionRuta.text.toString().trim()
        val radioTexto = etRadioDeteccion.text.toString().trim()

        // Validaciones intactas de tu código original
        if (nombre.isEmpty()) {
            etNombreRuta.error = "Ingrese el nombre de la ruta"
            etNombreRuta.requestFocus()
            return
        }

        if (descripcion.isEmpty()) {
            etDescripcionRuta.error = "Ingrese la descripción"
            etDescripcionRuta.requestFocus()
            return
        }

        if (radioTexto.isEmpty()) {
            etRadioDeteccion.error = "Ingrese el radio de detección"
            etRadioDeteccion.requestFocus()
            return
        }

        val radio = radioTexto.toFloatOrNull()
        if (radio == null || radio <= 0f) {
            etRadioDeteccion.error = "Ingrese un valor válido"
            etRadioDeteccion.requestFocus()
            return
        }

        // Bloqueamos el botón y animamos la barra de progreso mientras Firebase trabaja
        btnSiguientePuntos.isEnabled = false
        progressRoute.isIndeterminate = true

        val rutaRef = db.child("rutas").push()
        val rutaId = rutaRef.key ?: ""

        val ruta = Ruta(
            id = rutaId,
            nombre = nombre,
            descripcion = descripcion,
            activa = true,
            radioDeteccion = radio,
            creadaEn = System.currentTimeMillis()
        )

        rutaRef.setValue(ruta)
            .addOnSuccessListener {
                progressRoute.isIndeterminate = false
                progressRoute.progress = 100 // Llenamos la barra al 100%
                Toast.makeText(this, "Ruta creada. Pasando al mapa...", Toast.LENGTH_SHORT).show()

                // EL CAMBIO MAESTRO: Saltamos directo al Paso 2 enviando los datos recién creados
                val intent = Intent(this, ConfigurarPuntosRuta::class.java)
                intent.putExtra("rutaId", rutaId)
                intent.putExtra("rutaNombre", nombre)
                intent.putExtra("rutaRadio", radio)
                startActivity(intent)

                // Cerramos esta pantalla para que si el usuario da "Atrás" en el mapa, vuelva al menú, no aquí.
                finish()
            }
            .addOnFailureListener { e ->
                progressRoute.isIndeterminate = false
                btnSiguientePuntos.isEnabled = true
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}