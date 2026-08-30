package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.MainAppNavigation
import com.example.ui.theme.VirtualSpaceTheme
import com.example.ui.viewmodel.ProfileViewModel
import com.example.ui.viewmodel.SpoofAppListViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MyApplication

        setContent {
            VirtualSpaceTheme {
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ProfileViewModel(app.profileRepository) as T
                        }
                    }
                )

                val appViewModel: SpoofAppListViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SpoofAppListViewModel(
                                appRepository = app.appRepository,
                                profileRepository = app.profileRepository,
                                cloneManager = app.cloneManager
                            ) as T
                        }
                    }
                )

                MainAppNavigation(
                    appViewModel = appViewModel,
                    profileViewModel = profileViewModel,
                    profileManager = app.profileManager
                )
            }
        }
    }
}
