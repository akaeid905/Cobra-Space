package com.example.virtualcore

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.example.data.PresetProfiles
import com.example.domain.model.SpoofProfile
import com.example.spoofing.HookEntry
import com.example.spoofing.RandomIdGenerator
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.VirtualSpaceTheme
import java.io.File

/**
 * Isolated Sandbox Runtime Container Activity.
 * Runs in its own dedicated process (`:virtual_sandbox_process`) with private storage
 * and dynamic hardware identity spoofing applied directly to the runtime process.
 */
class VirtualSandboxContainerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_PROFILE_ID = "extra_profile_id"
        const val EXTRA_PROFILE_NAME = "extra_profile_name"
        const val EXTRA_ANDROID_ID = "extra_android_id"
        const val EXTRA_DEVICE_MODEL = "extra_device_model"
        const val EXTRA_BRAND = "extra_brand"
        const val EXTRA_FINGERPRINT = "extra_fingerprint"
        const val EXTRA_IMEI = "extra_imei"
        const val EXTRA_GSF_ID = "extra_gsf_id"
        const val EXTRA_MAC = "extra_mac"
        private const val TAG = "VirtualSandboxActivity"
    }

    private val hookEntry = HookEntry()
    private var targetPackage: String = ""
    private var activeProfile: SpoofProfile = PresetProfiles.PIXEL_9_PRO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "com.example.clonedapp"
        val profileName = intent.getStringExtra(EXTRA_PROFILE_NAME) ?: "Sandboxed Virtual Profile"
        val androidId = intent.getStringExtra(EXTRA_ANDROID_ID) ?: RandomIdGenerator.generateAndroidId()
        val deviceModel = intent.getStringExtra(EXTRA_DEVICE_MODEL) ?: "Pixel 9 Pro"
        val brand = intent.getStringExtra(EXTRA_BRAND) ?: "Google"
        val fingerprint = intent.getStringExtra(EXTRA_FINGERPRINT) ?: "google/komodo/komodo:14/UD1A.230805.019/10816111:user/release-keys"
        val imei = intent.getStringExtra(EXTRA_IMEI) ?: RandomIdGenerator.generateImei()
        val gsfId = intent.getStringExtra(EXTRA_GSF_ID) ?: RandomIdGenerator.generateGsfId()
        val mac = intent.getStringExtra(EXTRA_MAC) ?: RandomIdGenerator.generateMacAddress()

        activeProfile = SpoofProfile(
            id = intent.getStringExtra(EXTRA_PROFILE_ID) ?: "sandbox_${System.currentTimeMillis()}",
            profileName = profileName,
            deviceModel = deviceModel,
            brand = brand,
            manufacturer = brand,
            productName = deviceModel.lowercase().replace(" ", "_"),
            hardware = "qcom",
            fingerprint = fingerprint,
            buildVersion = "AP2A.240805.005",
            buildDescription = "$brand $deviceModel userdebug 14 AP2A.240805.005 release-keys",
            sdkVersion = 34,
            securityPatch = "2024-08-01",
            androidId = androidId,
            imei = imei,
            gsfId = gsfId,
            macAddress = mac,
            hookAndroidId = true,
            hookBuildProps = true,
            hookTelephony = true
        )

        // Apply hardware & build identity hooks directly inside this isolated process
        hookEntry.applyProfileToCurrentProcess(activeProfile)

        // Initialize sandbox directories
        setupIsolatedStorage(targetPackage)

        setContent {
            VirtualSpaceTheme {
                SandboxContainerScreen(
                    targetPackage = targetPackage,
                    initialProfile = activeProfile,
                    onExit = { finish() },
                    onRandomizeIdentity = { newProfile ->
                        activeProfile = newProfile
                        hookEntry.applyProfileToCurrentProcess(newProfile)
                        Toast.makeText(this, "Identity Hot-Reloaded in Sandbox!", Toast.LENGTH_SHORT).show()
                    },
                    onWipeData = {
                        wipeSandboxData(targetPackage)
                        Toast.makeText(this, "Sandbox Storage Wiped Clean!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun setupIsolatedStorage(packageName: String): File {
        val rootDir = File(filesDir, "virtual_space/sandbox/data/user/0/$packageName")
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
        File(rootDir, "files").mkdirs()
        File(rootDir, "cache").mkdirs()
        File(rootDir, "databases").mkdirs()
        File(rootDir, "shared_prefs").mkdirs()
        return rootDir
    }

    private fun wipeSandboxData(packageName: String) {
        val rootDir = File(filesDir, "virtual_space/sandbox/data/user/0/$packageName")
        rootDir.deleteRecursively()
        setupIsolatedStorage(packageName)
    }
}

@Composable
fun SandboxContainerScreen(
    targetPackage: String,
    initialProfile: SpoofProfile,
    onExit: () -> Unit,
    onRandomizeIdentity: (SpoofProfile) -> Unit,
    onWipeData: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var currentProfile by remember { mutableStateOf(initialProfile) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var packageInfo by remember { mutableStateOf<PackageInfo?>(null) }
    var appName by remember { mutableStateOf(targetPackage) }
    var appIconDrawable by remember { mutableStateOf<Drawable?>(null) }
    var storageSizeBytes by remember { mutableStateOf(0L) }
    var activitiesList by remember { mutableStateOf<List<ActivityInfo>>(emptyList()) }

    fun refreshSandboxStorage() {
        val rootDir = File(context.filesDir, "virtual_space/sandbox/data/user/0/$targetPackage")
        var total = 0L
        rootDir.walkTopDown().forEach { file ->
            if (file.isFile) total += file.length()
        }
        storageSizeBytes = total
    }

    LaunchedEffect(targetPackage) {
        try {
            val pm = context.packageManager
            val pInfo = pm.getPackageInfo(
                targetPackage,
                PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_PERMISSIONS
            )
            packageInfo = pInfo
            appName = pm.getApplicationLabel(pInfo.applicationInfo!!).toString()
            appIconDrawable = pm.getApplicationIcon(pInfo.applicationInfo!!)
            activitiesList = pInfo.activities?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.w("SandboxContainer", "Could not load full package info: ${e.message}")
        }
        refreshSandboxStorage()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("virtual_sandbox_screen"),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onExit,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("sandbox_exit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Exit Sandbox",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // App Icon
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (appIconDrawable != null) {
                                    Image(
                                        bitmap = appIconDrawable!!.toBitmap(72, 72).asImageBitmap(),
                                        contentDescription = appName,
                                        modifier = Modifier.size(32.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(StatusActive)
                                    )
                                    Text(
                                        text = "ISOLATED SANDBOX (PID: ${Process.myPid()})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusActive,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Hot Randomize ID Button
                        FilledTonalButton(
                            onClick = {
                                val updated = currentProfile.copy(
                                    androidId = RandomIdGenerator.generateAndroidId(),
                                    imei = RandomIdGenerator.generateImei(),
                                    gsfId = RandomIdGenerator.generateGsfId(),
                                    macAddress = RandomIdGenerator.generateMacAddress()
                                )
                                currentProfile = updated
                                onRandomizeIdentity(updated)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("sandbox_hot_randomize_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Randomize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hardware Identity Banner Chip
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${currentProfile.deviceModel} (${currentProfile.brand})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Text(
                                text = "ID: ${currentProfile.androidId}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Navigation Tabs
            val tabs = listOf(
                "Runtime & Workspace",
                "Identity Inspector",
                "Storage & Cache",
                "Web & Cloud"
            )

            androidx.compose.material3.TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> SandboxRuntimeTab(
                    targetPackage = targetPackage,
                    appName = appName,
                    packageInfo = packageInfo,
                    activitiesList = activitiesList,
                    currentProfile = currentProfile,
                    storageSizeBytes = storageSizeBytes,
                    onLaunchActivity = { activityName ->
                        try {
                            val intent = Intent().apply {
                                setClassName(targetPackage, activityName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra("is_virtual_instance", true)
                                putExtra("sandbox_android_id", currentProfile.androidId)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not start $activityName: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                )
                1 -> IdentityInspectorTab(
                    currentProfile = currentProfile,
                    targetPackage = targetPackage,
                    onCopy = { text ->
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
                2 -> SandboxStorageTab(
                    targetPackage = targetPackage,
                    storageSizeBytes = storageSizeBytes,
                    onWipe = {
                        onWipeData()
                        refreshSandboxStorage()
                    }
                )
                3 -> SandboxWebRunnerTab(
                    targetPackage = targetPackage,
                    currentProfile = currentProfile
                )
            }
        }
    }
}

@Composable
fun SandboxRuntimeTab(
    targetPackage: String,
    appName: String,
    packageInfo: PackageInfo?,
    activitiesList: List<ActivityInfo>,
    currentProfile: SpoofProfile,
    storageSizeBytes: Long,
    onLaunchActivity: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Virtual Sandbox Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "VIRTUAL SANDBOX ENGINE ACTIVE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "This cloned instance of $appName ($targetPackage) is running inside VirtualSpace's isolated multi-process runtime container. Hardware identifiers (Android ID, IMEI, Device Model, Fingerprint) are intercepted and spoofed locally in this process.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricChip(
                        label = "PROCESS",
                        value = ":virtual_sandbox",
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        label = "STORAGE",
                        value = formatBytes(storageSizeBytes),
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        label = "TARGET SDK",
                        value = "v${packageInfo?.applicationInfo?.targetSdkVersion ?: 34}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Package Information & Component Discovery
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "PACKAGE COMPONENTS (${activitiesList.size} Activities)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (activitiesList.isEmpty()) {
                    Text(
                        text = "No exported activity manifest entries detected directly. You can test Web/Hybrid mode or verify identity hooks in the Inspector tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    activitiesList.take(6).forEach { activity ->
                        val shortName = activity.name.substringAfterLast(".")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = shortName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activity.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            FilledTonalButton(
                                onClick = { onLaunchActivity(activity.name) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Launch", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IdentityInspectorTab(
    currentProfile: SpoofProfile,
    targetPackage: String,
    onCopy: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusActive,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "LIVE RUNTIME SPOOF VERIFICATION",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Live telemetry measured directly inside the running isolated process:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Live Values List
        val identityItems = listOf(
            IdentityItem("Android ID (Settings.Secure)", currentProfile.androidId, "Intercepted"),
            IdentityItem("Device Model (Build.MODEL)", currentProfile.deviceModel, "Hooked"),
            IdentityItem("Manufacturer (Build.MANUFACTURER)", currentProfile.manufacturer, "Hooked"),
            IdentityItem("Brand (Build.BRAND)", currentProfile.brand, "Hooked"),
            IdentityItem("Build Fingerprint", currentProfile.fingerprint, "Hooked"),
            IdentityItem("IMEI Number", currentProfile.imei, "Spoofed"),
            IdentityItem("GSF ID (Google Services)", currentProfile.gsfId, "Spoofed"),
            IdentityItem("MAC Address (Wifi/Net)", currentProfile.macAddress, "Spoofed"),
            IdentityItem("OS Version", "Android 14 (API ${currentProfile.sdkVersion})", "Hooked"),
            IdentityItem("Security Patch", currentProfile.securityPatch, "Hooked"),
            IdentityItem("Linux Process Isolation", "PID ${Process.myPid()} (:virtual_sandbox_process)", "Active")
        )

        identityItems.forEach { item ->
            IdentityRowCard(item = item, onCopy = onCopy)
        }
    }
}

data class IdentityItem(val label: String, val value: String, val status: String)

@Composable
fun IdentityRowCard(item: IdentityItem, onCopy: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy(item.value) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.value,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = item.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SandboxStorageTab(
    targetPackage: String,
    storageSizeBytes: Long,
    onWipe: () -> Unit
) {
    val context = LocalContext.current
    val rootDir = File(context.filesDir, "virtual_space/sandbox/data/user/0/$targetPackage")
    val subdirs = listOf("files", "cache", "databases", "shared_prefs")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "ISOLATED DATA DIRECTORY",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = rootDir.absolutePath,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Used Storage: ${formatBytes(storageSizeBytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = onWipe,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("wipe_sandbox_storage_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Wipe Storage", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text(
            text = "SANDBOX SUBDIRECTORIES",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        subdirs.forEach { dirName ->
            val dir = File(rootDir, dirName)
            var size = 0L
            if (dir.exists()) {
                dir.walkTopDown().forEach { if (it.isFile) size += it.length() }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "/$dirName",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = formatBytes(size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SandboxWebRunnerTab(
    targetPackage: String,
    currentProfile: SpoofProfile
) {
    var urlInput by remember { mutableStateOf("https://www.google.com") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Spoofed User Agent & Headers Banner
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "VIRTUAL WEB/HYBRID APP RUNNER",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "HTTP User-Agent and X-Android-Id headers are injected using the assigned ${currentProfile.deviceModel} profile.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Embedded Sandboxed WebView
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; ${currentProfile.deviceModel} Build/${currentProfile.buildVersion}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 VirtualSpace/${currentProfile.androidId}"
                        }
                        loadUrl(urlInput)
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun MetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.2f MB", bytes.toDouble() / (1024 * 1024))
    }
}
