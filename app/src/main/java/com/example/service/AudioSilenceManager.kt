package com.example.service

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.example.data.model.AudioProfile
import com.example.data.model.CallPriorityMode
import com.example.data.model.CallSignalMode

class AudioSilenceManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var originalInterruptionFilter: Int? = null
    private var originalNotificationVolume: Int? = null
    private var isSilenceActive: Boolean = false

    fun isNotificationPolicyAccessGranted(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    /**
     * Activates audio silence for messages while ensuring phone calls produce a signal.
     * CRITICAL: AudioManager.STREAM_MUSIC is protected and NEVER muted or locked.
     */
    @Synchronized
    fun activateProfileSilence(profile: AudioProfile, sourceReason: String): Boolean {
        try {
            // Read and safeguard the user's media music volume
            val userMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            // 1. Save current notification volume BEFORE muting, if not already saved
            if (originalNotificationVolume == null) {
                val currentNotifVol = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
                originalNotificationVolume = if (currentNotifVol > 0) {
                    currentNotifVol
                } else {
                    (audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) / 2).coerceAtLeast(2)
                }
            }

            // 2. Configure Do Not Disturb Priority Filter
            // CRITICAL: Must explicitly include PRIORITY_CATEGORY_MEDIA and PRIORITY_CATEGORY_SYSTEM
            // Otherwise Android suppresses media audio to 0 and locks the media volume slider!
            if (isNotificationPolicyAccessGranted()) {
                if (originalInterruptionFilter == null) {
                    originalInterruptionFilter = notificationManager.currentInterruptionFilter
                }

                // Explicitly ALLOW Media, Calls, Alarms, System sounds
                var categories = NotificationManager.Policy.PRIORITY_CATEGORY_CALLS
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // MUST BE INCLUDED so Android does not mute or lock media playback volume
                    categories = categories or NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA
                    categories = categories or NotificationManager.Policy.PRIORITY_CATEGORY_SYSTEM
                }

                if (profile.allowAlarms) {
                    categories = categories or NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS
                }
                if (profile.callPriorityMode == CallPriorityMode.REPEAT_CALLERS) {
                    categories = categories or NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS
                }

                val callSenders = when (profile.callPriorityMode) {
                    CallPriorityMode.ANY_CALLER, CallPriorityMode.REPEAT_CALLERS ->
                        NotificationManager.Policy.PRIORITY_SENDERS_ANY
                    CallPriorityMode.CONTACTS_ONLY ->
                        NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS
                    CallPriorityMode.STARRED_ONLY ->
                        NotificationManager.Policy.PRIORITY_SENDERS_STARRED
                }

                // Explicitly OMIT PRIORITY_CATEGORY_MESSAGES so chat & message notifications are silenced
                val policy = NotificationManager.Policy(
                    categories,
                    callSenders,
                    NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS
                )

                notificationManager.notificationPolicy = policy
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                Log.d(TAG, "Applied Profile DND: Media allowed (PRIORITY_CATEGORY_MEDIA), Calls allowed, Messages silenced. Reason: $sourceReason")
            }

            // 3. Mute notification stream specifically
            if (profile.silenceMessages) {
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set STREAM_NOTIFICATION volume: ${e.message}")
                }
            }

            // 4. Double check that STREAM_MUSIC was NOT touched or lowered by Android
            val currentMusicVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (currentMusicVol == 0 && userMusicVolume > 0) {
                Log.w(TAG, "STREAM_MUSIC was affected by system DND, immediately restoring to $userMusicVolume")
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, userMusicVolume, 0)
            }

            // 5. Ensure phone calls have an active signal (STREAM_RING preserved / audible)
            val ringVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
            if (ringVol == 0 && profile.callSignalMode != CallSignalMode.VIBRATE_ONLY) {
                val halfMaxRing = (audioManager.getStreamMaxVolume(AudioManager.STREAM_RING) / 2).coerceAtLeast(3)
                audioManager.setStreamVolume(AudioManager.STREAM_RING, halfMaxRing, 0)
            }

            isSilenceActive = true
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to activate profile silence: ${e.message}", e)
            return false
        }
    }

    /**
     * Deactivates silence and restores notification volumes immediately upon pause/stop.
     * CRITICAL: STREAM_MUSIC volume remains strictly untouched.
     */
    @Synchronized
    fun deactivateSilence(): Boolean {
        if (!isSilenceActive && originalNotificationVolume == null) {
            return true
        }

        try {
            // Save current music volume so it NEVER changes during restore
            val musicVolBeforeRestore = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            // 1. Immediately restore notification volume
            originalNotificationVolume?.let { savedVol ->
                val targetVol = savedVol.coerceAtLeast(1)
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, targetVol, 0)
                Log.d(TAG, "Restored STREAM_NOTIFICATION volume to $targetVol immediately upon pause")
                originalNotificationVolume = null
            }

            // 2. Restore system Do Not Disturb filter
            if (isNotificationPolicyAccessGranted()) {
                val restoreFilter = originalInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
                notificationManager.setInterruptionFilter(
                    if (restoreFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
                        NotificationManager.INTERRUPTION_FILTER_ALL
                    } else {
                        restoreFilter
                    }
                )
                originalInterruptionFilter = null
                Log.d(TAG, "Restored interruption filter to normal")
            }

            // 3. Confirm STREAM_MUSIC was not altered
            val musicVolAfterRestore = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (musicVolAfterRestore != musicVolBeforeRestore && musicVolBeforeRestore > 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, musicVolBeforeRestore, 0)
            }

            isSilenceActive = false
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deactivate silence: ${e.message}", e)
            return false
        }
    }

    fun isCurrentlySilenced(): Boolean = isSilenceActive

    companion object {
        private const val TAG = "AudioSilenceManager"
    }
}
