package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val isKnownMusicApp: Boolean = true,
    val isInstalled: Boolean = false,
    val lastActiveTimestamp: Long = 0L
)
