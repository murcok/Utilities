package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsManager
import com.example.data.local.SoundFocusDatabase
import com.example.data.model.AudioProfile
import com.example.data.model.CallPriorityMode
import com.example.data.model.CallSignalMode
import com.example.data.model.FocusLog
import com.example.data.model.FocusSettings
import com.example.data.model.MonitoredApp
import com.example.data.repository.SoundFocusRepository
import com.example.service.PlaybackFocusState
import com.example.service.PlaybackStateCoordinator
import com.example.service.SoundFocusService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class SoundFocusUiState(
    val monitoredApps: List<MonitoredApp> = emptyList(),
    val profiles: List<AudioProfile> = emptyList(),
    val installedApps: List<MonitoredApp> = emptyList(),
    val recentLogs: List<FocusLog> = emptyList(),
    val settings: FocusSettings = FocusSettings(),
    val playbackFocusState: PlaybackFocusState = PlaybackFocusState(),
    val isDndPermissionGranted: Boolean = false,
    val isNotificationListenerGranted: Boolean = false,
    val isPostNotificationGranted: Boolean = true,
    val searchQuery: String = "",
    val isLoadingApps: Boolean = false
)

private data class BaseData(
    val monitored: List<MonitoredApp>,
    val profiles: List<AudioProfile>,
    val logs: List<FocusLog>,
    val settings: FocusSettings,
    val playback: PlaybackFocusState
)

data class SystemPermissions(
    val isDndGranted: Boolean,
    val isListenerGranted: Boolean,
    val isPostNotificationGranted: Boolean
)

class SoundFocusViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val dao = SoundFocusDatabase.getInstance(context).soundFocusDao()
    private val settingsManager = SettingsManager.getInstance(context)
    private val repository = SoundFocusRepository(context, dao, settingsManager)
    private val coordinator = PlaybackStateCoordinator.getInstance(context)

    private val _searchQuery = MutableStateFlow("")
    private val _installedApps = MutableStateFlow<List<MonitoredApp>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(false)
    private val _permissionsState = MutableStateFlow(checkPermissions())

    private val baseDataFlow = combine(
        repository.monitoredAppsFlow,
        repository.allProfilesFlow,
        repository.recentLogsFlow,
        repository.settingsFlow,
        coordinator.focusState
    ) { monitored, profiles, logs, settings, playback ->
        BaseData(monitored, profiles, logs, settings, playback)
    }

    val uiState: StateFlow<SoundFocusUiState> = combine(
        baseDataFlow,
        _searchQuery,
        _installedApps,
        _permissionsState,
        _isLoadingApps
    ) { base, query, installed, perms, loading ->
        SoundFocusUiState(
            monitoredApps = base.monitored,
            profiles = base.profiles,
            installedApps = installed,
            recentLogs = base.logs,
            settings = base.settings,
            playbackFocusState = base.playback,
            isDndPermissionGranted = perms.isDndGranted,
            isNotificationListenerGranted = perms.isListenerGranted,
            isPostNotificationGranted = perms.isPostNotificationGranted,
            searchQuery = query,
            isLoadingApps = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SoundFocusUiState()
    )

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
            loadInstalledApps()
            checkAndSyncService()
        }
    }

    fun refreshPermissions() {
        _permissionsState.value = checkPermissions()
    }

    private fun checkPermissions(): SystemPermissions {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val dndGranted = nm.isNotificationPolicyAccessGranted

        val pkgName = context.packageName
        val listeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        val listenerGranted = listeners.contains(pkgName)

        val postGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true
        }

        return SystemPermissions(
            isDndGranted = dndGranted,
            isListenerGranted = listenerGranted,
            isPostNotificationGranted = postGranted
        )
    }

    fun checkAndSyncService() {
        val settings = repository.getSettings()
        if (settings.isMasterEnabled) {
            SoundFocusService.start(context)
        } else {
            SoundFocusService.stop(context)
        }
    }

    fun toggleMaster(enabled: Boolean) {
        repository.updateSettings { it.copy(isMasterEnabled = enabled) }
        checkAndSyncService()
    }

    fun toggleManualProfile(profile: AudioProfile) {
        coordinator.toggleManualProfile(profile)
    }

    fun saveProfile(profile: AudioProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
        }
    }

    fun createNewProfile(
        name: String,
        description: String,
        iconName: String,
        silenceMessages: Boolean,
        callSignalMode: CallSignalMode,
        callPriorityMode: CallPriorityMode,
        isAutoTriggerOnMusic: Boolean
    ) {
        viewModelScope.launch {
            val newProfile = AudioProfile(
                id = "profile_${UUID.randomUUID().toString().take(8)}",
                name = name,
                description = description,
                iconName = iconName,
                isDefaultMusicProfile = false,
                isAutoTriggerOnMusic = isAutoTriggerOnMusic,
                silenceMessages = silenceMessages,
                callSignalMode = callSignalMode,
                callPriorityMode = callPriorityMode,
                allowAlarms = true
            )
            repository.saveProfile(newProfile)
        }
    }

    fun deleteProfile(profile: AudioProfile) {
        viewModelScope.launch {
            if (!profile.isDefaultMusicProfile) {
                repository.deleteProfile(profile)
            }
        }
    }

    fun setAsMusicTriggerProfile(profile: AudioProfile) {
        viewModelScope.launch {
            // Unset other auto trigger profiles
            val all = uiState.value.profiles
            for (p in all) {
                if (p.id != profile.id && p.isAutoTriggerOnMusic) {
                    repository.saveProfile(p.copy(isAutoTriggerOnMusic = false))
                }
            }
            repository.saveProfile(profile.copy(isAutoTriggerOnMusic = true))
        }
    }

    fun toggleApp(app: MonitoredApp) {
        viewModelScope.launch {
            repository.toggleAppEnabled(app)
        }
    }

    fun deleteApp(app: MonitoredApp) {
        viewModelScope.launch {
            repository.deleteApp(app)
        }
    }

    fun addCustomApp(pkg: String, name: String) {
        viewModelScope.launch {
            repository.addCustomApp(pkg, name)
            loadInstalledApps()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = repository.getInstalledMediaApps()
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }

    fun updateCallPriority(mode: CallPriorityMode) {
        repository.updateSettings { it.copy(callPriorityMode = mode) }
    }

    fun updateMuteNotificationStream(muted: Boolean) {
        repository.updateSettings { it.copy(muteNotificationStream = muted) }
    }

    fun updateGracePeriod(seconds: Int) {
        repository.updateSettings { it.copy(gracePeriodSeconds = seconds) }
    }

    fun updateAllowAlarms(allow: Boolean) {
        repository.updateSettings { it.copy(allowAlarms = allow) }
    }

    fun toggleSimulation(targetApp: MonitoredApp?) {
        val pkg = targetApp?.packageName ?: "com.spotify.music"
        val name = targetApp?.appName ?: "Spotify"
        coordinator.toggleSimulation(pkg, name)
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }
}
