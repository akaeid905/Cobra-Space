package com.example.domain.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Represents a spoofed hardware and device identity profile.
 */
data class SpoofProfile(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    
    @SerializedName("profileName")
    val profileName: String,
    
    @SerializedName("androidId")
    val androidId: String,
    
    @SerializedName("gsfId")
    val gsfId: String,
    
    @SerializedName("deviceModel")
    val deviceModel: String,
    
    @SerializedName("brand")
    val brand: String,
    
    @SerializedName("manufacturer")
    val manufacturer: String,
    
    @SerializedName("fingerprint")
    val fingerprint: String,
    
    @SerializedName("buildVersion")
    val buildVersion: String,
    
    @SerializedName("sdkVersion")
    val sdkVersion: Int = 34,
    
    @SerializedName("securityPatch")
    val securityPatch: String = "2024-01-01",
    
    @SerializedName("productName")
    val productName: String,
    
    @SerializedName("hardware")
    val hardware: String,
    
    @SerializedName("buildDescription")
    val buildDescription: String,
    
    @SerializedName("imei")
    val imei: String = "",
    
    @SerializedName("macAddress")
    val macAddress: String = "",
    
    @SerializedName("isPreset")
    val isPreset: Boolean = false,
    
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @SerializedName("hookAndroidId")
    val hookAndroidId: Boolean = true,
    
    @SerializedName("hookBuildProps")
    val hookBuildProps: Boolean = true,
    
    @SerializedName("hookTelephony")
    val hookTelephony: Boolean = true
) {
    /**
     * Validates if Android ID is a valid 16-character hexadecimal string.
     */
    fun isAndroidIdValid(): Boolean {
        return androidId.length == 16 && androidId.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
    }
}
