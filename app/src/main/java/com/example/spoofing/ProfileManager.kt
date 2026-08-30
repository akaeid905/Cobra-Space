package com.example.spoofing

import com.example.data.PresetProfiles
import com.example.data.storage.AppProfilesData
import com.example.data.storage.AppProfilesStorage
import com.example.domain.model.AppSettings
import com.example.domain.model.SpoofProfile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages device profiles, validation, presets, and app-to-profile bindings.
 */
class ProfileManager(private val storage: AppProfilesStorage) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val _profiles = MutableStateFlow<List<SpoofProfile>>(emptyList())
    val profiles: StateFlow<List<SpoofProfile>> = _profiles.asStateFlow()

    private val _appAssignments = MutableStateFlow<Map<String, String>>(emptyMap())
    val appAssignments: StateFlow<Map<String, String>> = _appAssignments.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /**
     * Initializes state by loading data from storage.
     */
    suspend fun initialize() {
        val data = storage.loadData()
        _profiles.value = data.profiles
        _appAssignments.value = data.appAssignments
        _settings.value = data.settings
    }

    /**
     * Retrieves all profiles.
     */
    fun getAllProfiles(): List<SpoofProfile> = _profiles.value

    /**
     * Finds a profile by its ID.
     */
    fun getProfileById(id: String): SpoofProfile? {
        return _profiles.value.find { it.id == id }
    }

    /**
     * Saves or updates a device profile.
     */
    suspend fun saveProfile(profile: SpoofProfile) {
        val current = _profiles.value.toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            current[index] = profile
        } else {
            current.add(0, profile)
        }
        _profiles.value = current
        persistData()
    }

    /**
     * Deletes a profile by ID.
     */
    suspend fun deleteProfile(id: String) {
        val current = _profiles.value.filterNot { it.id == id }
        _profiles.value = current
        
        // Remove assignments pointing to deleted profile
        val assignments = _appAssignments.value.toMutableMap()
        val modified = assignments.filterValues { it != id }
        _appAssignments.value = modified

        persistData()
    }

    /**
     * Duplicates an existing profile with a new ID and generated unique Android ID.
     */
    suspend fun duplicateProfile(original: SpoofProfile): SpoofProfile {
        val newAndroidId = RandomIdGenerator.generateAndroidId()
        val newGsfId = RandomIdGenerator.generateGsfId()
        val duplicated = original.copy(
            id = UUID.randomUUID().toString(),
            profileName = "${original.profileName} (Copy)",
            androidId = newAndroidId,
            gsfId = newGsfId,
            isPreset = false,
            createdAt = System.currentTimeMillis()
        )
        saveProfile(duplicated)
        return duplicated
    }

    /**
     * Assigns a profile to a specific cloned package name.
     */
    suspend fun assignProfileToApp(packageName: String, profileId: String) {
        val updated = _appAssignments.value.toMutableMap()
        updated[packageName] = profileId
        _appAssignments.value = updated
        persistData()
    }

    /**
     * Bulk assigns a profile to multiple package names at once.
     */
    suspend fun bulkAssignProfile(packageNames: List<String>, profileId: String) {
        val updated = _appAssignments.value.toMutableMap()
        for (pkg in packageNames) {
            updated[pkg] = profileId
        }
        _appAssignments.value = updated
        persistData()
    }

    /**
     * Removes profile assignment for an uninstalled app.
     */
    suspend fun removeAppAssignment(packageName: String) {
        val updated = _appAssignments.value.toMutableMap()
        updated.remove(packageName)
        _appAssignments.value = updated
        persistData()
    }

    /**
     * Retrieves assigned profile for a specific cloned app package.
     */
    fun getAssignedProfileForApp(packageName: String): SpoofProfile? {
        val profileId = _appAssignments.value[packageName] ?: return null
        return getProfileById(profileId)
    }

    /**
     * Updates sandbox and application settings.
     */
    suspend fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        persistData()
    }

    /**
     * Generates a completely new random profile and saves it.
     */
    suspend fun generateNewRandomProfile(customName: String? = null): SpoofProfile {
        val profile = PresetProfiles.createRandomProfile(customName)
        saveProfile(profile)
        return profile
    }

    /**
     * Exports a single profile to JSON.
     */
    fun exportProfileJson(profile: SpoofProfile): String {
        return gson.toJson(profile)
    }

    /**
     * Imports a single profile from JSON string.
     */
    suspend fun importProfileJson(json: String): SpoofProfile? {
        return try {
            val parsed = gson.fromJson(json, SpoofProfile::class.java)
            if (parsed != null && parsed.isAndroidIdValid()) {
                val newProfile = parsed.copy(id = UUID.randomUUID().toString(), isPreset = false)
                saveProfile(newProfile)
                newProfile
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Exports all database content.
     */
    suspend fun exportAllProfilesJson(): String {
        return storage.exportToJson()
    }

    /**
     * Imports full database backup JSON.
     */
    suspend fun importBackupJson(json: String): Boolean {
        val success = storage.importFromJson(json)
        if (success) {
            initialize()
        }
        return success
    }

    /**
     * Clears all data and re-initializes presets.
     */
    suspend fun clearAllData(): Boolean {
        val success = storage.clearAll()
        if (success) {
            initialize()
        }
        return success
    }

    private suspend fun persistData() {
        val data = AppProfilesData(
            profiles = _profiles.value,
            appAssignments = _appAssignments.value,
            settings = _settings.value
        )
        storage.saveData(data)
    }
}
