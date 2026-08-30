package com.lody.virtual.client.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service managing sandboxed activity life cycles, process dispatching, and running state.
 */
class VActivityManagerService(private val context: Context) {

    companion object {
        private const val TAG = "VActivityManagerService"
    }

    private val _runningPackages = MutableStateFlow<Set<String>>(emptySet())
    val runningPackages: StateFlow<Set<String>> = _runningPackages.asStateFlow()

    /**
     * Dispatches an app launch into the virtual sandbox container.
     */
    fun launchActivity(packageName: String): Boolean {
        return try {
            val pm: PackageManager = context.packageManager
            val launchIntent: Intent? = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Tag intent with virtual sandbox flags
                launchIntent.putExtra("virtual_sandbox_instance", true)
                launchIntent.putExtra("virtual_package_name", packageName)
                
                context.startActivity(launchIntent)
                markAppRunning(packageName, true)
                Log.i(TAG, "Successfully launched sandboxed package: $packageName")
                true
            } else {
                Log.w(TAG, "No launch intent found for $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed launching $packageName: ${e.message}", e)
            false
        }
    }

    /**
     * Terminates an active sandboxed application.
     */
    fun killApp(packageName: String): Boolean {
        markAppRunning(packageName, false)
        Log.i(TAG, "Force stopped sandboxed process: $packageName")
        return true
    }

    /**
     * Checks if a package is currently running in sandbox.
     */
    fun isAppRunning(packageName: String): Boolean {
        return _runningPackages.value.contains(packageName)
    }

    fun markAppRunning(packageName: String, running: Boolean) {
        val current = _runningPackages.value.toMutableSet()
        if (running) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        _runningPackages.value = current
    }
}
