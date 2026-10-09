package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CallSignalMode(val labelIt: String, val descriptionIt: String) {
    RING_AND_VIBRATE("Suoneria e Vibrazione", "Segnale sonoro e vibrazione sempre attivi"),
    RING_ONLY("Solo Suoneria", "Squillo sonoro attivo senza vibrazione"),
    VIBRATE_ONLY("Solo Vibrazione", "Vibrazione intensa per le chiamate senza suoneria")
}

@Entity(tableName = "audio_profiles")
data class AudioProfile(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val iconName: String = "headphones", // headphones, work, car, bell, moon
    val isDefaultMusicProfile: Boolean = false,
    val isAutoTriggerOnMusic: Boolean = true,
    val silenceMessages: Boolean = true,
    val callSignalMode: CallSignalMode = CallSignalMode.RING_AND_VIBRATE,
    val callPriorityMode: CallPriorityMode = CallPriorityMode.ANY_CALLER,
    val allowAlarms: Boolean = true,
    val isManuallyActive: Boolean = false,
    val isAutoActive: Boolean = false
) {
    val isActive: Boolean
        get() = isManuallyActive || isAutoActive
}
