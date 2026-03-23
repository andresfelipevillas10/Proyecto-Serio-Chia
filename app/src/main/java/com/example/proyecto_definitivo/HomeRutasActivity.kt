package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class HomeRutasActivity : AppCompatActivity() {

    private lateinit var btnConfigRutas: MaterialCardView
    private lateinit var btnSoporte: MaterialCardView
    private lateinit var rvRoutes: RecyclerView

    private lateinit var tabRecorrido: LinearLayout
    private lateinit var tabStats: LinearLayout
    private lateinit var tabSettings: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_rutas)

        btnConfigRutas = findViewById(R.id.btnConfigRutas)
        btnSoporte = findViewById(R.id.btnSoporte)
        rvRoutes = findViewById(R.id.rvRoutes)

        tabRecorrido = findViewById(R.id.tabRecorrido)
        tabStats = findViewById(R.id.tabStats)
        tabSettings = findViewById(R.id.tabSettings)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        btnConfigRutas.setOnClickListener {
            showToast(getString(R.string.loading_route_config))
        }

        btnSoporte.setOnClickListener {
            showToast(getString(R.string.opening_support))
        }

        tabRecorrido.setOnClickListener {
            showToast(getString(R.string.already_in_home))
        }

        tabStats.setOnClickListener {
            showToast(getString(R.string.loading_stats))
        }

        tabSettings.setOnClickListener {
            showToast(getString(R.string.opening_user_settings))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}