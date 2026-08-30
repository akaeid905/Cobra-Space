package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PresetProfiles
import com.example.data.repository.ProfileRepository
import com.example.domain.model.SpoofProfile
import com.example.spoofing.RandomIdGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profiles: List<SpoofProfile> = emptyList(),
    val filteredProfiles: List<SpoofProfile> = emptyList(),
    val searchQuery: String = "",
    val editingProfile: SpoofProfile? = null,
    val isEditDialogOpen: Boolean = false,
    val isNewProfile: Boolean = false,
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _editingProfile = MutableStateFlow<SpoofProfile?>(null)
    private val _isEditDialogOpen = MutableStateFlow(false)
    private val _isNewProfile = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProfileUiState> = combine(
        profileRepository.profiles,
        _searchQuery,
        _editingProfile,
        _isEditDialogOpen,
        _isNewProfile,
        _isLoading,
        _snackbarMessage
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val profiles = args[0] as List<SpoofProfile>
        val query = args[1] as String
        val editing = args[2] as SpoofProfile?
        val isDialogOpen = args[3] as Boolean
        val isNew = args[4] as Boolean
        val loading = args[5] as Boolean
        val message = args[6] as String?

        val filtered = if (query.isBlank()) {
            profiles
        } else {
            profiles.filter {
                it.profileName.contains(query, ignoreCase = true) ||
                it.brand.contains(query, ignoreCase = true) ||
                it.deviceModel.contains(query, ignoreCase = true) ||
                it.androidId.contains(query, ignoreCase = true)
            }
        }

        ProfileUiState(
            profiles = profiles,
            filteredProfiles = filtered,
            searchQuery = query,
            editingProfile = editing,
            isEditDialogOpen = isDialogOpen,
            isNewProfile = isNew,
            isLoading = loading,
            snackbarMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun openCreateDialog() {
        val template = PresetProfiles.createRandomProfile("New Device Profile")
        _editingProfile.value = template
        _isNewProfile.value = true
        _isEditDialogOpen.value = true
    }

    fun openEditDialog(profile: SpoofProfile) {
        _editingProfile.value = profile
        _isNewProfile.value = false
        _isEditDialogOpen.value = true
    }

    fun dismissEditDialog() {
        _isEditDialogOpen.value = false
        _editingProfile.value = null
    }

    fun randomizeEditingFields(): SpoofProfile? {
        val current = _editingProfile.value ?: return null
        val base = PresetProfiles.ALL_PRESETS.random()
        val randomBuildId = RandomIdGenerator.generateBuildId()
        val randomized = current.copy(
            androidId = RandomIdGenerator.generateAndroidId(),
            gsfId = RandomIdGenerator.generateGsfId(),
            imei = RandomIdGenerator.generateImei(),
            macAddress = RandomIdGenerator.generateMacAddress(),
            buildVersion = randomBuildId,
            fingerprint = "${base.brand.lowercase()}/${base.productName}/${base.productName}:14/$randomBuildId/rel:user/release-keys",
            buildDescription = "${base.productName}-user 14 $randomBuildId release-keys"
        )
        _editingProfile.value = randomized
        return randomized
    }

    fun updateEditingProfile(updated: SpoofProfile) {
        _editingProfile.value = updated
    }

    fun saveProfile(profile: SpoofProfile) {
        if (!profile.isAndroidIdValid()) {
            _snackbarMessage.value = "Android ID must be exactly 16 hex characters"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            profileRepository.saveProfile(profile)
            _isLoading.value = false
            _isEditDialogOpen.value = false
            _editingProfile.value = null
            _snackbarMessage.value = "Profile '${profile.profileName}' saved"
        }
    }

    fun deleteProfile(profile: SpoofProfile) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profile.id)
            _snackbarMessage.value = "Profile '${profile.profileName}' deleted"
        }
    }

    fun duplicateProfile(profile: SpoofProfile) {
        viewModelScope.launch {
            val dup = profileRepository.duplicateProfile(profile)
            _snackbarMessage.value = "Duplicated as '${dup.profileName}'"
        }
    }

    fun addPreset(preset: SpoofProfile) {
        viewModelScope.launch {
            val newProfile = preset.copy(
                id = java.util.UUID.randomUUID().toString(),
                androidId = RandomIdGenerator.generateAndroidId(),
                gsfId = RandomIdGenerator.generateGsfId(),
                isPreset = false
            )
            profileRepository.saveProfile(newProfile)
            _snackbarMessage.value = "Added preset '${newProfile.profileName}'"
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }
}
