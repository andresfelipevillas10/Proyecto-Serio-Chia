package com.example.proyecto_definitivo

data class User(
    val uid: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val email: String = "",
    val noBus: String = "",     // solo conductor
    val turno: String = "",     // solo conductor
    val rol: String = ""        // "PASAJERO" o "CONDUCTOR"
)