package com.lody.virtual.client.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.example.domain.model.ClonedAppInfo
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * VirtualApp VirtualCore virtualization engine singleton.
 */
class VirtualCore private constructor() {

    companion object {
        private const val TAG = "VirtualCore"
        
        @Volatile
        private var instance: VirtualCore? = null

        fun get(): VirtualCore {
            return instance ?: synchronized(this) {
                instance ?: VirtualCore().also { instance = it }
            }
        }
    }

    private var appContext: Context? = null
    private var ioRedirectService: IORedirectService? = null
    private var activityManagerService: VActivityManagerService? = null

    private val installedVirtualApps = ConcurrentHashMap<String, ClonedAppInfo>()
    private val _clonedAppsFlow = MutableStateFlow<List<ClonedAppInfo>>(emptyList())
    val clonedAppsFlow: StateFlow<List<ClonedAppInfo>> = _clonedAppsFlow.asStateFlow()

    private var isInitialized = false

    /**
     * Initializes the VirtualCore virtualization engine.
     */
    fun startup(context: Context) {
        if (isInitialized) return
        this.appContext = context.applicationContext
        this.ioRedirectService = IORedirectService(context.applicationContext)
        this.activityManagerService = VActivityManagerService(context.applicationContext)
        loadStoredClones()
        isInitialized = true
        Log.i(TAG, "VirtualCore virtualization engine initialized successfully.")
    }

    fun getIORedirectService(): IORedirectService {
        return ioRedirectService ?: throw IllegalStateException("VirtualCore not started")
    }

    fun getActivityManagerService(): VActivityManagerService {
        return activityManagerService ?: throw IllegalStateException("VirtualCore not started")
    }

    /**
     * Clones and installs a package into the virtual sandbox.
     */
    fun installPackage(packageName: String, profileId: String? = null, profileName: String? = null): Boolean {
        val ctx = appContext ?: return false
        val pm = ctx.packageManager
        return try {
            val appInfo: ApplicationInfo = pm.getApplicationInfo(packageName, 0)
            val appName = pm.getApplicationLabel(appInfo).toString()
            
            // Allocate sandbox I/O directories
            ioRedirectService?.getPackageDataDir(packageName)

            val clonedInfo = ClonedAppInfo(
                packageName = packageName,
                appName = appName,
                assignedProfileId = profileId,
                assignedProfileName = profileName,
                cloneTime = System.currentTimeMillis()
            )
            installedVirtualApps[packageName] = clonedInfo
            updateClonedAppsList()
            saveStoredClones()
            Log.i(TAG, "Cloned and installed package: $packageName ($appName)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed installing package $packageName: ${e.message}", e)
            false
        }
    }

    /**
     * Uninstalls a cloned package and wipes its sandbox storage.
     */
    fun uninstallPackage(packageName: String): Boolean {
        installedVirtualApps.remove(packageName)
        ioRedirectService?.cleanPackageData(packageName)
        activityManagerService?.killApp(packageName)
        updateClonedAppsList()
        saveStoredClones()
        Log.i(TAG, "Uninstalled cloned package: $packageName")
        return true
    }

    /**
     * Checks if a package is cloned in the virtual space.
     */
    fun isAppInstalled(packageName: String): Boolean {
        return installedVirtualApps.containsKey(packageName)
    }

    /**
     * Returns a list of all cloned applications.
     */
    fun getInstalledApps(): List<ClonedAppInfo> {
        return installedVirtualApps.values.toList().sortedByDescending { it.cloneTime }
    }

    /**
     * Dispatches launch request for cloned app.
     */
    fun launchApp(context: Context, packageName: String): Boolean {
        val cloned = installedVirtualApps[packageName] ?: return false
        val launched = activityManagerService?.launchActivity(packageName) ?: false
        if (launched) {
            val updated = cloned.copy(
                launchCount = cloned.launchCount + 1,
                lastLaunchedTime = System.currentTimeMillis(),
                isRunning = true
            )
            installedVirtualApps[packageName] = updated
            updateClonedAppsList()
            saveStoredClones()
        }
        return launched
    }

    /**
     * Terminates running process.
     */
    fun killApp(packageName: String): Boolean {
        val killed = activityManagerService?.killApp(packageName) ?: false
        val cloned = installedVirtualApps[packageName]
        if (cloned != null) {
            installedVirtualApps[packageName] = cloned.copy(isRunning = false)
            updateClonedAppsList()
        }
        return killed
    }

    /**
     * Checks if app is running.
     */
    fun isAppRunning(packageName: String): Boolean {
        return activityManagerService?.isAppRunning(packageName) ?: false
    }

    /**
     * Updates the assigned profile on an existing cloned app.
     */
    fun updateAppProfile(packageName: String, profileId: String?, profileName: String?) {
        val existing = installedVirtualApps[packageName] ?: return
        val updated = existing.copy(
            assignedProfileId = profileId,
            assignedProfileName = profileName
        )
        installedVirtualApps[packageName] = updated
        updateClonedAppsList()
        saveStoredClones()
    }

    /**
     * Wipes all cloned apps and sandbox data.
     */
    fun clearAllClones() {
        installedVirtualApps.clear()
        ioRedirectService?.cleanAll()
        updateClonedAppsList()
        saveStoredClones()
    }

    private fun updateClonedAppsList() {
        _clonedAppsFlow.value = installedVirtualApps.values.toList().sortedByDescending { it.cloneTime }
    }

    private fun saveStoredClones() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences("virtual_clones_pref", Context.MODE_PRIVATE)
            val json = com.google.gson.Gson().toJson(installedVirtualApps.values.toList())
            prefs.edit().putString("cloned_apps", json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist clones: ${e.message}")
        }
    }

    private fun loadStoredClones() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences("virtual_clones_pref", Context.MODE_PRIVATE)
            val json = prefs.getString("cloned_apps", null) ?: return
            val type = object : com.google.gson.reflect.TypeToken<List<ClonedAppInfo>>() {}.type
            val list: List<ClonedAppInfo> = com.google.gson.Gson().fromJson(json, type) ?: emptyList()
            for (item in list) {
                installedVirtualApps[item.packageName] = item.copy(isRunning = false)
            }
            updateClonedAppsList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed loading clones: ${e.message}")
        }
    }
}
