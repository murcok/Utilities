package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AudioProfile
import com.example.data.model.FocusLog
import com.example.data.model.MonitoredApp
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundFocusDao {

    // Monitored Apps
    @Query("SELECT * FROM monitored_apps ORDER BY isEnabled DESC, appName ASC")
    fun getAllMonitoredAppsFlow(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps WHERE isEnabled = 1")
    suspend fun getEnabledApps(): List<MonitoredApp>

    @Query("SELECT * FROM monitored_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackage(packageName: String): MonitoredApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateApp(app: MonitoredApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<MonitoredApp>)

    @Update
    suspend fun updateApp(app: MonitoredApp)

    @Delete
    suspend fun deleteApp(app: MonitoredApp)

    @Query("SELECT COUNT(*) FROM monitored_apps")
    suspend fun getMonitoredAppsCount(): Int

    // Audio Profiles
    @Query("SELECT * FROM audio_profiles ORDER BY isDefaultMusicProfile DESC, name ASC")
    fun getAllProfilesFlow(): Flow<List<AudioProfile>>

    @Query("SELECT * FROM audio_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): AudioProfile?

    @Query("SELECT * FROM audio_profiles WHERE isDefaultMusicProfile = 1 LIMIT 1")
    suspend fun getDefaultMusicProfile(): AudioProfile?

    @Query("SELECT * FROM audio_profiles WHERE isAutoTriggerOnMusic = 1 LIMIT 1")
    suspend fun getMusicTriggerProfile(): AudioProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AudioProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<AudioProfile>)

    @Update
    suspend fun updateProfile(profile: AudioProfile)

    @Delete
    suspend fun deleteProfile(profile: AudioProfile)

    @Query("UPDATE audio_profiles SET isManuallyActive = 0, isAutoActive = 0")
    suspend fun deactivateAllProfiles()

    @Query("SELECT COUNT(*) FROM audio_profiles")
    suspend fun getProfilesCount(): Int

    // Focus Logs
    @Query("SELECT * FROM focus_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogsFlow(): Flow<List<FocusLog>>

    @Insert
    suspend fun insertLog(log: FocusLog)

    @Query("DELETE FROM focus_logs")
    suspend fun clearAllLogs()
}
