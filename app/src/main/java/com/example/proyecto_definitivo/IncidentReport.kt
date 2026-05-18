package com.example.proyecto_definitivo

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// 🎸 LOS RIFFS: ¿Qué salió mal en la carretera?
enum class IncidentType {
    ACCIDENTE,      // Choques, raspadas, emergencias graves
    MECANICO,       // Motor fundido, llanta pinchada
    MEDICO,         // Conductor o pasajero indispuesto
    TRAFICO_PESADO, // Vía bloqueada, manifestación
    OTRO            // Cualquier otra rareza
}

// 🔊 EL VOLUMEN: Nivel de urgencia
enum class Priority {
    ALTA,   // Emergencia inmediata (Requiere 112 / Policía / Ambulancia)
    MEDIA,  // El bus no puede continuar, pero todos están a salvo
    BAJA    // Retraso menor, reporte informativo
}

// 📝 LA PARTITURA: Nuestro modelo de datos y tabla local
@Entity(tableName = "incident_reports")
data class IncidentReport(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // ID único inquebrantable

    // Identificación
    val driverId: String, // El UID del conductor de Firebase Auth

    // GPS y Tiempo (El núcleo de la evidencia)
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,

    // Clasificación
    val type: IncidentType,
    val priority: Priority,

    // Detalles adicionales
    val description: String = "", // "Notes from the road" (Opcional)
    val photoUrl: String? = null, // "Visual Lies" (Para el Step 6 de CameraX)

    // Estado de red (Crucial para el Bunker)
    val isSynced: Boolean = false // false = Guardado local, true = Subido a Firestore
)