package com.example.proyecto_definitivo

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Si agregamos más tablas en el futuro, se incluyen en el array "entities"
@Database(entities = [IncidentReport::class], version = 1, exportSchema = false)
abstract class ZendaDatabase : RoomDatabase() {

    abstract fun incidentDao(): IncidentDao

    companion object {
        @Volatile
        private var INSTANCE: ZendaDatabase? = null

        fun getDatabase(context: Context): ZendaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZendaDatabase::class.java,
                    "zenda_bunker_db" // El nombre de nuestro archivo SQLite
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}