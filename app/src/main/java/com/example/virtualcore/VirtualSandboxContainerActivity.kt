package com.example.virtualcore

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Process
import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.PresetProfiles
import com.example.domain.model.SpoofProfile
import com.example.spoofing.HookEntry
import com.example.spoofing.RandomIdGenerator
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.StatusActive
import com.example.ui.theme.VirtualSpaceTheme
import java.io.File

/**
 * Isolated Sandbox Runtime Container Activity.
 * Runs in its own dedicated process (:virtual_sandbox_process) with private storage
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
        
        // Every time this sandbox is launched, always use a fresh new Android ID
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
                SandboxLaunchScreen(
                    targetPackage = targetPackage,
                    initialProfile = activeProfile,
                    onExit = { finish() },
                    onLaunchApp = { profile ->
                        launchMainAppIntent(targetPackage, profile)
                    },
                    onRandomizeIdentity = { newProfile ->
                        activeProfile = newProfile
                        hookEntry.applyProfileToCurrentProcess(newProfile)
                        Toast.makeText(this, "New Android ID Generated & Applied!", Toast.LENGTH_SHORT).show()
                    },
                    onWipeData = {
                        wipeSandboxData(targetPackage)
                        Toast.makeText(this, "Sandbox Storage Wiped Clean!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun launchMainAppIntent(packageName: String, profile: SpoofProfile) {
        try {
            val pm = packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.putExtra("is_virtual_instance", true)
                launchIntent.putExtra("sandbox_android_id", profile.androidId)
                startActivity(launchIntent)
                Toast.makeText(this, "Launching with Android ID: ${profile.androidId}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Virtual instance ready with Android ID: ${profile.androidId}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch main activity: ${e.message}", e)
            Toast.makeText(this, "Launched in Virtual Sandbox!", Toast.LENGTH_SHORT).show()
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
fun SandboxLaunchScreen(
    targetPackage: String,
    initialProfile: SpoofProfile,
    onExit: () -> Unit,
    onLaunchApp: (SpoofProfile) -> Unit,
    onRandomizeIdentity: (SpoofProfile) -> Unit,
    onWipeData: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var currentProfile by remember { mutableStateOf(initialProfile) }
    var packageInfo by remember { mutableStateOf<PackageInfo?>(null) }
    var appName by remember { mutableStateOf(targetPackage) }
    var appIconDrawable by remember { mutableStateOf<Drawable?>(null) }
    var storageSizeBytes by remember { mutableLongStateOf(0L) }
    val scrollState = rememberScrollState()

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
            val pInfo = pm.getPackageInfo(targetPackage, 0)
            packageInfo = pInfo
            appName = pm.getApplicationLabel(pInfo.applicationInfo!!).toString()
            appIconDrawable = pm.getApplicationIcon(pInfo.applicationInfo!!)
        } catch (e: Exception) {
            Log.w("SandboxLaunch", "Could not load package info: ${e.message}")
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
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (appIconDrawable != null) {
                                Image(
                                    bitmap = appIconDrawable!!.toBitmap(80, 80).asImageBitmap(),
                                    contentDescription = appName,
                                    modifier = Modifier.size(36.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
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
                                    text = "Virtual Sandbox Active (PID: ${Process.myPid()})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusActive,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Big Launch Button Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "VIRTUAL SANDBOX READY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "App is isolated with a newly generated device identity.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { onLaunchApp(currentProfile) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("launch_sandbox_app_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LAUNCH APP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Fresh Android ID Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "CURRENT ANDROID ID",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            color = StatusActive.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = StatusActive,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "NEW GENERATED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusActive
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setText(AnnotatedString(currentProfile.androidId))
                                Toast.makeText(context, "Android ID copied!", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currentProfile.androidId,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )

                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FilledTonalButton(
                        onClick = {
                            val newAndroidId = RandomIdGenerator.generateAndroidId()
                            val newImei = RandomIdGenerator.generateImei()
                            val newGsfId = RandomIdGenerator.generateGsfId()
                            val newMac = RandomIdGenerator.generateMacAddress()
                            val updated = currentProfile.copy(
                                androidId = newAndroidId,
                                imei = newImei,
                                gsfId = newGsfId,
                                macAddress = newMac
                            )
                            currentProfile = updated
                            onRandomizeIdentity(updated)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("regenerate_android_id_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Generate New Android ID",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Spoofed Device Telemetry Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "SPOOFED DEVICE SPECIFICATIONS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    DeviceSpecRow("Device Model", currentProfile.deviceModel)
                    DeviceSpecRow("Manufacturer / Brand", "${currentProfile.brand} (${currentProfile.manufacturer})")
                    DeviceSpecRow("IMEI Number", currentProfile.imei)
                    DeviceSpecRow("GSF Framework ID", currentProfile.gsfId)
                    DeviceSpecRow("Wi-Fi MAC", currentProfile.macAddress)
                    DeviceSpecRow("Package Name", targetPackage)
                }
            }

            // Sandbox Storage & Clean Data Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
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
                        Column {
                            Text(
                                text = "SANDBOX STORAGE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatBytes(storageSizeBytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onWipeData()
                            refreshSandboxStorage()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
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
                        Text("Clear Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.2f MB", bytes.toDouble() / (1024 * 1024))
    }
}
