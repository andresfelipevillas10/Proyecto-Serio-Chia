package com.example.proyecto_definitivo

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

class IncidentSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = ZendaDatabase.getDatabase(applicationContext)
        val firestore = FirebaseFirestore.getInstance()
        val storageRef = FirebaseStorage.getInstance().reference

        // 1. Extraer del Búnker
        val unsyncedReports = db.incidentDao().getUnsyncedReports()

        if (unsyncedReports.isEmpty()) return Result.success()

        return try {
            for (report in unsyncedReports) {
                var finalPhotoUrl: String? = null

                // 2. ¿Hay Evidencia Visual? Subir a Firebase Storage primero
                if (!report.photoUrl.isNullOrEmpty()) {
                    val localFile = File(report.photoUrl)
                    if (localFile.exists()) {
                        val fileUri = Uri.fromFile(localFile)
                        // Creamos una carpeta "incident_evidence" y guardamos la foto con el ID del reporte
                        val imageRef = storageRef.child("incident_evidence/${report.id}.jpg")

                        // Subimos la imagen y esperamos a que termine (.await)
                        imageRef.putFile(fileUri).await()
                        // Obtenemos el link público de descarga
                        finalPhotoUrl = imageRef.downloadUrl.await().toString()
                    }
                }

                // 3. Preparar el mapa de datos para Firestore (Evita errores de clases Room)
                val reportMap = hashMapOf(
                    "id" to report.id,
                    "driverId" to report.driverId,
                    "type" to report.type.name, // Asegúrate de que el enum se guarde como texto
                    "priority" to report.priority.name,
                    "latitude" to report.latitude,
                    "longitude" to report.longitude,
                    "timestamp" to report.timestamp,
                    "description" to report.description,
                    "photoUrl" to finalPhotoUrl // Si no hubo foto, se guarda como null. Si hubo, guarda el link de Firebase.
                )

                // 4. Subir a Firestore (El Rayo)
                firestore.collection("incident_reports")
                    .document(report.id)
                    .set(reportMap)
                    .await() // Espera la confirmación de Firebase

                // 5. Marcar como sincronizado localmente
                db.incidentDao().markAsSynced(report.id)
            }
            Log.d("SYNC_WORKER", "⚡ Lightning strike: ${unsyncedReports.size} reportes sincronizados.")
            Result.success()
        } catch (e: Exception) {
            Log.e("SYNC_WORKER", "Falla en la Matrix. El worker reintentará automáticamente.", e)
            Result.retry() // Vital: Si falla por red, WorkManager lo vuelve a intentar más tarde
        }
    }
}