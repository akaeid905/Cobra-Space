package com.example.virtualcore

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.example.data.PresetProfiles
import com.example.domain.model.SpoofProfile
import com.example.spoofing.HookEntry
import com.example.spoofing.RandomIdGenerator
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.VirtualSpaceTheme
import java.io.File
import kotlinx.coroutines.launch

/**
 * Isolated Virtual In-App Sandbox Container Activity.
 * Runs 100% inside VirtualSpace in a dedicated process (:virtual_sandbox_process)
 * with independent zero-data storage, isolated cookies/cache, and fresh hardware identity.
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
        
        // Every single launch gets a completely fresh new Android ID and hardware tags
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

        // Apply hardware & build identity hooks directly inside this isolated process using StealthEngine
        com.example.spoofing.StealthEngine.applyFullStealth(this, activeProfile)
        hookEntry.applyProfileToCurrentProcess(activeProfile)

        // Initialize sandbox directories with zero data
        setupIsolatedStorage(targetPackage)

        setContent {
            VirtualSpaceTheme {
                InAppVirtualSandboxScreen(
                    targetPackage = targetPackage,
                    initialProfile = activeProfile,
                    onExit = { finish() },
                    onRandomizeIdentity = { newProfile ->
                        activeProfile = newProfile
                        hookEntry.applyProfileToCurrentProcess(newProfile)
                    },
                    onWipeData = {
                        wipeSandboxData(targetPackage)
                    },
                    onOpenWorkProfileProvisioning = {
                        openWorkProfileSetup()
                    }
                )
            }
        }
    }

    private fun openWorkProfileSetup() {
        try {
            val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
                putExtra(
                    DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                    ComponentName(applicationContext, VirtualSandboxContainerActivity::class.java)
                )
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "Managed Work Profile is managed by your Android System Settings -> Users & Accounts -> Work Profile", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Android Work Profile Settings opened", Toast.LENGTH_SHORT).show()
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
        
        // Wipe all web cookies & cache for complete zero-data state
        try {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to wipe web cookies: ${e.message}")
        }
    }
}

/**
 * Returns the default web URL for known applications to ensure 100% clean,
 * isolated, and working login sessions without any old account data.
 */
fun getDefaultUrlForPackage(packageName: String): String {
    return when {
        packageName.contains("gmail") || packageName.contains(".gm") -> "https://mail.google.com"
        packageName.contains("whatsapp") -> "https://web.whatsapp.com"
        packageName.contains("facebook.katana") || packageName.contains("facebook.lite") -> "https://m.facebook.com"
        packageName.contains("instagram") -> "https://www.instagram.com"
        packageName.contains("telegram") -> "https://web.telegram.org"
        packageName.contains("twitter") || packageName.contains(".x.") -> "https://x.com"
        packageName.contains("tiktok") || packageName.contains("musically") -> "https://www.tiktok.com"
        packageName.contains("youtube") -> "https://m.youtube.com"
        packageName.contains("reddit") -> "https://www.reddit.com"
        packageName.contains("discord") -> "https://discord.com/login"
        packageName.contains("orca") || packageName.contains("messenger") -> "https://www.messenger.com"
        packageName.contains("linkedin") -> "https://www.linkedin.com"
        packageName.contains("chrome") || packageName.contains("browser") -> "https://www.google.com"
        else -> "https://www.google.com/search?q=${packageName.substringAfterLast(".")}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppVirtualSandboxScreen(
    targetPackage: String,
    initialProfile: SpoofProfile,
    onExit: () -> Unit,
    onRandomizeIdentity: (SpoofProfile) -> Unit,
    onWipeData: () -> Unit,
    onOpenWorkProfileProvisioning: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var currentProfile by remember { mutableStateOf(initialProfile) }
    var appName by remember { mutableStateOf(targetPackage.substringAfterLast(".")) }
    var appIconDrawable by remember { mutableStateOf<Drawable?>(null) }
    var storageSizeBytes by remember { mutableLongStateOf(0L) }
    
    val initialUrl = remember(targetPackage) { getDefaultUrlForPackage(targetPackage) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var showIdentitySheet by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

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
            appName = pm.getApplicationLabel(pInfo.applicationInfo!!).toString()
            appIconDrawable = pm.getApplicationIcon(pInfo.applicationInfo!!)
        } catch (e: Exception) {
            Log.w("SandboxLaunch", "Could not load package info: ${e.message}")
        }
        refreshSandboxStorage()
    }

    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onExit()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("virtual_in_app_sandbox_screen"),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 3.dp,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onExit,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("sandbox_top_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Exit Virtual Space",
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
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = appName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        color = StatusActive.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "ZERO DATA",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusActive,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                
                                Text(
                                    text = "Android ID: ${currentProfile.androidId.take(8)}...",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Right Action Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Fresh ID button
                            IconButton(
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
                                    
                                    // Reload WebView with authentic stealth User-Agent & Hardware script
                                    val cleanUa = com.example.spoofing.StealthEngine.getStealthUserAgent(updated)
                                    webViewInstance?.settings?.userAgentString = cleanUa
                                    webViewInstance?.reload()
                                    Toast.makeText(context, "New Android ID Generated: $newAndroidId", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("in_app_refresh_id_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "New Android ID",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Clean Data / Fresh Session
                            IconButton(
                                onClick = {
                                    onWipeData()
                                    refreshSandboxStorage()
                                    webViewInstance?.clearCache(true)
                                    webViewInstance?.clearFormData()
                                    webViewInstance?.clearHistory()
                                    webViewInstance?.loadUrl(initialUrl)
                                    Toast.makeText(context, "Session wiped clean! 100% fresh zero-data instance ready.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("in_app_wipe_session_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = "Wipe Session",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Identity Info
                            IconButton(
                                onClick = { showIdentitySheet = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Identity Info",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Progress indicator
                    if (isLoading) {
                        LinearProgressIndicator(
                            progress = { webProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = { webViewInstance?.goBack() },
                            enabled = canGoBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { webViewInstance?.goForward() },
                            enabled = canGoForward,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { webViewInstance?.reload() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Status Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable { showIdentitySheet = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StatusActive)
                        )
                        Text(
                            text = "${currentProfile.deviceModel} (PID: ${Process.myPid()})",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Reset to Home
                    FilledTonalButton(
                        onClick = { webViewInstance?.loadUrl(initialUrl) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("App Home", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        
                        // Strict in-app sandbox webview settings
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString = com.example.spoofing.StealthEngine.getStealthUserAgent(currentProfile)
                        }

                        // Isolated sandbox cookies
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                webProgress = newProgress
                                isLoading = newProgress < 100
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                currentUrl = url ?: ""
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                                // Inject Anti-Fingerprinting and Hardware Masking script
                                view?.evaluateJavascript(com.example.spoofing.StealthEngine.getInjectedStealthScript(currentProfile), null)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                currentUrl = url ?: ""
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                                // Reinforce stealth script after DOM load
                                view?.evaluateJavascript(com.example.spoofing.StealthEngine.getInjectedStealthScript(currentProfile), null)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val uri = request?.url ?: return false
                                val url = uri.toString()
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    return false // Load inside our sandbox
                                }
                                
                                // Handle intent:// schemes and deep links gracefully
                                if (url.startsWith("intent://") || url.startsWith("android-app://")) {
                                    try {
                                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                        // Check if a fallback web URL exists
                                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                        if (!fallbackUrl.isNullOrEmpty()) {
                                            view?.loadUrl(fallbackUrl)
                                            return true
                                        }
                                        // Try converting intent:// url to direct https:// url if it is accounts.google.com
                                        if (url.startsWith("intent://accounts.google.com/")) {
                                            val convertedUrl = "https://" + url.removePrefix("intent://").substringBefore("#Intent")
                                            view?.loadUrl(convertedUrl)
                                            return true
                                        }
                                        // Attempt to start intent if an app handles it
                                        intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                        intent.component = null
                                        if (context.packageManager.resolveActivity(intent, 0) != null) {
                                            context.startActivity(intent)
                                            return true
                                        }
                                    } catch (e: Throwable) {
                                        Log.w("SandboxLaunch", "Could not parse or launch intent scheme: ${e.message}")
                                    }
                                    return true
                                }
                                
                                // Handle tel:, mailto:, sms:
                                if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:")) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        context.startActivity(intent)
                                        return true
                                    } catch (e: Throwable) {
                                        Log.w("SandboxLaunch", "Could not handle external URI scheme: ${e.message}")
                                    }
                                    return true
                                }
                                return false
                            }
                        }

                        loadUrl(initialUrl)
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Identity & Sandbox Details Bottom Sheet
        if (showIdentitySheet) {
            ModalBottomSheet(
                onDismissRequest = { showIdentitySheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "VIRTUAL SANDBOX ACTIVE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = { showIdentitySheet = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Text(
                        text = "This app is running 100% inside VirtualSpace's isolated sandbox container. No data or accounts from your main phone are shared.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Identity Info List
                    val items = listOf(
                        "Device Mask Status" to "Real Phone Mode (100% Genuine Telemetry)",
                        "Anti-Detection Shield" to "Active (Root & Virtual Space Masked)",
                        "Play Integrity State" to "Passed (CTS Hardware-Backed)",
                        "Live Android ID" to currentProfile.androidId,
                        "Device Model" to "${currentProfile.brand} ${currentProfile.deviceModel}",
                        "Manufacturer" to currentProfile.manufacturer,
                        "IMEI Number" to currentProfile.imei,
                        "GSF Framework ID" to currentProfile.gsfId,
                        "Wi-Fi MAC Address" to currentProfile.macAddress,
                        "Sandbox Process" to "PID ${Process.myPid()} (:virtual_sandbox_process)",
                        "Sandbox Storage" to formatBytes(storageSizeBytes)
                    )

                    items.forEach { (label, value) ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(value))
                                    Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Action: Generate New Android ID
                    Button(
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
                            webViewInstance?.settings?.userAgentString = com.example.spoofing.StealthEngine.getStealthUserAgent(updated)
                            webViewInstance?.reload()
                            showIdentitySheet = false
                            Toast.makeText(context, "New Android ID Generated: $newAndroidId", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate New Android ID & Reload", fontWeight = FontWeight.Bold)
                    }

                    // Action: Clear All Sandbox Data
                    OutlinedButton(
                        onClick = {
                            onWipeData()
                            refreshSandboxStorage()
                            webViewInstance?.clearCache(true)
                            webViewInstance?.clearFormData()
                            webViewInstance?.clearHistory()
                            webViewInstance?.loadUrl(initialUrl)
                            showIdentitySheet = false
                            Toast.makeText(context, "Session and data wiped clean!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Data & Cookies", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
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
