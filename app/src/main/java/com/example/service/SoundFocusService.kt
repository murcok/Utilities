package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SoundFocusService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var coordinator: PlaybackStateCoordinator
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager
    private var monitoringJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        coordinator = PlaybackStateCoordinator.getInstance(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildForegroundNotification("In ascolto della riproduzione musicale...")
        startForeground(NOTIFICATION_ID, notification)

        startPeriodicCheck()
        observeCoordinatorState()

        return START_STICKY
    }

    private fun observeCoordinatorState() {
        serviceScope.launch {
            coordinator.focusState.collect { state ->
                val text = if (state.isSilenceActive) {
                    val app = state.activeAppName ?: "App Musicale"
                    "Silenzioso Attivo per messaggi ($app) • Chiamate consentite"
                } else {
                    "Monitoraggio attivo in background"
                }
                val notification = buildForegroundNotification(text)
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun startPeriodicCheck() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (isActive) {
                delay(3000L)
                val currentState = coordinator.focusState.value
                val isHardwareActive = audioManager.isMusicActive

                // If hardware music is completely stopped and coordinator thinks music is playing (not simulation)
                if (!isHardwareActive && currentState.isMusicPlaying && !currentState.isSimulationActive) {
                    currentState.activePackageName?.let { pkg ->
                        coordinator.onPlaybackPausedOrStopped(pkg)
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SoundFocus Stato Servizio",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mostra lo stato del monitoraggio e silenzioso automatico"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SoundFocus")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitoringJob?.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "sound_focus_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.soundfocus.START"
        const val ACTION_STOP = "com.example.soundfocus.STOP"

        fun start(context: Context) {
            val intent = Intent(context, SoundFocusService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SoundFocusService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
