package com.auramusic.app.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.media.MediaIntentBuilder
import com.google.android.gms.cast.framework.media.NotificationOptions

class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
            .setReceiverApplicationId("CC1AD845")
            .setNotificationOptions(
                NotificationOptions.Builder()
                    .setActions(
                        arrayOf(
                            MediaIntentBuilder().setAction(MediaIntentBuilder.ACTION_SKIP_PREV).build(),
                            MediaIntentBuilder().setAction(MediaIntentBuilder.ACTION_TOGGLE_PLAYBACK).build(),
                            MediaIntentBuilder().setAction(MediaIntentBuilder.ACTION_SKIP_NEXT).build()
                        ),
                        intArrayOf(0, 1, 2)
                    )
                    .setTargetActivityClassName("com.auramusic.app.MainActivity")
                    .build()
            )
            .build()
    }
}