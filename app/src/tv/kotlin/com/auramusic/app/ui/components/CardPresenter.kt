package com.auramusic.app.ui.components

import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import coil3.load
import com.auramusic.app.db.entities.*

class CardPresenter : Presenter() {

    var onItemClicked: ((Any) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        cardView.setMainImageDimensions(
            parent.resources.getDimensionPixelSize(R.dimen.card_width),
            parent.resources.getDimensionPixelSize(R.dimen.card_height)
        )
        cardView.setOnClickListener {
            val item = cardView.tag
            item?.let { onItemClicked?.invoke(it) }
        }
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val cardView = viewHolder.view as ImageCardView
        cardView.tag = item

        when (item) {
            is Song -> {
                cardView.titleText = item.song.title
                cardView.contentText = item.artists.joinToString(", ") { it.name }
                cardView.mainImageView.load(item.song.thumbnailUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_music_note)
                    error(R.drawable.ic_music_note)
                }
            }
            is Playlist -> {
                cardView.titleText = item.playlist.name
                cardView.contentText = "${item.songCount} songs"
                cardView.mainImageView.load(item.playlist.thumbnailUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_playlist)
                    error(R.drawable.ic_playlist)
                }
            }
            is Artist -> {
                cardView.titleText = item.artist.name
                cardView.contentText = "${item.songCount} songs"
                cardView.mainImageView.load(item.artist.thumbnailUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_person)
                    error(R.drawable.ic_person)
                }
            }
            is Album -> {
                cardView.titleText = item.album.title
                cardView.contentText = item.artists.joinToString(", ") { it.name }
                cardView.mainImageView.load(item.album.thumbnailUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_album)
                    error(R.drawable.ic_album)
                }
            }
            is com.auramusic.innertube.models.PlaylistItem -> {
                cardView.titleText = item.title
                cardView.contentText = item.author?.name ?: ""
                cardView.mainImageView.load(item.thumbnail) {
                    crossfade(true)
                    placeholder(R.drawable.ic_playlist)
                    error(R.drawable.ic_playlist)
                }
            }
            is com.auramusic.app.viewmodels.CommunityPlaylistItem -> {
                cardView.titleText = item.playlist.title
                cardView.contentText = "${item.songs.size} songs"
                cardView.mainImageView.load(item.playlist.thumbnail) {
                    crossfade(true)
                    placeholder(R.drawable.ic_playlist)
                    error(R.drawable.ic_playlist)
                }
            }
            is com.auramusic.app.viewmodels.SimilarRecommendation -> {
                cardView.titleText = item.title
                cardView.contentText = item.subtitle ?: ""
                cardView.mainImageView.load(item.thumbnail) {
                    crossfade(true)
                    placeholder(R.drawable.ic_music_note)
                    error(R.drawable.ic_music_note)
                }
            }
            is com.auramusic.app.db.entities.LocalItem -> {
                cardView.titleText = when (item) {
                    is Song -> item.song.title
                    else -> item.toString()
                }
                cardView.contentText = when (item) {
                    is Song -> item.artists.joinToString(", ") { it.name }
                    else -> ""
                }
                cardView.mainImageView.setImageResource(R.drawable.ic_music_note)
            }
            is com.auramusic.app.viewmodels.SpeedDialItem -> {
                cardView.titleText = item.title
                cardView.contentText = ""
                cardView.mainImageView.load(item.thumbnail) {
                    crossfade(true)
                    placeholder(R.drawable.ic_music_note)
                    error(R.drawable.ic_music_note)
                }
            }
            else -> {
                cardView.titleText = item.toString()
                cardView.contentText = ""
                cardView.mainImageView.setImageResource(R.drawable.ic_music_note)
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.mainImageView.setImageDrawable(null)
    }
}