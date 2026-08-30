package com.example.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.domain.model.ClonedAppInfo
import com.example.domain.model.InstalledAppInfo
import com.example.virtualcore.CloneManager
import com.lody.virtual.client.core.VirtualCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

interface AppRepository {
    val clonedApps: StateFlow<List<ClonedAppInfo>>

    suspend fun getInstalledApps(includeSystemApps: Boolean): List<InstalledAppInfo>
    suspend fun cloneApp(packageName: String, profileId: String? = null): Boolean
    suspend fun launchClonedApp(packageName: String): Boolean
    suspend fun uninstallClonedApp(packageName: String): Boolean
    fun stopClonedApp(packageName: String): Boolean
}

class AppRepositoryImpl(
    private val context: Context,
    private val cloneManager: CloneManager
) : AppRepository {

    private val virtualCore = VirtualCore.get()
    override val clonedApps: StateFlow<List<ClonedAppInfo>> = cloneManager.clonedApps

    override suspend fun getInstalledApps(includeSystemApps: Boolean): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm: PackageManager = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val list = mutableListOf<InstalledAppInfo>()

        for (app in installed) {
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!includeSystemApps && isSystem) {
                continue
            }
            // Exclude self
            if (app.packageName == context.packageName) {
                continue
            }

            val appName = pm.getApplicationLabel(app).toString()
            val isCloned = virtualCore.isAppInstalled(app.packageName)
            val icon = try { pm.getApplicationIcon(app) } catch (e: Exception) { null }
            val versionName = try {
                pm.getPackageInfo(app.packageName, 0).versionName ?: "1.0"
            } catch (e: Exception) {
                "1.0"
            }

            list.add(
                InstalledAppInfo(
                    packageName = app.packageName,
                    appName = appName,
                    versionName = versionName,
                    isSystemApp = isSystem,
                    isCloned = isCloned,
                    icon = icon
                )
            )
        }

        list.sortedWith(compareBy<InstalledAppInfo> { !it.isCloned }.thenBy { it.appName.lowercase() })
    }

    override suspend fun cloneApp(packageName: String, profileId: String?): Boolean {
        return cloneManager.cloneApp(packageName, profileId)
    }

    override suspend fun launchClonedApp(packageName: String): Boolean {
        return cloneManager.launchClonedApp(packageName)
    }

    override suspend fun uninstallClonedApp(packageName: String): Boolean {
        return cloneManager.uninstallClonedApp(packageName)
    }

    override fun stopClonedApp(packageName: String): Boolean {
        return cloneManager.stopClonedApp(packageName)
    }
}
