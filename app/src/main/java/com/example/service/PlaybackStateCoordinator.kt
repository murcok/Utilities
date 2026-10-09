package com.example.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.local.SettingsManager
import com.example.data.local.SoundFocusDatabase
import com.example.data.model.AudioProfile
import com.example.data.model.FocusEventType
import com.example.data.repository.SoundFocusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ActivationSource {
    NONE,
    AUTO_MUSIC_APP,
    MANUAL_USER,
    SIMULATION
}

data class PlaybackFocusState(
    val isMusicPlaying: Boolean = false,
    val activePackageName: String? = null,
    val activeAppName: String? = null,
    val trackTitle: String? = null,
    val isSilenceActive: Boolean = false,
    val isSimulationActive: Boolean = false,
    val isManualProfileActive: Boolean = false,
    val activeProfileId: String? = null,
    val activeProfileName: String? = null,
    val activationSource: ActivationSource = ActivationSource.NONE,
    val lastEventTime: Long = System.currentTimeMillis()
)

class PlaybackStateCoordinator private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val audioSilenceManager = AudioSilenceManager(context)
    private val repository = SoundFocusRepository(
        context = context,
        dao = SoundFocusDatabase.getInstance(context).soundFocusDao(),
        settingsManager = SettingsManager.getInstance(context)
    )

    private val _focusState = MutableStateFlow(PlaybackFocusState())
    val focusState: StateFlow<PlaybackFocusState> = _focusState.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Triggered automatically when music starts playing in a monitored app.
     */
    fun onPlaybackStarted(packageName: String, title: String? = null) {
        scope.launch {
            val settings = repository.getSettings()
            if (!settings.isMasterEnabled) {
                Log.d(TAG, "Master switch is disabled, ignoring playback from $packageName")
                return@launch
            }

            // Check if this app is in the enabled list
            val isMonitored = repository.isAppMonitored(packageName)
            if (!isMonitored) {
                Log.d(TAG, "App $packageName is not monitored, ignoring")
                return@launch
            }

            val appName = repository.getAppName(packageName)
            val profile = repository.getMusicTriggerProfile()

            // Activate silence based on profile rules
            val success = audioSilenceManager.activateProfileSilence(
                profile = profile,
                sourceReason = "Riproduzione avviata da $appName"
            )

            // Update profile state in db
            repository.updateProfileAutoActive(profile.id, true)

            _focusState.value = PlaybackFocusState(
                isMusicPlaying = true,
                activePackageName = packageName,
                activeAppName = appName,
                trackTitle = title,
                isSilenceActive = success,
                isSimulationActive = false,
                isManualProfileActive = false,
                activeProfileId = profile.id,
                activeProfileName = profile.name,
                activationSource = ActivationSource.AUTO_MUSIC_APP,
                lastEventTime = System.currentTimeMillis()
            )

            repository.logEvent(
                eventType = FocusEventType.PLAYBACK_DETECTED,
                appName = appName,
                packageName = packageName,
                details = "${profile.name} attivato automaticamente da $appName${if (!title.isNullOrBlank()) " ($title)" else ""}. Messaggi silenziati, chiamate attive."
            )
        }
    }

    /**
     * Triggered as soon as the music app goes into pause or stops playing.
     * RESTORES NOTIFICATION VOLUMES IMMEDIATELY while leaving media playback volume strictly unchanged!
     */
    fun onPlaybackPausedOrStopped(packageName: String) {
        scope.launch {
            val current = _focusState.value
            if (current.isSimulationActive || current.isManualProfileActive) {
                // If manually forced active or in simulation, don't stop via player pause
                return@launch
            }

            if (current.activePackageName != packageName && current.isMusicPlaying) {
                return@launch
            }

            val appName = current.activeAppName ?: packageName
            val profileId = current.activeProfileId

            // IMMEDIATELY restore notification volume and normal DND filter
            audioSilenceManager.deactivateSilence()

            if (profileId != null) {
                repository.updateProfileAutoActive(profileId, false)
            }

            _focusState.value = PlaybackFocusState(
                isMusicPlaying = false,
                activePackageName = null,
                activeAppName = null,
                trackTitle = null,
                isSilenceActive = false,
                isSimulationActive = false,
                isManualProfileActive = false,
                activeProfileId = null,
                activeProfileName = null,
                activationSource = ActivationSource.NONE,
                lastEventTime = System.currentTimeMillis()
            )

            repository.logEvent(
                eventType = FocusEventType.PLAYBACK_STOPPED,
                appName = appName,
                packageName = packageName,
                details = "Pausa musica da $appName: Volumi notifiche ripristinati immediatamente. Volume musica inalterato."
            )
        }
    }

    /**
     * Manually toggles a profile on or off at the user's discretion.
     */
    fun toggleManualProfile(profile: AudioProfile) {
        scope.launch {
            val current = _focusState.value

            if (current.isManualProfileActive && current.activeProfileId == profile.id) {
                // Deactivate
                audioSilenceManager.deactivateSilence()
                repository.updateProfileManualActive(profile.id, false)

                _focusState.value = PlaybackFocusState(
                    isMusicPlaying = false,
                    activePackageName = null,
                    activeAppName = null,
                    trackTitle = null,
                    isSilenceActive = false,
                    isSimulationActive = false,
                    isManualProfileActive = false,
                    activeProfileId = null,
                    activeProfileName = null,
                    activationSource = ActivationSource.NONE,
                    lastEventTime = System.currentTimeMillis()
                )

                repository.logEvent(
                    eventType = FocusEventType.MANUAL_OVERRIDE,
                    appName = profile.name,
                    packageName = "com.example.soundfocus",
                    details = "${profile.name} disattivato manualmente. Volumi normali ripristinati."
                )
            } else {
                // Activate
                val success = audioSilenceManager.activateProfileSilence(
                    profile = profile,
                    sourceReason = "Attivazione manuale utente"
                )
                repository.updateProfileManualActive(profile.id, true)

                _focusState.value = PlaybackFocusState(
                    isMusicPlaying = current.isMusicPlaying,
                    activePackageName = current.activePackageName,
                    activeAppName = current.activeAppName,
                    trackTitle = current.trackTitle,
                    isSilenceActive = success,
                    isSimulationActive = false,
                    isManualProfileActive = true,
                    activeProfileId = profile.id,
                    activeProfileName = profile.name,
                    activationSource = ActivationSource.MANUAL_USER,
                    lastEventTime = System.currentTimeMillis()
                )

                repository.logEvent(
                    eventType = FocusEventType.MANUAL_OVERRIDE,
                    appName = profile.name,
                    packageName = "com.example.soundfocus",
                    details = "${profile.name} attivato manualmente. Messaggi silenziati, chiamate attive."
                )
            }
        }
    }

    fun toggleSimulation(targetAppPackage: String = "com.spotify.music", targetAppName: String = "Spotify") {
        scope.launch {
            val currentState = _focusState.value
            val profile = repository.getMusicTriggerProfile()

            if (currentState.isSimulationActive) {
                audioSilenceManager.deactivateSilence()
                _focusState.value = PlaybackFocusState(
                    isMusicPlaying = false,
                    activePackageName = null,
                    activeAppName = null,
                    trackTitle = null,
                    isSilenceActive = false,
                    isSimulationActive = false,
                    isManualProfileActive = false,
                    activeProfileId = null,
                    activeProfileName = null,
                    activationSource = ActivationSource.NONE,
                    lastEventTime = System.currentTimeMillis()
                )
                repository.logEvent(
                    eventType = FocusEventType.SIMULATION_STOP,
                    appName = targetAppName,
                    packageName = targetAppPackage,
                    details = "Simulazione interrotta: volume notifiche ripristinato immediatamente."
                )
            } else {
                val success = audioSilenceManager.activateProfileSilence(
                    profile = profile,
                    sourceReason = "Simulazione riproduzione $targetAppName"
                )
                _focusState.value = PlaybackFocusState(
                    isMusicPlaying = true,
                    activePackageName = targetAppPackage,
                    activeAppName = targetAppName,
                    trackTitle = "Simulazione: Traccia di Test",
                    isSilenceActive = success,
                    isSimulationActive = true,
                    isManualProfileActive = false,
                    activeProfileId = profile.id,
                    activeProfileName = profile.name,
                    activationSource = ActivationSource.SIMULATION,
                    lastEventTime = System.currentTimeMillis()
                )
                repository.logEvent(
                    eventType = FocusEventType.SIMULATION_START,
                    appName = targetAppName,
                    packageName = targetAppPackage,
                    details = "Simulazione avviata con ${profile.name}. Messaggi silenziati, chiamate attive."
                )
            }
        }
    }

    fun onMessageNotificationIntercepted(sourceAppName: String, sourcePackage: String) {
        scope.launch {
            if (_focusState.value.isSilenceActive) {
                repository.logEvent(
                    eventType = FocusEventType.MESSAGE_SILENCED,
                    appName = sourceAppName,
                    packageName = sourcePackage,
                    details = "Messaggio silenzioso soppresso da $sourceAppName (Profilo attivo: ${_focusState.value.activeProfileName ?: "Musica"})."
                )
            }
        }
    }

    fun onCallNotificationDetected(callerName: String?) {
        scope.launch {
            if (_focusState.value.isSilenceActive) {
                repository.logEvent(
                    eventType = FocusEventType.CALL_ALLOWED,
                    appName = "Telefono",
                    packageName = "com.android.server.telecom",
                    details = "Chiamata in arrivo consentita${if (!callerName.isNullOrBlank()) " da $callerName" else ""}: squillo attivo e percepibile!"
                )
            }
        }
    }

    companion object {
        private const val TAG = "PlaybackCoordinator"

        @Volatile
        private var INSTANCE: PlaybackStateCoordinator? = null

        fun getInstance(context: Context): PlaybackStateCoordinator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlaybackStateCoordinator(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
