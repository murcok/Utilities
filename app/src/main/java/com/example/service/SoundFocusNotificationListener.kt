package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class SoundFocusNotificationListener : NotificationListenerService() {

    private lateinit var coordinator: PlaybackStateCoordinator
    private var mediaSessionManager: MediaSessionManager? = null
    private val activeControllers = mutableMapOf<String, MediaController>()
    private val controllerCallbacks = mutableMapOf<String, MediaController.Callback>()

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            updateControllers(controllers)
        }

    override fun onCreate() {
        super.onCreate()
        coordinator = PlaybackStateCoordinator.getInstance(this)
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        Log.d(TAG, "SoundFocusNotificationListener created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListener connected")
        registerMediaSessionListener()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "NotificationListener disconnected")
        unregisterMediaSessionListener()
    }

    private fun registerMediaSessionListener() {
        try {
            val componentName = ComponentName(this, SoundFocusNotificationListener::class.java)
            mediaSessionManager?.let { mgr ->
                mgr.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
                val initialSessions = mgr.getActiveSessions(componentName)
                updateControllers(initialSessions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register active sessions listener: ${e.message}", e)
        }
    }

    private fun unregisterMediaSessionListener() {
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            clearControllerCallbacks()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister sessions listener: ${e.message}", e)
        }
    }

    private fun clearControllerCallbacks() {
        for ((pkg, callback) in controllerCallbacks) {
            activeControllers[pkg]?.unregisterCallback(callback)
        }
        controllerCallbacks.clear()
        activeControllers.clear()
    }

    private fun updateControllers(controllers: List<MediaController>?) {
        if (controllers == null) return

        val newPackages = controllers.map { it.packageName }.toSet()
        val toRemove = activeControllers.keys.filter { it !in newPackages }

        for (pkg in toRemove) {
            controllerCallbacks[pkg]?.let { activeControllers[pkg]?.unregisterCallback(it) }
            controllerCallbacks.remove(pkg)
            activeControllers.remove(pkg)
            coordinator.onPlaybackPausedOrStopped(pkg)
        }

        for (controller in controllers) {
            val pkg = controller.packageName
            if (!activeControllers.containsKey(pkg)) {
                activeControllers[pkg] = controller

                val callback = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        handlePlaybackState(pkg, state, controller)
                    }

                    override fun onSessionDestroyed() {
                        coordinator.onPlaybackPausedOrStopped(pkg)
                    }
                }

                controller.registerCallback(callback)
                controllerCallbacks[pkg] = callback

                // Check initial state
                handlePlaybackState(pkg, controller.playbackState, controller)
            } else {
                handlePlaybackState(pkg, controller.playbackState, controller)
            }
        }
    }

    private fun handlePlaybackState(pkg: String, state: PlaybackState?, controller: MediaController) {
        if (state == null) return
        val isPlaying = state.state == PlaybackState.STATE_PLAYING

        if (isPlaying) {
            val title = controller.metadata?.description?.title?.toString()
            coordinator.onPlaybackStarted(pkg, title)
        } else {
            coordinator.onPlaybackPausedOrStopped(pkg)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val notification = sbn.notification ?: return
        val pkg = sbn.packageName

        // Don't intercept own notifications or system telecom call alerts
        if (pkg == packageName) return

        // 1. Detect call notifications
        val category = notification.category
        if (category == Notification.CATEGORY_CALL) {
            val caller = notification.extras?.getString(Notification.EXTRA_TITLE)
            coordinator.onCallNotificationDetected(caller)
            return
        }

        // 2. Detect messaging notifications
        val isMessage = category == Notification.CATEGORY_MESSAGE ||
                category == Notification.CATEGORY_SOCIAL ||
                pkg.contains("whatsapp", ignoreCase = true) ||
                pkg.contains("telegram", ignoreCase = true) ||
                pkg.contains("messaging", ignoreCase = true) ||
                pkg.contains("messenger", ignoreCase = true) ||
                pkg.contains("signal", ignoreCase = true) ||
                pkg.contains("viber", ignoreCase = true) ||
                pkg.contains("discord", ignoreCase = true)

        if (isMessage && coordinator.focusState.value.isSilenceActive) {
            val appLabel = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
            } catch (_: Exception) {
                pkg
            }
            coordinator.onMessageNotificationIntercepted(appLabel, pkg)
        }
    }

    override fun onDestroy() {
        unregisterMediaSessionListener()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SoundFocusListener"
    }
}
