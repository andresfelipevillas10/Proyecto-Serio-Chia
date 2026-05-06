package com.example.proyecto_definitivo

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONArray
import org.json.JSONObject

object OfflineSyncManager {
    private const val PREFS_NAME = "offline_sync_prefs"
    private const val KEY_PENDING_TASKS = "pending_tasks"

    fun savePendingTask(context: Context, recorridoId: String, userId: String, updateData: Map<String, Any>) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tasksString = prefs.getString(KEY_PENDING_TASKS, "[]") ?: "[]"
        try {
            val tasksArray = JSONArray(tasksString)
            val newTask = JSONObject()
            newTask.put("recorridoId", recorridoId)
            newTask.put("userId", userId)
            
            val dataObj = JSONObject()
            updateData.forEach { (key, value) ->
                dataObj.put(key, value)
            }
            newTask.put("data", dataObj)
            
            tasksArray.put(newTask)
            prefs.edit().putString(KEY_PENDING_TASKS, tasksArray.toString()).apply()
            Log.d("OfflineSync", "Saved pending task for recorrido: $recorridoId")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncPendingTasks(context: Context) {
        if (!NetworkUtil.isNetworkAvailable(context)) return

        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tasksString = prefs.getString(KEY_PENDING_TASKS, "[]") ?: "[]"
        if (tasksString == "[]") return

        try {
            val tasksArray = JSONArray(tasksString)
            val db = FirebaseDatabase.getInstance().reference

            for (i in 0 until tasksArray.length()) {
                val task = tasksArray.getJSONObject(i)
                val recorridoId = task.getString("recorridoId")
                val userId = task.getString("userId")
                val dataObj = task.getJSONObject("data")
                
                val updateMap = mutableMapOf<String, Any>()
                val keys = dataObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    updateMap[key] = dataObj.get(key)
                }

                db.child("recorridos").child(userId).child(recorridoId).updateChildren(updateMap)
                    .addOnSuccessListener {
                        Log.d("OfflineSync", "Successfully synced task for $recorridoId")
                    }
                    .addOnFailureListener {
                        Log.e("OfflineSync", "Failed to sync task for $recorridoId")
                    }
            }
            // Clear tasks after pushing to Firebase
            prefs.edit().putString(KEY_PENDING_TASKS, "[]").apply()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
