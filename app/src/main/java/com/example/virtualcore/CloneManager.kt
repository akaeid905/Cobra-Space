package com.example.virtualcore

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.domain.model.ClonedAppInfo
import com.example.domain.model.SpoofProfile
import com.example.spoofing.ProfileManager
import com.example.spoofing.SpoofEngine
import com.lody.virtual.client.core.VirtualCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
     * Prepares fresh spoofed identity (with a newly generated Android ID every time)
     * and launches the cloned app in virtual sandbox.
     */
    fun launchClonedApp(packageName: String): Boolean {
        if (!virtualCore.isAppInstalled(packageName)) {
            Log.e(TAG, "Cannot launch: $packageName is not cloned")
            return false
        }

        // Always generate a fresh, new Android ID & spoof identifiers every time the app is opened
        val freshAndroidId = com.example.spoofing.RandomIdGenerator.generateAndroidId()
        val freshImei = com.example.spoofing.RandomIdGenerator.generateImei()
        val freshGsfId = com.example.spoofing.RandomIdGenerator.generateGsfId()
        val freshMac = com.example.spoofing.RandomIdGenerator.generateMacAddress()

        val existingProfile = profileManager.getAssignedProfileForApp(packageName)
        val profile = (existingProfile ?: com.example.data.PresetProfiles.PIXEL_9_PRO).copy(
            androidId = freshAndroidId,
            imei = freshImei,
            gsfId = freshGsfId,
            macAddress = freshMac
        )

        // Save updated profile with new Android ID
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            profileManager.saveProfile(profile)
            profileManager.assignProfileToApp(packageName, profile.id)
        }
        virtualCore.updateAppProfile(packageName, profile.id, profile.profileName)

        // Apply device identity spoofing hooks
        spoofEngine.prepareAppIdentity(packageName)

        // Launch in isolated multi-process Sandbox Container Activity
        try {
            val intent = Intent(context, VirtualSandboxContainerActivity::class.java).apply {
                putExtra(VirtualSandboxContainerActivity.EXTRA_PACKAGE_NAME, packageName)
                putExtra(VirtualSandboxContainerActivity.EXTRA_PROFILE_ID, profile.id)
                putExtra(VirtualSandboxContainerActivity.EXTRA_PROFILE_NAME, profile.profileName)
                putExtra(VirtualSandboxContainerActivity.EXTRA_ANDROID_ID, profile.androidId)
                putExtra(VirtualSandboxContainerActivity.EXTRA_DEVICE_MODEL, profile.deviceModel)
                putExtra(VirtualSandboxContainerActivity.EXTRA_BRAND, profile.brand)
                putExtra(VirtualSandboxContainerActivity.EXTRA_FINGERPRINT, profile.fingerprint)
                putExtra(VirtualSandboxContainerActivity.EXTRA_IMEI, profile.imei)
                putExtra(VirtualSandboxContainerActivity.EXTRA_GSF_ID, profile.gsfId)
                putExtra(VirtualSandboxContainerActivity.EXTRA_MAC, profile.macAddress)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            virtualCore.markAppRunning(packageName, true)

            // Start background service to maintain notification & sandbox lifecycle
            try {
                val serviceIntent = Intent(context, VirtualEngineService::class.java).apply {
                    putExtra("action", "APP_LAUNCHED")
                    putExtra("packageName", packageName)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Could not start engine service: ${e.message}")
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch sandbox container: ${e.message}", e)
            return false
        }
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
