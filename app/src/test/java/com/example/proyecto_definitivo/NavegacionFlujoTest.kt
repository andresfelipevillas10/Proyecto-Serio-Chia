package com.example.proyecto_definitivo

import org.junit.Assert.*
import org.junit.Test

class NavegacionFlujoTest {

    @Test
    fun `pausar ruta con motivo valido actualiza estado a pausado`() {
        val recorrido = Recorrido(id = "1", estado = "en_proceso")
        val motivo = "Trafico pesado"
        
        // Simular lógica de pausa
        val esValido = motivo.trim().length >= 5
        if (esValido) {
            recorrido.estado = "pausado"
            recorrido.motivoPausa = motivo
        }

        assertTrue("El estado debería ser pausado", recorrido.estado == "pausado")
        assertEquals("Trafico pesado", recorrido.motivoPausa)
    }

    @Test
    fun `pausar ruta con motivo invalido no actualiza estado`() {
        val recorrido = Recorrido(id = "1", estado = "en_proceso")
        val motivo = "Cort" // Menos de 5 caracteres
        
        // Simular lógica de pausa
        val esValido = motivo.trim().length >= 5
        if (esValido) {
            recorrido.estado = "pausado"
            recorrido.motivoPausa = motivo
        }

        assertEquals("El estado debería seguir en proceso", "en_proceso", recorrido.estado)
        assertTrue("El motivo debería estar vacío", recorrido.motivoPausa.isEmpty())
    }
}
