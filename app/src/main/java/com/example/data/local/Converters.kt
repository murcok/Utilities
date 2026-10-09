package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.CallPriorityMode
import com.example.data.model.CallSignalMode
import com.example.data.model.FocusEventType

class Converters {
    @TypeConverter
    fun fromEventType(value: FocusEventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): FocusEventType {
        return try {
            FocusEventType.valueOf(value)
        } catch (_: Exception) {
            FocusEventType.PLAYBACK_DETECTED
        }
    }

    @TypeConverter
    fun fromCallPriorityMode(value: CallPriorityMode): String = value.name

    @TypeConverter
    fun toCallPriorityMode(value: String): CallPriorityMode {
        return try {
            CallPriorityMode.valueOf(value)
        } catch (_: Exception) {
            CallPriorityMode.ANY_CALLER
        }
    }

    @TypeConverter
    fun fromCallSignalMode(value: CallSignalMode): String = value.name

    @TypeConverter
    fun toCallSignalMode(value: String): CallSignalMode {
        return try {
            CallSignalMode.valueOf(value)
        } catch (_: Exception) {
            CallSignalMode.RING_AND_VIBRATE
        }
    }
}
