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
import android.os.Bundle
import com.auramusic.app.playback.MusicService

class CompactWideWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_COMPACT_WIDE_UPDATE
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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_COMPACT_WIDE_UPDATE
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
            ACTION_COMPACT_WIDE_PLAY_PAUSE, ACTION_COMPACT_WIDE_LIKE -> {
                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    action = when (intent.action) {
                        ACTION_COMPACT_WIDE_PLAY_PAUSE -> MusicWidgetReceiver.ACTION_PLAY_PAUSE
                        ACTION_COMPACT_WIDE_LIKE -> MusicWidgetReceiver.ACTION_LIKE
                        else -> ""
                    }
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
        const val ACTION_COMPACT_WIDE_PLAY_PAUSE = "com.auramusic.app.widget.COMPACT_WIDE_PLAY_PAUSE"
        const val ACTION_COMPACT_WIDE_LIKE = "com.auramusic.app.widget.COMPACT_WIDE_LIKE"
        const val ACTION_COMPACT_WIDE_UPDATE = "com.auramusic.app.widget.COMPACT_WIDE_UPDATE"
    }
}