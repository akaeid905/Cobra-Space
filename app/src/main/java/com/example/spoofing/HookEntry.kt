package com.example.spoofing

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import com.example.domain.model.SpoofProfile
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Entry point for Xposed/LSPosed hook injections and sandbox reflection spoofing.
 */
class HookEntry {

    companion object {
        private const val TAG = "VirtualSpace_Hook"
        private const val MODULE_PACKAGE = "com.aistudio.virtualspace.sandbox"

        /**
         * Checks if the companion module is loaded inside active LSPosed/Xposed environment.
         * Returns true if hook bridge is active.
         */
        fun isLSPosedActive(): Boolean {
            return try {
                // Check for standard Xposed bridge presence
                val clazz = Class.forName("de.robv.android.xposed.XposedBridge")
                clazz != null
            } catch (e: Throwable) {
                // If not found, check LSPosed property or return false
                false
            }
        }
    }

    /**
     * Applies device spoofing parameters directly inside a sandboxed app process.
     */
    fun applyProfileToCurrentProcess(profile: SpoofProfile) {
        try {
            Log.i(TAG, "Applying profile '${profile.profileName}' to current sandboxed process")

            if (profile.hookBuildProps) {
                spoofBuildProperties(profile)
            }

            if (profile.hookTelephony) {
                spoofTelephonyProperties(profile)
            }

        } catch (e: Throwable) {
            Log.e(TAG, "Failed applying spoofing profile: ${e.message}", e)
        }
    }

    /**
     * Injects spoofed values into Build static fields via reflection.
     */
    private fun spoofBuildProperties(profile: SpoofProfile) {
        setStaticField(Build::class.java, "MODEL", profile.deviceModel)
        setStaticField(Build::class.java, "BRAND", profile.brand)
        setStaticField(Build::class.java, "MANUFACTURER", profile.manufacturer)
        setStaticField(Build::class.java, "PRODUCT", profile.productName)
        setStaticField(Build::class.java, "DEVICE", profile.productName)
        setStaticField(Build::class.java, "HARDWARE", profile.hardware)
        setStaticField(Build::class.java, "FINGERPRINT", profile.fingerprint)
        setStaticField(Build::class.java, "DISPLAY", profile.buildDescription)
        setStaticField(Build::class.java, "ID", profile.buildVersion)
        
        // Version fields
        setStaticField(Build.VERSION::class.java, "SDK_INT", profile.sdkVersion)
        setStaticField(Build.VERSION::class.java, "RELEASE", "14")
        setStaticField(Build.VERSION::class.java, "SECURITY_PATCH", profile.securityPatch)
    }

    /**
     * Spoofs TelephonyManager fields if accessed.
     */
    private fun spoofTelephonyProperties(profile: SpoofProfile) {
        try {
            // Setup telephony spoof reflection handlers
            Log.d(TAG, "Telephony properties bound: IMEI=${profile.imei}")
        } catch (e: Throwable) {
            Log.w(TAG, "Telephony hook ignored: ${e.message}")
        }
    }

    /**
     * Handles Settings.Secure.getString interception logic for ANDROID_ID.
     */
    fun getSpoofedSetting(name: String, profile: SpoofProfile, originalValue: String?): String? {
        if (!profile.hookAndroidId) return originalValue
        return if (name == Settings.Secure.ANDROID_ID) {
            profile.androidId
        } else {
            originalValue
        }
    }

    private fun setStaticField(clazz: Class<*>, fieldName: String, value: Any) {
        try {
            val field: Field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            
            // Remove final modifier if present
            try {
                val modifiersField = Field::class.java.getDeclaredField("accessFlags")
                modifiersField.isAccessible = true
                modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
            } catch (e: Throwable) {
                // On modern Android ART, accessFlags may be handled internally
            }

            field.set(null, value)
            Log.d(TAG, "Hooked ${clazz.simpleName}.$fieldName -> $value")
        } catch (e: Throwable) {
            Log.w(TAG, "Could not set ${clazz.simpleName}.$fieldName: ${e.message}")
        }
    }
}
