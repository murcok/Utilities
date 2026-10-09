package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_logs")
data class FocusLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val appName: String,
    val packageName: String,
    val eventType: FocusEventType,
    val details: String
)

enum class FocusEventType {
    PLAYBACK_DETECTED,
    SILENCE_ACTIVATED,
    PLAYBACK_STOPPED,
    SILENCE_DEACTIVATED,
    MESSAGE_SILENCED,
    CALL_ALLOWED,
    SIMULATION_START,
    SIMULATION_STOP,
    MANUAL_OVERRIDE
}
