package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.CallPriorityMode
import com.example.data.model.FocusSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sound_focus_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<FocusSettings> = _settings.asStateFlow()

    fun getSettings(): FocusSettings = _settings.value

    fun updateSettings(update: (FocusSettings) -> FocusSettings) {
        val newSettings = update(_settings.value)
        saveSettings(newSettings)
        _settings.value = newSettings
    }

    private fun loadSettings(): FocusSettings {
        val isMasterEnabled = prefs.getBoolean("master_enabled", true)
        val callPriorityName = prefs.getString("call_priority", CallPriorityMode.ANY_CALLER.name)
            ?: CallPriorityMode.ANY_CALLER.name
        val callPriority = try {
            CallPriorityMode.valueOf(callPriorityName)
        } catch (_: Exception) {
            CallPriorityMode.ANY_CALLER
        }
        val muteNotificationStream = prefs.getBoolean("mute_notification_stream", true)
        val allowAlarms = prefs.getBoolean("allow_alarms", true)
        val gracePeriodSeconds = prefs.getInt("grace_period_seconds", 4)
        val notifyOnModeChange = prefs.getBoolean("notify_on_mode_change", true)
        val snoozeMessages = prefs.getBoolean("snooze_messages", false)
        val simulationMode = prefs.getBoolean("simulation_mode", false)

        return FocusSettings(
            isMasterEnabled = isMasterEnabled,
            callPriorityMode = callPriority,
            muteNotificationStream = muteNotificationStream,
            allowAlarms = allowAlarms,
            gracePeriodSeconds = gracePeriodSeconds,
            notifyOnModeChange = notifyOnModeChange,
            snoozeMessages = snoozeMessages,
            simulationMode = simulationMode
        )
    }

    private fun saveSettings(s: FocusSettings) {
        prefs.edit()
            .putBoolean("master_enabled", s.isMasterEnabled)
            .putString("call_priority", s.callPriorityMode.name)
            .putBoolean("mute_notification_stream", s.muteNotificationStream)
            .putBoolean("allow_alarms", s.allowAlarms)
            .putInt("grace_period_seconds", s.gracePeriodSeconds)
            .putBoolean("notify_on_mode_change", s.notifyOnModeChange)
            .putBoolean("snooze_messages", s.snoozeMessages)
            .putBoolean("simulation_mode", s.simulationMode)
            .apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
