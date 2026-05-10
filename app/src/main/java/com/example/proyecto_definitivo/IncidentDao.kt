package com.example.proyecto_definitivo

import androidx.room.*

@Dao
interface IncidentDao {
    // Inserta un nuevo reporte o lo actualiza si el ID ya existe
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: IncidentReport): Long

    // Historial completo de la gira (Tours)
    @Query("SELECT * FROM incident_reports ORDER BY timestamp DESC")
    suspend fun getAllReports(): List<IncidentReport>

    // CRUCIAL PARA EL STEP 3: Busca los reportes huérfanos sin internet
    @Query("SELECT * FROM incident_reports WHERE isSynced = 0")
    suspend fun getUnsyncedReports(): List<IncidentReport>

    // Marca el reporte como subido una vez que Firebase confirme la recepción
    @Query("UPDATE incident_reports SET isSynced = 1 WHERE id = :reportId")
    suspend fun markAsSynced(reportId: String): Int

    @Delete
    suspend fun deleteReport(report: IncidentReport)
}