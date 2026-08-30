package com.example.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Information regarding a cloned application inside the virtual sandbox.
 */
data class ClonedAppInfo(
    @SerializedName("packageName")
    val packageName: String,
    
    @SerializedName("appName")
    val appName: String,
    
    @SerializedName("assignedProfileId")
    val assignedProfileId: String? = null,
    
    @SerializedName("isRunning")
    val isRunning: Boolean = false,
    
    @SerializedName("cloneTime")
    val cloneTime: Long = System.currentTimeMillis(),
    
    @SerializedName("launchCount")
    val launchCount: Int = 0,
    
    @SerializedName("lastLaunchedTime")
    val lastLaunchedTime: Long = 0,
    
    @SerializedName("assignedProfileName")
    val assignedProfileName: String? = null
)
