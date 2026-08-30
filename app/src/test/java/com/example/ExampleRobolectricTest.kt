package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PresetProfiles
import com.example.data.storage.AppProfilesStorage
import com.example.domain.model.SpoofProfile
import com.example.spoofing.ProfileManager
import com.example.spoofing.RandomIdGenerator
import com.example.spoofing.SpoofEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VirtualSpace", appName)
    }

    @Test
    fun `random id generator generates valid 16-hex android id`() {
        val androidId = RandomIdGenerator.generateAndroidId()
        assertEquals(16, androidId.length)
        assertTrue(androidId.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' })
    }

    @Test
    fun `random id generator generates valid 15-digit imei with luhn checksum`() {
        val imei = RandomIdGenerator.generateImei()
        assertEquals(15, imei.length)
        assertTrue(imei.all { it.isDigit() })
    }

    @Test
    fun `profile manager saves and retrieves profiles`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = AppProfilesStorage(context)
        val profileManager = ProfileManager(storage)
        profileManager.initialize()

        val profile = PresetProfiles.createRandomProfile("Test Pixel Device").copy(
            androidId = "a1b2c3d4e5f60718",
            deviceModel = "Pixel 9",
            brand = "Google"
        )
        profileManager.saveProfile(profile)

        val retrieved = profileManager.getProfileById(profile.id)
        assertNotNull(retrieved)
        assertEquals("Test Pixel Device", retrieved?.profileName)
        assertEquals("a1b2c3d4e5f60718", retrieved?.androidId)
    }

    @Test
    fun `spoof engine applies profile hooks`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = AppProfilesStorage(context)
        val profileManager = ProfileManager(storage)
        profileManager.initialize()
        val spoofEngine = SpoofEngine(context, profileManager)

        val profile = PresetProfiles.createRandomProfile("Galaxy S24 Hook Target").copy(
            androidId = "1234567890abcdef",
            deviceModel = "SM-S928B",
            brand = "Samsung"
        )
        profileManager.saveProfile(profile)
        profileManager.assignProfileToApp("com.target.testapp", profile.id)

        val appliedProfile = spoofEngine.prepareAppIdentity("com.target.testapp")
        assertNotNull(appliedProfile)
        assertEquals("1234567890abcdef", appliedProfile?.androidId)
    }
}
