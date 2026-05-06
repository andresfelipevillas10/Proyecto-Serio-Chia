package com.example.proyecto_definitivo

data class Recorrido(
    var id:String = "",
    var rutaId:String = "",
    var rutaNombre :String = "",
    var usuarioId: String = "",
    var inicioTiempo : Long =0L,
    var finTiempo:Long=0L,
    var tiempoTotalMs: Long=0L,
    var latitudActual: Double = 0.0,
    var longitudActual: Double = 0.0,
    var estado:String= "en_proceso",
    var motivoPausa: String = "",
    var timestampPausa: Long = 0L,
    var userIdPausa: String = "",
    var motivoTermino: String = ""
)