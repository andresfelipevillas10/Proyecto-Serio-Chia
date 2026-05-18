package com.example.proyecto_definitivo

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestiona los atajos de rutas frecuentes del conductor.
 *
 * Combina dos fuentes:
 * - **Rutas fijadas manualmente** (★): el conductor las ancla desde la lista de rutas.
 *   Se almacenan en SharedPreferences con un máximo de 3.
 * - **Rutas automáticas por uso**: ordenadas por `usoCount` descendente.
 *   Se complementan con las fijadas hasta completar un máximo de 5 atajos visibles.
 *
 * @param context Contexto de la aplicación para acceder a SharedPreferences.
 */
object FrecuentesManager {

    private const val PREFS_NAME = "frecuentes_prefs"
    private const val KEY_PINNED_IDS = "pinned_route_ids"
    private const val KEY_PINNED_DATA = "pinned_route_data"
    private const val MAX_PINNED = 3
    private const val MAX_TOTAL_SHORTCUTS = 5

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Fija una ruta como atajo frecuente. Máximo [MAX_PINNED] rutas fijadas.
     *
     * @param context Contexto de la aplicación.
     * @param ruta La ruta a fijar.
     * @return `true` si se fijó correctamente, `false` si ya se alcanzó el límite.
     */
    fun pinRoute(context: Context, ruta: Ruta): Boolean {
        val prefs = getPrefs(context)
        val currentIds = getPinnedIds(prefs).toMutableSet()

        if (currentIds.contains(ruta.id)) return true // Ya está fijada
        if (currentIds.size >= MAX_PINNED) return false // Límite alcanzado

        currentIds.add(ruta.id)
        prefs.edit().putStringSet(KEY_PINNED_IDS, currentIds).apply()

        // Guardar datos de la ruta para renderizar sin Firebase
        val dataStr = prefs.getString(KEY_PINNED_DATA, "") ?: ""
        val newEntry = "${ruta.id}|${ruta.nombre}|${ruta.radioDeteccion}|${ruta.descripcion}"
        val updated = if (dataStr.isEmpty()) newEntry else "$dataStr;;$newEntry"
        prefs.edit().putString(KEY_PINNED_DATA, updated).apply()

        return true
    }

    /**
     * Elimina una ruta fijada manualmente.
     *
     * @param context Contexto de la aplicación.
     * @param rutaId El ID de la ruta a desfijar.
     */
    fun unpinRoute(context: Context, rutaId: String) {
        val prefs = getPrefs(context)
        val currentIds = getPinnedIds(prefs).toMutableSet()
        currentIds.remove(rutaId)
        prefs.edit().putStringSet(KEY_PINNED_IDS, currentIds).apply()

        // Remover de los datos serializados
        val dataStr = prefs.getString(KEY_PINNED_DATA, "") ?: ""
        val entries = dataStr.split(";;").filter { !it.startsWith("$rutaId|") }
        prefs.edit().putString(KEY_PINNED_DATA, entries.joinToString(";;")).apply()
    }

    /**
     * Verifica si una ruta está fijada manualmente.
     */
    fun isPinned(context: Context, rutaId: String): Boolean {
        return getPinnedIds(getPrefs(context)).contains(rutaId)
    }

    /**
     * Obtiene las rutas fijadas manualmente desde SharedPreferences.
     * No requiere acceso a Firebase.
     */
    fun getPinnedRoutes(context: Context): List<Ruta> {
        val prefs = getPrefs(context)
        val dataStr = prefs.getString(KEY_PINNED_DATA, "") ?: ""
        if (dataStr.isEmpty()) return emptyList()

        return dataStr.split(";;").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 4) {
                Ruta(
                    id = parts[0],
                    nombre = parts[1],
                    radioDeteccion = parts[2].toFloatOrNull() ?: 30f,
                    descripcion = parts[3]
                )
            } else null
        }
    }

    /**
     * Combina rutas fijadas + automáticas por uso para generar la lista final de atajos.
     * Las fijadas aparecen primero, seguidas de las más usadas (sin duplicados),
     * hasta un máximo de [MAX_TOTAL_SHORTCUTS].
     *
     * @param context Contexto de la aplicación.
     * @param allRoutes Todas las rutas del conductor (desde Firebase).
     * @return Lista combinada de atajos.
     */
    fun getShortcuts(context: Context, allRoutes: List<Ruta>): List<ShortcutItem> {
        val pinnedIds = getPinnedIds(getPrefs(context))
        val pinnedRoutes = getPinnedRoutes(context)
        val result = mutableListOf<ShortcutItem>()

        // 1. Rutas fijadas primero
        for (pinned in pinnedRoutes) {
            val freshData = allRoutes.find { it.id == pinned.id } ?: pinned
            result.add(ShortcutItem(ruta = freshData, isPinned = true))
        }

        // 2. Complementar con automáticas por usoCount
        val autoRoutes = allRoutes
            .filter { route -> !pinnedIds.contains(route.id) && route.usoCount > 0 }
            .sortedByDescending { it.usoCount }

        for (auto in autoRoutes) {
            if (result.size >= MAX_TOTAL_SHORTCUTS) break
            result.add(ShortcutItem(ruta = auto, isPinned = false))
        }

        return result
    }

    private fun getPinnedIds(prefs: SharedPreferences): Set<String> {
        return prefs.getStringSet(KEY_PINNED_IDS, emptySet()) ?: emptySet()
    }

    /**
     * Modelo de un atajo visible en el Home.
     */
    data class ShortcutItem(
        val ruta: Ruta,
        val isPinned: Boolean
    )
}
