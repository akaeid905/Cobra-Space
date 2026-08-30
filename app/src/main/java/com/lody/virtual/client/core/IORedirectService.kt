package com.lody.virtual.client.core

import android.content.Context
import java.io.File

/**
 * Service managing I/O storage redirection to isolate cloned app data.
 */
class IORedirectService(private val context: Context) {

    private val virtualRoot: File by lazy {
        File(context.filesDir, "virtual_space/sandbox").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Gets the sandboxed private data directory for a given cloned package.
     */
    fun getPackageDataDir(packageName: String): File {
        val dir = File(virtualRoot, "data/user/0/$packageName")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Gets the sandboxed cache directory for a given package.
     */
    fun getPackageCacheDir(packageName: String): File {
        val dir = File(getPackageDataDir(packageName), "cache")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Wipes all sandbox data for a given package upon uninstallation.
     */
    fun cleanPackageData(packageName: String): Boolean {
        val dir = File(virtualRoot, "data/user/0/$packageName")
        return if (dir.exists()) {
            dir.deleteRecursively()
        } else {
            true
        }
    }

    /**
     * Wipes the entire virtual space sandbox.
     */
    fun cleanAll(): Boolean {
        return virtualRoot.deleteRecursively().also {
            virtualRoot.mkdirs()
        }
    }
}
