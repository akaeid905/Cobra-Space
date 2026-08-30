package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AppRepository
import com.example.data.repository.ProfileRepository
import com.example.domain.model.AppSettings
import com.example.domain.model.ClonedAppInfo
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.SpoofProfile
import com.example.spoofing.HookEntry
import com.example.virtualcore.CloneManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppFilter {
    ALL,
    USER,
    SYSTEM
}

data class AppListUiState(
    val clonedApps: List<ClonedAppInfo> = emptyList(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val filteredInstalledApps: List<InstalledAppInfo> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: AppFilter = AppFilter.ALL,
    val selectedAppForProfileAssignment: ClonedAppInfo? = null,
    val isAssignDialogOpen: Boolean = false,
    val isBulkAssignDialogOpen: Boolean = false,
    val selectedPackagesForBulk: Set<String> = emptySet(),
    val isLSPosedActive: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null
)

class SpoofAppListViewModel(
    private val appRepository: AppRepository,
    private val profileRepository: ProfileRepository,
    private val cloneManager: CloneManager
) : ViewModel() {

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(AppFilter.ALL)
    private val _selectedAppForAssignment = MutableStateFlow<ClonedAppInfo?>(null)
    private val _isAssignDialogOpen = MutableStateFlow(false)
    private val _isBulkAssignDialogOpen = MutableStateFlow(false)
    private val _selectedPackagesForBulk = MutableStateFlow<Set<String>>(emptySet())
    private val _isLoading = MutableStateFlow(false)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AppListUiState> = combine(
        appRepository.clonedApps,
        _installedApps,
        _searchQuery,
        _selectedFilter,
        _selectedAppForAssignment,
        _isAssignDialogOpen,
        _isBulkAssignDialogOpen,
        _selectedPackagesForBulk,
        profileRepository.settings,
        _isLoading,
        _snackbarMessage
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val clones = args[0] as List<ClonedAppInfo>
        @Suppress("UNCHECKED_CAST")
        val installed = args[1] as List<InstalledAppInfo>
        val query = args[2] as String
        val filter = args[3] as AppFilter
        val assignApp = args[4] as ClonedAppInfo?
        val isAssignOpen = args[5] as Boolean
        val isBulkOpen = args[6] as Boolean
        @Suppress("UNCHECKED_CAST")
        val bulkPackages = args[7] as Set<String>
        val settings = args[8] as AppSettings
        val loading = args[9] as Boolean
        val message = args[10] as String?

        val filtered = installed.filter { app ->
            val matchesQuery = query.isBlank() ||
                app.appName.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
            
            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.USER -> !app.isSystemApp
                AppFilter.SYSTEM -> app.isSystemApp
            }

            matchesQuery && matchesFilter
        }

        AppListUiState(
            clonedApps = clones,
            installedApps = installed,
            filteredInstalledApps = filtered,
            searchQuery = query,
            selectedFilter = filter,
            selectedAppForProfileAssignment = assignApp,
            isAssignDialogOpen = isAssignOpen,
            isBulkAssignDialogOpen = isBulkOpen,
            selectedPackagesForBulk = bulkPackages,
            isLSPosedActive = HookEntry.isLSPosedActive(),
            settings = settings,
            isLoading = loading,
            snackbarMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppListUiState()
    )

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val apps = appRepository.getInstalledApps(includeSystemApps = true)
            _installedApps.value = apps
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: AppFilter) {
        _selectedFilter.value = filter
    }

    fun cloneApp(packageName: String, profileId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = appRepository.cloneApp(packageName, profileId)
            _isLoading.value = false
            if (success) {
                _snackbarMessage.value = "App cloned into virtual sandbox"
                loadInstalledApps()
            } else {
                _snackbarMessage.value = "Failed to clone app (already cloned or inaccessible)"
            }
        }
    }

    fun launchClonedApp(packageName: String) {
        viewModelScope.launch {
            val success = appRepository.launchClonedApp(packageName)
            if (!success) {
                _snackbarMessage.value = "Could not launch cloned app in sandbox"
            }
        }
    }

    fun stopClonedApp(packageName: String) {
        appRepository.stopClonedApp(packageName)
        _snackbarMessage.value = "Process stopped"
    }

    fun uninstallClonedApp(packageName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = appRepository.uninstallClonedApp(packageName)
            _isLoading.value = false
            if (success) {
                _snackbarMessage.value = "Uninstalled from virtual sandbox"
                loadInstalledApps()
            }
        }
    }

    fun openAssignProfileDialog(app: ClonedAppInfo) {
        _selectedAppForAssignment.value = app
        _isAssignDialogOpen.value = true
    }

    fun dismissAssignProfileDialog() {
        _isAssignDialogOpen.value = false
        _selectedAppForAssignment.value = null
    }

    fun assignProfileToApp(packageName: String, profile: SpoofProfile) {
        viewModelScope.launch {
            cloneManager.assignProfile(packageName, profile)
            dismissAssignProfileDialog()
            _snackbarMessage.value = "Assigned '${profile.profileName}' to app"
        }
    }

    fun openBulkAssignDialog() {
        _selectedPackagesForBulk.value = uiState.value.clonedApps.map { it.packageName }.toSet()
        _isBulkAssignDialogOpen.value = true
    }

    fun dismissBulkAssignDialog() {
        _isBulkAssignDialogOpen.value = false
        _selectedPackagesForBulk.value = emptySet()
    }

    fun toggleBulkPackageSelection(packageName: String) {
        val current = _selectedPackagesForBulk.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedPackagesForBulk.value = current
    }

    fun applyBulkAssign(profile: SpoofProfile) {
        viewModelScope.launch {
            val packages = _selectedPackagesForBulk.value.toList()
            if (packages.isNotEmpty()) {
                cloneManager.bulkAssignProfile(packages, profile)
                _snackbarMessage.value = "Assigned '${profile.profileName}' to ${packages.size} apps"
            }
            dismissBulkAssignDialog()
        }
    }

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            profileRepository.updateSettings(settings)
            _snackbarMessage.value = "Settings updated"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            cloneManager.clearAll()
            loadInstalledApps()
            _isLoading.value = false
            _snackbarMessage.value = "All sandbox clones and profiles reset"
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }
}
