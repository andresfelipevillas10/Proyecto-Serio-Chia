package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class SelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.selection_activity) // El XML que diseñamos previamente

        setupSelectionLogic()
    }

    private fun setupSelectionLogic() {
        val cardPasajero: MaterialCardView = findViewById(R.id.cardPasajero)
        val cardConductor: MaterialCardView = findViewById(R.id.cardConductor)

        cardPasajero.setOnClickListener {
            startRegistration("PASAJERO")
        }

        cardConductor.setOnClickListener {
            startRegistration("CONDUCTOR")
        }
    }

    /**
     * Despacho de flujo basado en rol.
     * Se utiliza un solo punto de salida para facilitar el mantenimiento.
     */
    private fun startRegistration(role: String) {
        val intent = Intent(this, Register::class.java).apply {
            putExtra("USER_ROLE", role)
        }
        startActivity(intent)
    }
}