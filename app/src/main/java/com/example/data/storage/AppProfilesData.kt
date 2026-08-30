package com.example.data.storage

import com.example.domain.model.AppSettings
import com.example.domain.model.SpoofProfile
import com.google.gson.annotations.SerializedName

/**
 * Top-level structure for app_profiles.json
 */
data class AppProfilesData(
    @SerializedName("profiles")
    val profiles: List<SpoofProfile> = emptyList(),
    
    @SerializedName("appAssignments")
    val appAssignments: Map<String, String> = emptyMap(),
    
    @SerializedName("settings")
    val settings: AppSettings = AppSettings()
)
