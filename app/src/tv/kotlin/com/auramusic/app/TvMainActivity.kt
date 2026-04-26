package com.auramusic.app

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.auramusic.app.db.MusicDatabase
import com.auramusic.app.playback.DownloadUtil
import com.auramusic.app.playback.MusicService
import com.auramusic.app.playback.MusicService.MusicBinder
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.utils.SyncUtils
import com.auramusic.app.ui.components.CardPresenter
import com.auramusic.app.ui.fragments.TvBrowseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TvMainActivity : FragmentActivity() {

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    @Inject
    lateinit var listenTogetherManager: com.auramusic.app.listentogether.ListenTogetherManager

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)

    // Voice command system
    @Inject
    lateinit var voiceCommandViewModel: com.auramusic.app.voice.VoiceCommandViewModel

    private var hasMicPermission by mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicBinder) {
                try {
                    playerConnection = PlayerConnection(this@TvMainActivity, service, database, lifecycleScope)
                    Timber.tag("TvMainActivity").d("PlayerConnection created successfully")
                    // Connect Listen Together manager to player
                    listenTogetherManager.setPlayerConnection(playerConnection)
                } catch (e: Exception) {
                    Timber.tag("TvMainActivity").e(e, "Failed to create PlayerConnection")
                    // Retry after a delay of 500ms
                    lifecycleScope.launch {
                        delay(500)
                        try {
                            playerConnection = PlayerConnection(this@TvMainActivity, service, database, lifecycleScope)
                            listenTogetherManager.setPlayerConnection(playerConnection)
                        } catch (e2: Exception) {
                            Timber.tag("TvMainActivity").e(e2, "Failed to create PlayerConnection on retry")
                        }
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // Disconnect Listen Together manager
            listenTogetherManager.setPlayerConnection(null)
            playerConnection?.dispose()
            playerConnection = null
        }
    }

    override fun onStart() {
        super.onStart()
        // Bind to MusicService
        bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize voice commands
        lifecycleScope.launch {
            voiceCommandViewModel.onMicPermissionChanged(hasMicPermission)
            voiceCommandViewModel.onAppForeground()
        }

        // Bind voice command handlers
        lifecycleScope.launch {
            voiceCommandViewModel.bindHandlers(
                playerConnectionProvider = { playerConnection },
                onSearch = { query ->
                    // Navigate to search with query
                    val searchFragment = TvSearchFragment()
                    searchFragment.playerConnection = playerConnection
                    supportFragmentManager.beginTransaction()
                        .replace(android.R.id.content, searchFragment)
                        .addToBackStack(null)
                        .commit()
                    // TODO: Pass query to search fragment
                },
                onNavigate = { route ->
                    // Handle navigation commands
                    when (route) {
                        "home" -> {
                            val browseFragment = TvBrowseFragment()
                            browseFragment.playerConnection = playerConnection
                            supportFragmentManager.beginTransaction()
                                .replace(android.R.id.content, browseFragment)
                                .commit()
                        }
                        "search" -> {
                            val searchFragment = TvSearchFragment()
                            searchFragment.playerConnection = playerConnection
                            supportFragmentManager.beginTransaction()
                                .replace(android.R.id.content, searchFragment)
                                .addToBackStack(null)
                                .commit()
                        }
                    }
                }
            )
        }

        val browseFragment = TvBrowseFragment()
        browseFragment.playerConnection = playerConnection

        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, browseFragment)
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceCommandViewModel.onAppBackground()
    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }


}