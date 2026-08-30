package com.example.domain.model

import android.graphics.drawable.Drawable

/**
 * Information regarding an installed host package available for cloning.
 */
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val isCloned: Boolean = false,
    val assignedProfileName: String? = null,
    val icon: Drawable? = null
)
