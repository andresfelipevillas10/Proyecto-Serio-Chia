package com.example.proyecto_definitivo

data class Ruta( var id: String = "",
                 var userId: String = "",
                 var nombre :String = "",
                 var descripcion: String="",
                 var activa:Boolean =true,
                 var radioDeteccion:Float=30f,
                 var creadaEn: Long = System.currentTimeMillis(),
                 var horaSalida: String = "",
                 var horaLlegada: String = "",
                 var usoCount: Int = 0,

                 var puntos: Map<String, PuntoRuta>? = null
)
