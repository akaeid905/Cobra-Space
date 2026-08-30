package com.example.data

import com.example.domain.model.SpoofProfile
import com.example.spoofing.RandomIdGenerator

/**
 * High-fidelity preset hardware and device identity profiles.
 */
object PresetProfiles {

    val PIXEL_9_PRO = SpoofProfile(
        id = "preset_pixel_9_pro",
        profileName = "Pixel 9 Pro",
        androidId = "9f4e2b810d7a65c3",
        gsfId = "3fa81b94c027ed11",
        deviceModel = "Pixel 9 Pro",
        brand = "Google",
        manufacturer = "Google",
        fingerprint = "google/caiman/caiman:14/AP2A.240805.005.F1/12111005:user/release-keys",
        buildVersion = "AP2A.240805.005.F1",
        sdkVersion = 34,
        securityPatch = "2024-08-05",
        productName = "caiman",
        hardware = "zuma",
        buildDescription = "caiman-user 14 AP2A.240805.005.F1 12111005 release-keys",
        imei = "358921094721839",
        macAddress = "02:1a:4c:89:b3:f1",
        isPreset = true
    )

    val GALAXY_S24_ULTRA = SpoofProfile(
        id = "preset_galaxy_s24_ultra",
        profileName = "Galaxy S24 Ultra",
        androidId = "7a1b5c9d3e2f4088",
        gsfId = "8bc294df012e56aa",
        deviceModel = "SM-S928B",
        brand = "Samsung",
        manufacturer = "Samsung",
        fingerprint = "samsung/e3qxxx/e3q:14/UP1A.231005.007/S928BXXU1AXCA:user/release-keys",
        buildVersion = "UP1A.231005.007",
        sdkVersion = 34,
        securityPatch = "2024-04-01",
        productName = "e3qxxx",
        hardware = "qcom",
        buildDescription = "e3qxxx-user 14 UP1A.231005.007 S928BXXU1AXCA release-keys",
        imei = "862143058912347",
        macAddress = "44:6d:57:3b:91:c2",
        isPreset = true
    )

    val XIAOMI_14_PRO = SpoofProfile(
        id = "preset_xiaomi_14_pro",
        profileName = "Xiaomi 14 Pro",
        androidId = "5c3d1e9f2a8b7460",
        gsfId = "4db90382fe1a7192",
        deviceModel = "23116PN5BC",
        brand = "Xiaomi",
        manufacturer = "Xiaomi",
        fingerprint = "Xiaomi/shennong/shennong:14/UKQ1.230804.001/V816.0.24.0.UNBCNXM:user/release-keys",
        buildVersion = "UKQ1.230804.001",
        sdkVersion = 34,
        securityPatch = "2024-03-01",
        productName = "shennong",
        hardware = "qcom",
        buildDescription = "shennong-user 14 UKQ1.230804.001 V816.0.24.0.UNBCNXM release-keys",
        imei = "865421063981245",
        macAddress = "68:db:54:19:a8:33",
        isPreset = true
    )

    val ONEPLUS_12 = SpoofProfile(
        id = "preset_oneplus_12",
        profileName = "OnePlus 12",
        androidId = "1a8e4b7c9f0d2356",
        gsfId = "7ca15b39e0842fd9",
        deviceModel = "CPH2581",
        brand = "OnePlus",
        manufacturer = "OnePlus",
        fingerprint = "OnePlus/CPH2581EEA/OP595DL1:14/UKQ1.230924.001/U.18d3632_1-1-1:user/release-keys",
        buildVersion = "UKQ1.230924.001",
        sdkVersion = 34,
        securityPatch = "2024-05-05",
        productName = "CPH2581",
        hardware = "qcom",
        buildDescription = "CPH2581-user 14 UKQ1.230924.001 U.18d3632_1-1-1 release-keys",
        imei = "354921081943820",
        macAddress = "9c:28:bf:12:47:0e",
        isPreset = true
    )

    val NOTHING_PHONE_2 = SpoofProfile(
        id = "preset_nothing_phone_2",
        profileName = "Nothing Phone 2",
        androidId = "3d7b9e1a5f8c0246",
        gsfId = "5ef01a938c2b74d1",
        deviceModel = "A065",
        brand = "Nothing",
        manufacturer = "Nothing",
        fingerprint = "Nothing/PongEEA/Pong:14/UP1A.231005.007/240308-1800:user/release-keys",
        buildVersion = "UP1A.231005.007",
        sdkVersion = 34,
        securityPatch = "2024-04-05",
        productName = "Pong",
        hardware = "qcom",
        buildDescription = "Pong-user 14 UP1A.231005.007 240308-1800 release-keys",
        imei = "352948119028374",
        macAddress = "b0:41:1d:9a:3c:5f",
        isPreset = true
    )

    val ALL_PRESETS = listOf(
        PIXEL_9_PRO,
        GALAXY_S24_ULTRA,
        XIAOMI_14_PRO,
        ONEPLUS_12,
        NOTHING_PHONE_2
    )

    /**
     * Generates a realistic randomized profile based on a chosen brand model template.
     */
    fun createRandomProfile(customName: String? = null): SpoofProfile {
        val base = ALL_PRESETS.random()
        val randomAndroidId = RandomIdGenerator.generateAndroidId()
        val randomGsfId = RandomIdGenerator.generateGsfId()
        val randomImei = RandomIdGenerator.generateImei()
        val randomMac = RandomIdGenerator.generateMacAddress()
        val randomBuildId = RandomIdGenerator.generateBuildId()

        return SpoofProfile(
            profileName = customName ?: "${base.brand} Virtual ${randomAndroidId.take(4).uppercase()}",
            androidId = randomAndroidId,
            gsfId = randomGsfId,
            deviceModel = base.deviceModel,
            brand = base.brand,
            manufacturer = base.manufacturer,
            fingerprint = "${base.brand.lowercase()}/${base.productName}/${base.productName}:14/$randomBuildId/rel:user/release-keys",
            buildVersion = randomBuildId,
            sdkVersion = 34,
            securityPatch = "2024-07-01",
            productName = base.productName,
            hardware = base.hardware,
            buildDescription = "${base.productName}-user 14 $randomBuildId release-keys",
            imei = randomImei,
            macAddress = randomMac,
            isPreset = false
        )
    }
}
