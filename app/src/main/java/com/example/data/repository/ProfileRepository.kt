package com.example.data.repository

import com.example.domain.model.AppSettings
import com.example.domain.model.SpoofProfile
import com.example.spoofing.ProfileManager
import kotlinx.coroutines.flow.StateFlow

interface ProfileRepository {
    val profiles: StateFlow<List<SpoofProfile>>
    val appAssignments: StateFlow<Map<String, String>>
    val settings: StateFlow<AppSettings>

    suspend fun saveProfile(profile: SpoofProfile)
    suspend fun deleteProfile(id: String)
    suspend fun duplicateProfile(profile: SpoofProfile): SpoofProfile
    suspend fun generateRandomProfile(customName: String? = null): SpoofProfile
    suspend fun assignProfileToApp(packageName: String, profileId: String)
    suspend fun bulkAssignProfile(packageNames: List<String>, profileId: String)
    suspend fun updateSettings(settings: AppSettings)
    suspend fun exportJson(): String
    suspend fun importJson(json: String): Boolean
    suspend fun clearAll(): Boolean
}

class ProfileRepositoryImpl(
    private val profileManager: ProfileManager
) : ProfileRepository {

    override val profiles: StateFlow<List<SpoofProfile>> = profileManager.profiles
    override val appAssignments: StateFlow<Map<String, String>> = profileManager.appAssignments
    override val settings: StateFlow<AppSettings> = profileManager.settings

    override suspend fun saveProfile(profile: SpoofProfile) {
        profileManager.saveProfile(profile)
    }

    override suspend fun deleteProfile(id: String) {
        profileManager.deleteProfile(id)
    }

    override suspend fun duplicateProfile(profile: SpoofProfile): SpoofProfile {
        return profileManager.duplicateProfile(original = profile)
    }

    override suspend fun generateRandomProfile(customName: String?): SpoofProfile {
        return profileManager.generateNewRandomProfile(customName)
    }

    override suspend fun assignProfileToApp(packageName: String, profileId: String) {
        profileManager.assignProfileToApp(packageName, profileId)
    }

    override suspend fun bulkAssignProfile(packageNames: List<String>, profileId: String) {
        profileManager.bulkAssignProfile(packageNames, profileId)
    }

    override suspend fun updateSettings(settings: AppSettings) {
        profileManager.updateSettings(settings)
    }

    override suspend fun exportJson(): String {
        return profileManager.exportAllProfilesJson()
    }

    override suspend fun importJson(json: String): Boolean {
        return profileManager.importBackupJson(json)
    }

    override suspend fun clearAll(): Boolean {
        return profileManager.clearAllData()
    }
}
