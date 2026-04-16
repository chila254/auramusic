/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import com.auramusic.app.playback.MusicService

class CompactSquareWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_COMPACT_SQUARE_UPDATE
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_COMPACT_SQUARE_PLAY_PAUSE -> {
                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    action = MusicWidgetReceiver.ACTION_PLAY_PAUSE
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                }
            }
        }
    }

    companion object {
        const val ACTION_COMPACT_SQUARE_PLAY_PAUSE = "com.auramusic.app.widget.COMPACT_SQUARE_PLAY_PAUSE"
        const val ACTION_COMPACT_SQUARE_UPDATE = "com.auramusic.app.widget.COMPACT_SQUARE_UPDATE"
    }
}