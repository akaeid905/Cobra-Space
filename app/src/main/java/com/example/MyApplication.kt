package com.example

import android.app.Application
import android.util.Log
import com.example.data.repository.AppRepository
import com.example.data.repository.AppRepositoryImpl
import com.example.data.repository.ProfileRepository
import com.example.data.repository.ProfileRepositoryImpl
import com.example.data.storage.AppProfilesStorage
import com.example.spoofing.ProfileManager
import com.example.spoofing.SpoofEngine
import com.example.virtualcore.CloneManager
import com.lody.virtual.client.core.VirtualCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var profileManager: ProfileManager
        private set

    lateinit var spoofEngine: SpoofEngine
        private set

    lateinit var cloneManager: CloneManager
        private set

    lateinit var profileRepository: ProfileRepository
        private set

    lateinit var appRepository: AppRepository
        private set

    companion object {
        private const val TAG = "MyApplication"
        lateinit var instance: MyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Safe setup for multi-process WebView to prevent IllegalStateException on Android 9+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try {
                val processName = Application.getProcessName()
                val pkgName = packageName
                if (pkgName != processName) {
                    val suffix = processName.substringAfter(":", "sandbox").replace(":", "_").replace(".", "_")
                    android.webkit.WebView.setDataDirectorySuffix(suffix)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "WebView setDataDirectorySuffix notice: ${e.message}")
            }
        }

        // Catch and log uncaught exceptions gracefully
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Caught uncaught exception in process ${Application.getProcessName()}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            // Initialize Virtualization Core
            VirtualCore.get().startup(this)

            // Initialize Storage and Spoofing Layer
            val storage = AppProfilesStorage(this)
            profileManager = ProfileManager(storage)
            applicationScope.launch {
                profileManager.initialize()
            }

            spoofEngine = SpoofEngine(this, profileManager)
            cloneManager = CloneManager(this, profileManager, spoofEngine)

            // Initialize Repositories
            profileRepository = ProfileRepositoryImpl(profileManager)
            appRepository = AppRepositoryImpl(this, cloneManager)

            Log.i(TAG, "VirtualSpace application components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during application startup: ${e.message}", e)
        }
    }
}
