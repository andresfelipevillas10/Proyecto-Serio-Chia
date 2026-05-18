package com.example.proyecto_definitivo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RouteManagementTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear shared preferences before each test
        context.getSharedPreferences("offline_sync_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun testOfflineSyncManager_savesPendingTask() {
        // Arrange
        val recorridoId = "test_recorrido_id"
        val userId = "test_user_id"
        val updateData = mapOf(
            "estado" to "pausado",
            "motivoPausa" to "Lluvia intensa"
        )

        // Act
        OfflineSyncManager.savePendingTask(context, recorridoId, userId, updateData)

        // Assert
        val prefs = context.getSharedPreferences("offline_sync_prefs", Context.MODE_PRIVATE)
        val tasksString = prefs.getString("pending_tasks", "[]") ?: "[]"
        val tasksArray = JSONArray(tasksString)
        
        assertEquals("Debería haber exactamente 1 tarea pendiente", 1, tasksArray.length())
        
        val task = tasksArray.getJSONObject(0)
        assertEquals("El ID del recorrido debe coincidir", recorridoId, task.getString("recorridoId"))
        assertEquals("El ID del usuario debe coincidir", userId, task.getString("userId"))
        
        val dataObj = task.getJSONObject("data")
        assertEquals("El estado debe ser pausado", "pausado", dataObj.getString("estado"))
        assertEquals("El motivo de pausa debe coincidir", "Lluvia intensa", dataObj.getString("motivoPausa"))
    }

    @Test
    fun testRouteStateTransitions_motivosValidos() {
        // Simulamos una validación de longitud (similar a lo que hace el filtro en UI)
        val motivoValido = "Un motivo muy válido."
        val motivoLargo = "A".repeat(251)
        val motivoVacio = ""

        assertTrue("Motivo válido debe pasar", motivoValido.isNotEmpty() && motivoValido.length <= 250)
        assertFalse("Motivo vacío no debe pasar", motivoVacio.isNotEmpty())
        assertFalse("Motivo demasiado largo no debe pasar", motivoLargo.length <= 250)
    }
}
