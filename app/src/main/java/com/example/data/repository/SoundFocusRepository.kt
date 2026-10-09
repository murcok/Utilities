package com.example.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.data.local.SettingsManager
import com.example.data.local.SoundFocusDao
import com.example.data.model.AudioProfile
import com.example.data.model.CallPriorityMode
import com.example.data.model.CallSignalMode
import com.example.data.model.FocusEventType
import com.example.data.model.FocusLog
import com.example.data.model.FocusSettings
import com.example.data.model.MonitoredApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class SoundFocusRepository(
    private val context: Context,
    private val dao: SoundFocusDao,
    private val settingsManager: SettingsManager
) {
    val monitoredAppsFlow: Flow<List<MonitoredApp>> = dao.getAllMonitoredAppsFlow()
    val allProfilesFlow: Flow<List<AudioProfile>> = dao.getAllProfilesFlow()
    val recentLogsFlow: Flow<List<FocusLog>> = dao.getRecentLogsFlow()
    val settingsFlow: StateFlow<FocusSettings> = settingsManager.settings

    suspend fun initializeDefaultsIfNeeded() = withContext(Dispatchers.IO) {
        initializeDefaultAppsIfNeeded()
        initializeDefaultProfilesIfNeeded()
    }

    private suspend fun initializeDefaultProfilesIfNeeded() {
        val count = dao.getProfilesCount()
        if (count == 0) {
            val defaultProfiles = listOf(
                AudioProfile(
                    id = "profile_music_default",
                    name = "Profilo Musica",
                    description = "Silenzia i messaggi durante l'ascolto di musica e lascia squillare tutte le chiamate.",
                    iconName = "headphones",
                    isDefaultMusicProfile = true,
                    isAutoTriggerOnMusic = true,
                    silenceMessages = true,
                    callSignalMode = CallSignalMode.RING_AND_VIBRATE,
                    callPriorityMode = CallPriorityMode.ANY_CALLER,
                    allowAlarms = true,
                    isManuallyActive = false,
                    isAutoActive = false
                ),
                AudioProfile(
                    id = "profile_focus",
                    name = "Profilo Studio & Lavoro",
                    description = "Messaggi silenziati per massima concentrazione; squillano solo contatti importanti.",
                    iconName = "work",
                    isDefaultMusicProfile = false,
                    isAutoTriggerOnMusic = false,
                    silenceMessages = true,
                    callSignalMode = CallSignalMode.RING_AND_VIBRATE,
                    callPriorityMode = CallPriorityMode.STARRED_ONLY,
                    allowAlarms = true,
                    isManuallyActive = false,
                    isAutoActive = false
                ),
                AudioProfile(
                    id = "profile_drive",
                    name = "Profilo Guida Sicura",
                    description = "Nessuna distrazione da notifiche chat; tutte le chiamate danno squillo forte.",
                    iconName = "car",
                    isDefaultMusicProfile = false,
                    isAutoTriggerOnMusic = false,
                    silenceMessages = true,
                    callSignalMode = CallSignalMode.RING_AND_VIBRATE,
                    callPriorityMode = CallPriorityMode.ANY_CALLER,
                    allowAlarms = true,
                    isManuallyActive = false,
                    isAutoActive = false
                )
            )
            dao.insertProfiles(defaultProfiles)
        }
    }

    suspend fun initializeDefaultAppsIfNeeded() {
        val count = dao.getMonitoredAppsCount()
        if (count == 0) {
            val defaults = listOf(
                MonitoredApp(
                    packageName = "com.spotify.music",
                    appName = "Spotify",
                    isEnabled = true,
                    isKnownMusicApp = true,
                    isInstalled = isPackageInstalled("com.spotify.music")
                ),
                MonitoredApp(
                    packageName = "com.google.android.apps.youtube.music",
                    appName = "YouTube Music",
                    isEnabled = true,
                    isKnownMusicApp = true,
                    isInstalled = isPackageInstalled("com.google.android.apps.youtube.music")
                ),
                MonitoredApp(
                    packageName = "com.apple.android.music",
                    appName = "Apple Music",
                    isEnabled = true,
                    isKnownMusicApp = true,
                    isInstalled = isPackageInstalled("com.apple.android.music")
                ),
                MonitoredApp(
                    packageName = "deezer.android.app",
                    appName = "Deezer",
                    isEnabled = true,
                    isKnownMusicApp = true,
                    isInstalled = isPackageInstalled("deezer.android.app")
                ),
                MonitoredApp(
                    packageName = "com.amazon.mp3",
                    appName = "Amazon Music",
                    isEnabled = true,
                    isKnownMusicApp = true,
                    isInstalled = isPackageInstalled("com.amazon.mp3")
                ),
                MonitoredApp(
                    packageName = "com.aspiro.tidal",
                    appName = "Tidal",
                    isEnabled = true,
                    isKnownMusicApp = true,
                    isInstalled = isPackageInstalled("com.aspiro.tidal")
                ),
                MonitoredApp(
                    packageName = "com.soundcloud.android",
                    appName = "SoundCloud",
                    isEnabled = true,
                    isKnownMusicApp = true,
                    isInstalled = isPackageInstalled("com.soundcloud.android")
                ),
                MonitoredApp(
                    packageName = "org.videolan.vlc",
                    appName = "VLC",
                    isEnabled = false,
                    isKnownMusicApp = true,
                    isInstalled = isPackageInstalled("org.videolan.vlc")
                ),
                MonitoredApp(
                    packageName = "au.com.shiftyjelly.pocketcasts",
                    appName = "Pocket Casts",
                    isEnabled = false,
                    isKnownMusicApp = true,
                    isInstalled = isPackageInstalled("au.com.shiftyjelly.pocketcasts")
                )
            )
            dao.insertApps(defaults)
        } else {
            val current = dao.getEnabledApps()
            for (app in current) {
                val installed = isPackageInstalled(app.packageName)
                if (installed != app.isInstalled) {
                    dao.updateApp(app.copy(isInstalled = installed))
                }
            }
        }
    }

    suspend fun getMusicTriggerProfile(): AudioProfile = withContext(Dispatchers.IO) {
        dao.getMusicTriggerProfile()
            ?: dao.getDefaultMusicProfile()
            ?: AudioProfile(
                id = "profile_music_default",
                name = "Profilo Musica",
                description = "Silenzia messaggi e lascia squillare le chiamate",
                isDefaultMusicProfile = true,
                isAutoTriggerOnMusic = true
            )
    }

    suspend fun saveProfile(profile: AudioProfile) = withContext(Dispatchers.IO) {
        dao.insertProfile(profile)
    }

    suspend fun updateProfileAutoActive(profileId: String, isAutoActive: Boolean) = withContext(Dispatchers.IO) {
        val p = dao.getProfileById(profileId)
        if (p != null) {
            dao.updateProfile(p.copy(isAutoActive = isAutoActive))
        }
    }

    suspend fun updateProfileManualActive(profileId: String, isManualActive: Boolean) = withContext(Dispatchers.IO) {
        val p = dao.getProfileById(profileId)
        if (p != null) {
            if (isManualActive) {
                // Deactivate other manual profiles first to ensure single active profile
                dao.deactivateAllProfiles()
            }
            dao.updateProfile(p.copy(isManuallyActive = isManualActive))
        }
    }

    suspend fun deactivateAllProfiles() = withContext(Dispatchers.IO) {
        dao.deactivateAllProfiles()
    }

    suspend fun deleteProfile(profile: AudioProfile) = withContext(Dispatchers.IO) {
        dao.deleteProfile(profile)
    }

    suspend fun getInstalledMediaApps(): List<MonitoredApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val result = mutableListOf<MonitoredApp>()

        for (info in installedApps) {
            val isSystemApp = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val isUserApp = !isSystemApp || isUpdatedSystem

            val pkg = info.packageName
            val label = pm.getApplicationLabel(info).toString()

            val isAudioApp = (info.category == ApplicationInfo.CATEGORY_AUDIO) ||
                    pkg.contains("music", ignoreCase = true) ||
                    pkg.contains("audio", ignoreCase = true) ||
                    pkg.contains("podcast", ignoreCase = true) ||
                    pkg.contains("player", ignoreCase = true) ||
                    pkg.contains("sound", ignoreCase = true) ||
                    pkg.contains("spotify", ignoreCase = true)

            if (isUserApp || isAudioApp) {
                val existing = dao.getAppByPackage(pkg)
                result.add(
                    existing ?: MonitoredApp(
                        packageName = pkg,
                        appName = label,
                        isEnabled = false,
                        isKnownMusicApp = isAudioApp,
                        isInstalled = true
                    )
                )
            }
        }
        result.sortedByDescending { it.isEnabled }
    }

    suspend fun toggleAppEnabled(app: MonitoredApp) = withContext(Dispatchers.IO) {
        dao.insertOrUpdateApp(app.copy(isEnabled = !app.isEnabled))
    }

    suspend fun addCustomApp(packageName: String, appName: String) = withContext(Dispatchers.IO) {
        dao.insertOrUpdateApp(
            MonitoredApp(
                packageName = packageName,
                appName = appName,
                isEnabled = true,
                isKnownMusicApp = false,
                isInstalled = isPackageInstalled(packageName)
            )
        )
    }

    suspend fun deleteApp(app: MonitoredApp) = withContext(Dispatchers.IO) {
        dao.deleteApp(app)
    }

    suspend fun logEvent(
        eventType: FocusEventType,
        appName: String,
        packageName: String,
        details: String
    ) = withContext(Dispatchers.IO) {
        dao.insertLog(
            FocusLog(
                timestamp = System.currentTimeMillis(),
                appName = appName,
                packageName = packageName,
                eventType = eventType,
                details = details
            )
        )
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        dao.clearAllLogs()
    }

    fun updateSettings(update: (FocusSettings) -> FocusSettings) {
        settingsManager.updateSettings(update)
    }

    fun getSettings(): FocusSettings = settingsManager.getSettings()

    suspend fun isAppMonitored(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val app = dao.getAppByPackage(packageName)
        app?.isEnabled == true
    }

    suspend fun getAppName(packageName: String): String = withContext(Dispatchers.IO) {
        val app = dao.getAppByPackage(packageName)
        if (app != null) return@withContext app.appName
        try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
