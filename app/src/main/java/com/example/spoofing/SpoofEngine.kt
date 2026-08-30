package com.example.spoofing

import android.content.Context
import android.util.Log
import com.example.domain.model.SpoofProfile

/**
 * Core engine coordinating device identity spoofing and sandbox profile injection.
 */
class SpoofEngine(
    private val context: Context,
    private val profileManager: ProfileManager
) {

    private val hookEntry = HookEntry()

    companion object {
        private const val TAG = "SpoofEngine"
    }

    /**
     * Checks if LSPosed framework is installed and operational.
     */
    fun isFrameworkActive(): Boolean {
        return HookEntry.isLSPosedActive()
    }

    /**
     * Prepares and applies identity spoofing for an app prior to launching in sandbox.
     */
    fun prepareAppIdentity(packageName: String): SpoofProfile {
        var profile = profileManager.getAssignedProfileForApp(packageName)
        if (profile == null) {
            Log.w(TAG, "No profile assigned for $packageName, generating fallback randomized identity")
            profile = RandomIdGenerator.generateAndroidId().let {
                com.example.data.PresetProfiles.PIXEL_9_PRO.copy(
                    androidId = it,
                    profileName = "Auto-Assigned Sandbox Profile"
                )
            }
        }

        // Apply hook engine in sandbox context
        hookEntry.applyProfileToCurrentProcess(profile)
        return profile
    }

    /**
     * Retrieves spoofed Android ID for a given package.
     */
    fun getEffectiveAndroidId(packageName: String, defaultId: String): String {
        val profile = profileManager.getAssignedProfileForApp(packageName) ?: return defaultId
        return if (profile.hookAndroidId) profile.androidId else defaultId
    }
}
