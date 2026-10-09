package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AppSelectorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.ProfilesScreen
import com.example.ui.screens.RulesConfigScreen
import com.example.ui.theme.SoundFocusTheme
import com.example.ui.viewmodel.SoundFocusViewModel

enum class SoundFocusTab(val title: String) {
    DASHBOARD("Home"),
    PROFILES("Profili"),
    APPS("App"),
    RULES("Regole"),
    LOGS("Registro")
}

class MainActivity : ComponentActivity() {

    private val viewModel: SoundFocusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SoundFocusTheme {
                SoundFocusApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SoundFocusApp(viewModel: SoundFocusViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(SoundFocusTab.DASHBOARD) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle back button when on sub-tabs
    BackHandler(enabled = currentTab != SoundFocusTab.DASHBOARD) {
        currentTab = SoundFocusTab.DASHBOARD
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                NavigationBarItem(
                    selected = currentTab == SoundFocusTab.DASHBOARD,
                    onClick = { currentTab = SoundFocusTab.DASHBOARD },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == SoundFocusTab.PROFILES,
                    onClick = { currentTab = SoundFocusTab.PROFILES },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Profili"
                        )
                    },
                    label = { Text("Profili") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == SoundFocusTab.APPS,
                    onClick = { currentTab = SoundFocusTab.APPS },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "App"
                        )
                    },
                    label = { Text("App") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == SoundFocusTab.RULES,
                    onClick = { currentTab = SoundFocusTab.RULES },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Regole"
                        )
                    },
                    label = { Text("Regole") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == SoundFocusTab.LOGS,
                    onClick = { currentTab = SoundFocusTab.LOGS },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Registro"
                        )
                    },
                    label = { Text("Registro") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentTab) {
                SoundFocusTab.DASHBOARD -> {
                    DashboardScreen(
                        uiState = uiState,
                        onToggleMaster = { viewModel.toggleMaster(it) },
                        onToggleApp = { viewModel.toggleApp(it) },
                        onToggleManualProfile = { viewModel.toggleManualProfile(it) },
                        onToggleSimulation = { viewModel.toggleSimulation(it) },
                        onNavigateToApps = { currentTab = SoundFocusTab.APPS },
                        onNavigateToProfiles = { currentTab = SoundFocusTab.PROFILES },
                        onNavigateToLogs = { currentTab = SoundFocusTab.LOGS },
                        onRefreshPermissions = { viewModel.refreshPermissions() }
                    )
                }

                SoundFocusTab.PROFILES -> {
                    ProfilesScreen(
                        uiState = uiState,
                        onToggleManualProfile = { viewModel.toggleManualProfile(it) },
                        onSaveProfile = { viewModel.saveProfile(it) },
                        onDeleteProfile = { viewModel.deleteProfile(it) },
                        onCreateProfile = { name, desc, icon, silence, signal, priority, autoTrigger ->
                            viewModel.createNewProfile(name, desc, icon, silence, signal, priority, autoTrigger)
                        }
                    )
                }

                SoundFocusTab.APPS -> {
                    AppSelectorScreen(
                        uiState = uiState,
                        onToggleApp = { viewModel.toggleApp(it) },
                        onDeleteApp = { viewModel.deleteApp(it) },
                        onAddCustomApp = { pkg, name -> viewModel.addCustomApp(pkg, name) },
                        onSearchQueryChanged = { viewModel.setSearchQuery(it) }
                    )
                }

                SoundFocusTab.RULES -> {
                    RulesConfigScreen(
                        settings = uiState.settings,
                        onCallPriorityChanged = { viewModel.updateCallPriority(it) },
                        onMuteNotificationStreamChanged = { viewModel.updateMuteNotificationStream(it) },
                        onAllowAlarmsChanged = { viewModel.updateAllowAlarms(it) },
                        onGracePeriodChanged = { viewModel.updateGracePeriod(it) }
                    )
                }

                SoundFocusTab.LOGS -> {
                    LogsScreen(
                        logs = uiState.recentLogs,
                        onClearLogs = { viewModel.clearLogs() }
                    )
                }
            }
        }
    }
}
