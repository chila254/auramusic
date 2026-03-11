/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.auramusic.innertube.models.AlbumItem
import com.auramusic.innertube.models.SongItem
import com.auramusic.innertube.models.YTItem
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.models.toMediaMetadata
import com.auramusic.app.playback.queues.YouTubeQueue
import com.auramusic.app.ui.component.LocalMenuState
import com.auramusic.app.ui.component.YouTubeListItem
import com.auramusic.app.ui.menu.YouTubeSongMenu
import com.auramusic.app.viewmodels.NewReleasesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NewReleasesScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    viewModel: NewReleasesViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val albumReleases by viewModel.albumReleases.collectAsState()
    val songReleases by viewModel.songReleases.collectAsState()
    val videoReleases by viewModel.videoReleases.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val lazyListState = rememberLazyListState()

    val tabs = listOf(
        stringResource(R.string.albums),
        stringResource(R.string.songs),
        stringResource(R.string.filter_videos)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.new_releases)) }
        )

        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                selectedTabIndex == 0 -> {
                    if (albumReleases.isEmpty()) {
                        Text(
                            stringResource(R.string.no_results),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = 0.dp,
                                vertical = 8.dp
                            )
                        ) {
                            items(
                                items = albumReleases.distinctBy { it.id },
                                key = { it.id }
                            ) { album ->
                                YouTubeListItem(
                                    item = album,
                                    isActive = album.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    modifier = Modifier
                                        .padding(horizontal = 0.dp, vertical = 4.dp)
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                navController.navigate("album/${album.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    // Cannot show menu without song context
                                                    menuState.dismiss()
                                                }
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                selectedTabIndex == 1 -> {
                    if (songReleases.isEmpty()) {
                        Text(
                            stringResource(R.string.no_results),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = 0.dp,
                                vertical = 8.dp
                            )
                        ) {
                            items(
                                items = songReleases.distinctBy { it.id },
                                key = { it.id }
                            ) { song ->
                                YouTubeListItem(
                                    item = song,
                                    isActive = song.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    modifier = Modifier
                                        .padding(horizontal = 0.dp, vertical = 4.dp)
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue.radio(song.toMediaMetadata()),
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                else -> {
                    if (videoReleases.isEmpty()) {
                        Text(
                            stringResource(R.string.no_results),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = 0.dp,
                                vertical = 8.dp
                            )
                        ) {
                            items(
                                items = videoReleases.distinctBy { it.id },
                                key = { it.id }
                            ) { video ->
                                YouTubeListItem(
                                    item = video,
                                    isActive = video.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    modifier = Modifier
                                        .padding(horizontal = 0.dp, vertical = 4.dp)
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (video.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    (video as? SongItem)?.let { songItem ->
                                                        playerConnection.playQueue(
                                                            YouTubeQueue.radio(songItem.toMediaMetadata()),
                                                        )
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                (video as? SongItem)?.let { songItem ->
                                                    menuState.show {
                                                        YouTubeSongMenu(
                                                            song = songItem,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
