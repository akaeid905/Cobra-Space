package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.SpoofProfile
import com.example.spoofing.RandomIdGenerator

@Composable
fun ProfileEditDialog(
    profile: SpoofProfile,
    isNewProfile: Boolean,
    onDismiss: () -> Unit,
    onSave: (SpoofProfile) -> Unit
) {
    var profileName by remember(profile) { mutableStateOf(profile.profileName) }
    var androidId by remember(profile) { mutableStateOf(profile.androidId) }
    var gsfId by remember(profile) { mutableStateOf(profile.gsfId) }
    var deviceModel by remember(profile) { mutableStateOf(profile.deviceModel) }
    var brand by remember(profile) { mutableStateOf(profile.brand) }
    var manufacturer by remember(profile) { mutableStateOf(profile.manufacturer) }
    var fingerprint by remember(profile) { mutableStateOf(profile.fingerprint) }
    var buildVersion by remember(profile) { mutableStateOf(profile.buildVersion) }
    var sdkVersionStr by remember(profile) { mutableStateOf(profile.sdkVersion.toString()) }
    var securityPatch by remember(profile) { mutableStateOf(profile.securityPatch) }
    var productName by remember(profile) { mutableStateOf(profile.productName) }
    var hardware by remember(profile) { mutableStateOf(profile.hardware) }
    var buildDescription by remember(profile) { mutableStateOf(profile.buildDescription) }
    var imei by remember(profile) { mutableStateOf(profile.imei) }
    var macAddress by remember(profile) { mutableStateOf(profile.macAddress) }

    var hookAndroidId by remember(profile) { mutableStateOf(profile.hookAndroidId) }
    var hookBuildProps by remember(profile) { mutableStateOf(profile.hookBuildProps) }
    var hookTelephony by remember(profile) { mutableStateOf(profile.hookTelephony) }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Identity", "Build Props", "Telephony", "Hooks")

    val isAndroidIdValid = androidId.length == 16 && androidId.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }

    fun randomizeAll() {
        val newAndroidId = RandomIdGenerator.generateAndroidId()
        val newGsfId = RandomIdGenerator.generateGsfId()
        val newImei = RandomIdGenerator.generateImei()
        val newMac = RandomIdGenerator.generateMacAddress()
        val newBuild = RandomIdGenerator.generateBuildId()

        androidId = newAndroidId
        gsfId = newGsfId
        imei = newImei
        macAddress = newMac
        buildVersion = newBuild
        fingerprint = "${brand.lowercase()}/$productName/$productName:14/$newBuild/rel:user/release-keys"
        buildDescription = "$productName-user 14 $newBuild release-keys"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isNewProfile) "Create Device Profile" else "Edit Device Profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Randomize All Quick Action
                FilledTonalButton(
                    onClick = { randomizeAll() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("randomize_all_fields_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Randomize All Identifiers (AxeSpoofer)",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Contents
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Identity Tab
                            OutlinedTextField(
                                value = profileName,
                                onValueChange = { profileName = it },
                                label = { Text("Profile Name") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_profile_name"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = androidId,
                                onValueChange = { if (it.length <= 16) androidId = it.lowercase() },
                                label = { Text("Android ID (16 Hex Characters)") },
                                isError = !isAndroidIdValid,
                                supportingText = {
                                    if (!isAndroidIdValid) {
                                        Text("Must be exactly 16 hex characters [0-9, a-f]", color = MaterialTheme.colorScheme.error)
                                    } else {
                                        Text("${androidId.length}/16 hex characters valid")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_android_id"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = gsfId,
                                onValueChange = { gsfId = it.lowercase() },
                                label = { Text("GSF ID (Google Services Framework)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = brand,
                                onValueChange = { brand = it },
                                label = { Text("Device Brand (e.g. Google, Samsung)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = deviceModel,
                                onValueChange = { deviceModel = it },
                                label = { Text("Device Model (e.g. Pixel 9 Pro)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = manufacturer,
                                onValueChange = { manufacturer = it },
                                label = { Text("Manufacturer") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        1 -> {
                            // Build Props Tab
                            OutlinedTextField(
                                value = buildVersion,
                                onValueChange = { buildVersion = it },
                                label = { Text("Build Version / ID") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = sdkVersionStr,
                                onValueChange = { sdkVersionStr = it.filter { ch -> ch.isDigit() } },
                                label = { Text("SDK Version (API Level, e.g. 34)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = securityPatch,
                                onValueChange = { securityPatch = it },
                                label = { Text("Security Patch Date (YYYY-MM-DD)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = productName,
                                onValueChange = { productName = it },
                                label = { Text("Product / Device Name (e.g. caiman)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = hardware,
                                onValueChange = { hardware = it },
                                label = { Text("Hardware Board (e.g. zuma, qcom)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = fingerprint,
                                onValueChange = { fingerprint = it },
                                label = { Text("Build Fingerprint") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )

                            OutlinedTextField(
                                value = buildDescription,
                                onValueChange = { buildDescription = it },
                                label = { Text("Build Display Description") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 3
                            )
                        }

                        2 -> {
                            // Telephony & Network Tab
                            OutlinedTextField(
                                value = imei,
                                onValueChange = { if (it.length <= 15) imei = it.filter { ch -> ch.isDigit() } },
                                label = { Text("IMEI (15 Digits)") },
                                modifier = Modifier.fillMaxWidth(),
                                supportingText = { Text("${imei.length}/15 digits") },
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = macAddress,
                                onValueChange = { macAddress = it },
                                label = { Text("WiFi MAC Address (e.g. 02:1a:4c:89:b3:f1)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        3 -> {
                            // Hooks Tab
                            Text(
                                text = "Hook Module Activation Scope",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Choose which runtime layers will be intercepted for apps assigned to this profile:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HookToggleItem(
                                title = "Spoof Android ID",
                                subtitle = "Intercept Settings.Secure.getString(ANDROID_ID)",
                                checked = hookAndroidId,
                                onCheckedChange = { hookAndroidId = it }
                            )

                            HookToggleItem(
                                title = "Spoof Build Properties",
                                subtitle = "Inject Model, Brand, Fingerprint, Manufacturer, SDK",
                                checked = hookBuildProps,
                                onCheckedChange = { hookBuildProps = it }
                            )

                            HookToggleItem(
                                title = "Spoof Telephony & Network",
                                subtitle = "Intercept TelephonyManager getDeviceId/IMEI & MAC",
                                checked = hookTelephony,
                                onCheckedChange = { hookTelephony = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val sdkInt = sdkVersionStr.toIntOrNull() ?: 34
                            val updatedProfile = profile.copy(
                                profileName = profileName.ifBlank { "Custom Device Profile" },
                                androidId = androidId,
                                gsfId = gsfId,
                                deviceModel = deviceModel,
                                brand = brand,
                                manufacturer = manufacturer,
                                fingerprint = fingerprint,
                                buildVersion = buildVersion,
                                sdkVersion = sdkInt,
                                securityPatch = securityPatch,
                                productName = productName,
                                hardware = hardware,
                                buildDescription = buildDescription,
                                imei = imei,
                                macAddress = macAddress,
                                hookAndroidId = hookAndroidId,
                                hookBuildProps = hookBuildProps,
                                hookTelephony = hookTelephony,
                                isPreset = false
                            )
                            onSave(updatedProfile)
                        },
                        enabled = isAndroidIdValid,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("save_profile_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HookToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

