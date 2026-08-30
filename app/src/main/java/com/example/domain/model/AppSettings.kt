package com.example.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Global sandbox and application settings.
 */
data class AppSettings(
    @SerializedName("autoRandomizeOnClone")
    val autoRandomizeOnClone: Boolean = true,
    
    @SerializedName("showSystemApps")
    val showSystemApps: Boolean = false,
    
    @SerializedName("keepOriginalIcon")
    val keepOriginalIcon: Boolean = true,
    
    @SerializedName("lsposedActive")
    val lsposedActive: Boolean = false
)
