/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.utils

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass Effect - Apple/iOS style frosted glass
 * Creates a translucent frosted glass appearance with blur effect
 * 
 * @param enabled Whether the liquid glass effect is enabled
 * @param cornerRadius The corner radius for the glass effect
 * @param alpha The transparency alpha (0.0 to 1.0)
 * @param blurRadius The blur intensity (requires Android 12+)
 */
@Composable
fun Modifier.liquidGlass(
    enabled: Boolean,
    cornerRadius: Dp = 16.dp,
    alpha: Float = 0.15f,
    blurRadius: Dp = 20.dp
): Modifier {
    return this.then(
        if (enabled) {
            Modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
                )
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(blurRadius)
                    } else {
                        Modifier
                    }
                )
                .background(
                    Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(cornerRadius)
                )
        } else {
            Modifier
        }
    )
}

/**
 * Liquid Glass container - provides the frosted glass background
 */
@Composable
fun LiquidGlassContainer(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    if (enabled) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.2f else 0.15f)
                )
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(20.dp)
                    } else {
                        Modifier
                    }
                )
                .background(
                    Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(cornerRadius)
                )
        ) {
            content()
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

/**
 * Simple frosted glass background for cards and containers
 */
@Composable
fun FrostedGlassCard(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    if (enabled) {
        val glassColor = MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.25f else 0.2f
        )
        
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    color = glassColor
                )
        ) {
            // Add subtle border for glass effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(cornerRadius)
                    )
            ) {
                content()
            }
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}