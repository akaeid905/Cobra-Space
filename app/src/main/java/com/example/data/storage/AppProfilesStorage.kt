package com.example.data.storage

import android.content.Context
import android.util.Log
import com.example.data.PresetProfiles
import com.example.domain.model.AppSettings
import com.example.domain.model.SpoofProfile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles reading, writing, and backup of app_profiles.json from internal storage.
 */
class AppProfilesStorage(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val profilesFile: File
        get() = File(context.filesDir, FILE_NAME)

    companion object {
        private const val TAG = "AppProfilesStorage"
        const val FILE_NAME = "app_profiles.json"
    }

    /**
     * Loads the stored profiles, assignments, and settings from app_profiles.json.
     * If file doesn't exist, initializes with default preset profiles.
     */
    suspend fun loadData(): AppProfilesData = withContext(Dispatchers.IO) {
        try {
            if (!profilesFile.exists()) {
                val initialData = AppProfilesData(
                    profiles = PresetProfiles.ALL_PRESETS,
                    appAssignments = emptyMap(),
                    settings = AppSettings()
                )
                saveData(initialData)
                return@withContext initialData
            }

            val json = profilesFile.readText()
            val parsed = gson.fromJson(json, AppProfilesData::class.java)
            if (parsed != null && parsed.profiles.isNotEmpty()) {
                parsed
            } else {
                val fallback = AppProfilesData(
                    profiles = PresetProfiles.ALL_PRESETS,
                    appAssignments = parsed?.appAssignments ?: emptyMap(),
                    settings = parsed?.settings ?: AppSettings()
                )
                saveData(fallback)
                fallback
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading $FILE_NAME: ${e.message}", e)
            AppProfilesData(
                profiles = PresetProfiles.ALL_PRESETS,
                appAssignments = emptyMap(),
                settings = AppSettings()
            )
        }
    }

    /**
     * Saves AppProfilesData to internal storage file.
     */
    suspend fun saveData(data: AppProfilesData): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(data)
            profilesFile.writeText(json)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving $FILE_NAME: ${e.message}", e)
            false
        }
    }

    /**
     * Exports full database as JSON string.
     */
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val data = loadData()
        gson.toJson(data)
    }

    /**
     * Imports and overwrites or merges database from JSON string.
     */
    suspend fun importFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val parsed = gson.fromJson(jsonString, AppProfilesData::class.java)
            if (parsed != null) {
                saveData(parsed)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing JSON: ${e.message}", e)
            false
        }
    }

    /**
     * Clears all saved data and re-initializes presets.
     */
    suspend fun clearAll(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (profilesFile.exists()) {
                profilesFile.delete()
            }
            val freshData = AppProfilesData(
                profiles = PresetProfiles.ALL_PRESETS,
                appAssignments = emptyMap(),
                settings = AppSettings()
            )
            saveData(freshData)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting data: ${e.message}", e)
            false
        }
    }
}
