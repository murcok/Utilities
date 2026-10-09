package com.example

import android.content.Context
import android.media.AudioManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SettingsManager
import com.example.data.local.SoundFocusDatabase
import com.example.data.model.AudioProfile
import com.example.data.model.CallPriorityMode
import com.example.data.model.CallSignalMode
import com.example.data.model.FocusEventType
import com.example.data.model.FocusLog
import com.example.data.model.MonitoredApp
import com.example.service.AudioSilenceManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: SoundFocusDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SoundFocusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context verifies SoundFocus app name`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("SoundFocus", appName)
    }

    @Test
    fun `database stores and retrieves audio profiles correctly`() = runBlocking {
        val dao = db.soundFocusDao()
        val musicProfile = AudioProfile(
            id = "profile_music_default",
            name = "Profilo Musica",
            description = "Silenzia i messaggi durante l'ascolto di musica e lascia squillare tutte le chiamate.",
            iconName = "headphones",
            isDefaultMusicProfile = true,
            isAutoTriggerOnMusic = true,
            silenceMessages = true,
            callSignalMode = CallSignalMode.RING_AND_VIBRATE,
            callPriorityMode = CallPriorityMode.ANY_CALLER,
            allowAlarms = true
        )
        dao.insertProfile(musicProfile)

        val retrieved = dao.getProfileById("profile_music_default")
        assertNotNull(retrieved)
        assertEquals("Profilo Musica", retrieved?.name)
        assertTrue(retrieved?.isDefaultMusicProfile == true)
        assertTrue(retrieved?.silenceMessages == true)
        assertEquals(CallSignalMode.RING_AND_VIBRATE, retrieved?.callSignalMode)
    }

    @Test
    fun `database stores and retrieves monitored apps correctly`() = runBlocking {
        val dao = db.soundFocusDao()
        val spotify = MonitoredApp(
            packageName = "com.spotify.music",
            appName = "Spotify",
            isEnabled = true,
            isKnownMusicApp = true,
            isInstalled = true
        )
        dao.insertOrUpdateApp(spotify)

        val retrieved = dao.getAppByPackage("com.spotify.music")
        assertNotNull(retrieved)
        assertEquals("Spotify", retrieved?.appName)
        assertTrue(retrieved?.isEnabled == true)

        // Toggle app
        dao.insertOrUpdateApp(spotify.copy(isEnabled = false))
        val disabled = dao.getAppByPackage("com.spotify.music")
        assertFalse(disabled?.isEnabled == true)
    }

    @Test
    fun `audio silence manager preserves media volume and handles deactivation`() {
        val silenceManager = AudioSilenceManager(context)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Set test music volume
        val targetMusicVol = 8
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetMusicVol, 0)

        val profile = AudioProfile(
            id = "profile_test",
            name = "Profilo Test",
            description = "Test",
            silenceMessages = true,
            callSignalMode = CallSignalMode.RING_AND_VIBRATE,
            callPriorityMode = CallPriorityMode.ANY_CALLER
        )

        silenceManager.activateProfileSilence(profile, "Test")
        assertEquals(targetMusicVol, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))

        silenceManager.deactivateSilence()
        assertEquals(targetMusicVol, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
}
