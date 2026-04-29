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

    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference

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

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference

        initViews()
        loadPassengerName()
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
        db.child("users").child("pasajeros").child(uid).child("nombre")
            .get().addOnSuccessListener { snapshot ->
                val nombre = snapshot.getValue(String::class.java) ?: "Pasajero"
                tvGreeting.text = getString(R.string.hello_user, nombre)
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

                for (recorridoSnap in snapshot.children) {
                    val recorrido = recorridoSnap.getValue(Recorrido::class.java) ?: continue
                    if (recorrido.estado == "en_proceso") {
                        if (recorrido.id.isEmpty()) {
                            recorrido.id = recorridoSnap.key ?: continue
                        }
                        listaRecorridos.add(recorrido)
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
                    Toast.makeText(this, "Perfil en desarrollo", Toast.LENGTH_SHORT).show()
                    false
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
