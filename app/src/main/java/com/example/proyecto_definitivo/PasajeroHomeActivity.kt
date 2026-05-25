package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PasajeroHomeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GUEST_MODE = "GUEST_MODE"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference
    private var isGuestMode = false

    private lateinit var tvGreeting: TextView
    private lateinit var rvRutasSalientes: RecyclerView
    private lateinit var cardEmptyState: MaterialCardView
    private lateinit var bottomNav: BottomNavigationView

    private val listaRecorridos = mutableListOf<Recorrido>()
    private lateinit var adapter: RecorridoActivoAdapter
    private var recorridosListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pasajero_home)

        isGuestMode = intent.getBooleanExtra(EXTRA_GUEST_MODE, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference

        initViews()
        if (isGuestMode) {
            tvGreeting.text = "Rutas disponibles"
        } else {
            loadPassengerName()
        }
        setupRecyclerView()
        listenActiveRecorridos()
        setupBottomNav()
    }

    private fun initViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        rvRutasSalientes = findViewById(R.id.rvRutasSalientes)
        cardEmptyState = findViewById(R.id.cardEmptyState)
        bottomNav = findViewById(R.id.bottomNavPasajero)
    }

    private fun loadPassengerName() {
        val uid = auth.currentUser?.uid ?: return
        db.child("pasajeros").child(uid).child("nombre")
            .get().addOnSuccessListener { snapshot ->
                val nombre = snapshot.getValue(String::class.java)

                if (!nombre.isNullOrBlank()) {
                    tvGreeting.text = getString(R.string.hello_user, nombre)
                } else {
                    db.child("users").child(uid).child("nombre")
                        .get().addOnSuccessListener { fallbackSnapshot ->
                            val fallbackName = fallbackSnapshot.getValue(String::class.java) ?: "Pasajero"
                            tvGreeting.text = getString(R.string.hello_user, fallbackName)
                        }
                }
            }
    }

    private fun setupRecyclerView() {
        rvRutasSalientes.layoutManager = LinearLayoutManager(this)
        adapter = RecorridoActivoAdapter(listaRecorridos) { recorrido ->
            val intent = Intent(this, SeguimientoBusActivity::class.java).apply {
                putExtra("recorridoId", recorrido.id)
                putExtra("rutaId", recorrido.rutaId)
                putExtra("conductorId", recorrido.usuarioId)
                putExtra("rutaNombre", recorrido.rutaNombre)
            }
            startActivity(intent)
        }
        rvRutasSalientes.adapter = adapter
    }

    private fun listenActiveRecorridos() {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaRecorridos.clear()

                for (conductorSnap in snapshot.children) {
                    for (recorridoSnap in conductorSnap.children) {
                        val recorrido = recorridoSnap.getValue(Recorrido::class.java) ?: continue
                        if (recorrido.estado == "en_proceso") {
                            if (recorrido.id.isEmpty()) {
                                recorrido.id = recorridoSnap.key ?: continue
                            }
                            listaRecorridos.add(recorrido)
                        }
                    }
                }

                listaRecorridos.sortByDescending { it.inicioTiempo }
                adapter.notifyDataSetChanged()
                updateEmptyState()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@PasajeroHomeActivity,
                    "Error al cargar rutas: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        db.child("recorridos").addValueEventListener(listener)
        recorridosListener = listener
    }

    private fun updateEmptyState() {
        val hayRecorridos = listaRecorridos.isNotEmpty()
        cardEmptyState.visibility = if (hayRecorridos) View.GONE else View.VISIBLE
        rvRutasSalientes.visibility = if (hayRecorridos) View.VISIBLE else View.GONE
    }

    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.nav_passenger_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_passenger_home -> true
                R.id.nav_passenger_profile -> {
                    if (isGuestMode) {
                        Toast.makeText(this, "Inicia sesión para ver tu perfil", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, Login::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(intent)
                    } else {
                        startActivity(Intent(this, PerfilActivity::class.java))
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recorridosListener?.let {
            db.child("recorridos").removeEventListener(it)
        }
    }
}
