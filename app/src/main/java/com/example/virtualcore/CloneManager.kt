package com.example.virtualcore

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.domain.model.ClonedAppInfo
import com.example.domain.model.SpoofProfile
import com.example.spoofing.ProfileManager
import com.example.spoofing.SpoofEngine
import com.lody.virtual.client.core.VirtualCore
import kotlinx.coroutines.flow.StateFlow

/**
 * High-level orchestration manager coordinating VirtualCore and Device Profile Spoofing.
 */
class CloneManager(
    private val context: Context,
    private val profileManager: ProfileManager,
    private val spoofEngine: SpoofEngine
) {

    private val virtualCore = VirtualCore.get()

    companion object {
        private const val TAG = "CloneManager"
    }

    val clonedApps: StateFlow<List<ClonedAppInfo>> = virtualCore.clonedAppsFlow

    /**
     * Clones an installed app into the sandbox, optionally assigning or auto-randomizing a profile.
     */
    suspend fun cloneApp(packageName: String, specificProfileId: String? = null): Boolean {
        if (virtualCore.isAppInstalled(packageName)) {
            Log.w(TAG, "App $packageName is already cloned")
            return false
        }

        val settings = profileManager.settings.value
        val profileToAssign: SpoofProfile = if (specificProfileId != null) {
            profileManager.getProfileById(specificProfileId)
                ?: profileManager.getAllProfiles().firstOrNull()
                ?: profileManager.generateNewRandomProfile()
        } else if (settings.autoRandomizeOnClone) {
            profileManager.generateNewRandomProfile()
        } else {
            profileManager.getAllProfiles().firstOrNull()
                ?: profileManager.generateNewRandomProfile()
        }

        val success = virtualCore.installPackage(
            packageName = packageName,
            profileId = profileToAssign.id,
            profileName = profileToAssign.profileName
        )

        if (success) {
            profileManager.assignProfileToApp(packageName, profileToAssign.id)
        }

        return success
    }

    /**
     * Prepares spoofed identity and launches the cloned app in virtual sandbox.
     */
    fun launchClonedApp(packageName: String): Boolean {
        if (!virtualCore.isAppInstalled(packageName)) {
            Log.e(TAG, "Cannot launch: $packageName is not cloned")
            return false
        }

        // Apply device identity spoofing hooks
        spoofEngine.prepareAppIdentity(packageName)

        // Launch in sandbox
        val launched = virtualCore.launchApp(context, packageName)
        if (launched) {
            // Start background service to maintain notification & sandbox lifecycle
            try {
                val serviceIntent = Intent(context, VirtualEngineService::class.java).apply {
                    putExtra("action", "APP_LAUNCHED")
                    putExtra("packageName", packageName)
                }
                context.startService(serviceIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not start engine service: ${e.message}")
            }
        }
        return launched
    }

    /**
     * Uninstalls cloned app from sandbox and removes its profile assignment.
     */
    suspend fun uninstallClonedApp(packageName: String): Boolean {
        val success = virtualCore.uninstallPackage(packageName)
        if (success) {
            profileManager.removeAppAssignment(packageName)
        }
        return success
    }

    /**
     * Assigns or updates the spoofed profile for an existing cloned app.
     */
    suspend fun assignProfile(packageName: String, profile: SpoofProfile) {
        profileManager.assignProfileToApp(packageName, profile.id)
        virtualCore.updateAppProfile(packageName, profile.id, profile.profileName)
    }

    /**
     * Bulk assigns a profile to multiple cloned apps.
     */
    suspend fun bulkAssignProfile(packageNames: List<String>, profile: SpoofProfile) {
        profileManager.bulkAssignProfile(packageNames, profile.id)
        for (pkg in packageNames) {
            virtualCore.updateAppProfile(pkg, profile.id, profile.profileName)
        }
    }

    /**
     * Force stops a running cloned app.
     */
    fun stopClonedApp(packageName: String): Boolean {
        return virtualCore.killApp(packageName)
    }

    /**
     * Clears all sandbox clones.
     */
    suspend fun clearAll() {
        virtualCore.clearAllClones()
        profileManager.clearAllData()
    }
}
