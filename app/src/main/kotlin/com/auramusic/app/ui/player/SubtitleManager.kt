package com.auramusic.app.ui.player

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CaptionStyleCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.SubtitleView

@UnstableApi
class SubtitleManager(
    private val subtitleView: SubtitleView,
    private val context: Context
) : Player.Listener {
    
    companion object {
        const val TAG = "SubtitleManager"
    }
    
    private var subsBuffer: CharSequence? = null
    
    init {
        configureSubtitleView()
    }
    
    private fun configureSubtitleView() {
        if (subtitleView != null) {
            subtitleView.setApplyEmbeddedStyles(false)
            subtitleView.setStyle(
                CaptionStyleCompat(
                    Color.WHITE,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                    Color.BLACK,
                    Typeface.DEFAULT_BOLD
                )
            )
            subtitleView.setBottomPaddingFraction(0.08f)
        }
    }
    
    override fun onCues(cueGroup: Player.CueGroup) {
        val cues = cueGroup.cues
        if (subtitleView != null && cues != null) {
            subtitleView.setCues(forceCenterAlignment(cues))
        }
    }
    
    fun show(show: Boolean) {
        if (subtitleView != null) {
            subtitleView.visibility = if (show) View.VISIBLE else View.GONE
        }
    }
    
    private fun forceCenterAlignment(cues: List<Cue>): List<Cue> {
        val result = mutableListOf<Cue>()
        
        for (cue in cues) {
            val textStr = cue.text?.toString() ?: ""
            
            if (textStr.endsWith("\n") || textStr.endsWith(" ")) {
                subsBuffer = textStr
            } 
            else if (textStr.contains("\n")) {
                val text: CharSequence
                if (subsBuffer != null && textStr.contains(subsBuffer.toString())) {
                    text = textStr.replace(subsBuffer.toString(), "").replace("\n", "")
                } else {
                    text = textStr
                }
                result.add(Cue.Builder().setText(text).build())
                
                val split = textStr.split("\n")
                subsBuffer = if (split.size == 2) split[1] else textStr
            } else {
                val text: CharSequence
                if (subsBuffer != null) {
                    text = textStr.replace(subsBuffer.toString(), "")
                } else {
                    text = textStr
                }
                result.add(Cue.Builder().setText(text).build())
                subsBuffer = text
            }
        }
        
        return result
    }
}
