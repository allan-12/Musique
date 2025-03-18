package com.exam.playmusique

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.TextView
import com.exam.playmusique.modal.Song
import com.exam.playmusique.service.MusicService
import java.io.IOException

class MusicPlayerManager : MediaPlayer.OnCompletionListener {
    private var mediaPlayer: MediaPlayer? = null
    private var isPaused: Boolean = false
    private var currentSong: Song? = null
    private var mediaPlayerListener: MediaPlayerListener? = null

    init {
        mediaPlayer = MediaPlayer()
    }

    fun playSong(
        song: Song,
        playPause: ImageView? = null,
        tv: TextView? = null,
        artistName: TextView? = null,
        albumArt: ImageView? = null
    ) {
        try {
            mediaPlayer?.reset() ?: run { mediaPlayer = MediaPlayer() }
            mediaPlayer?.setDataSource(MyApplication.instance, Uri.parse(song.data))
            mediaPlayer?.prepare()
            mediaPlayer?.start()
            isPaused = false
            currentSong = song
            mediaPlayer?.setOnCompletionListener(this)

            // Mise à jour de l’UI si les vues sont fournies
            tv?.text = song.title
            artistName?.text = song.artist ?: "Unknown Artist"
            playPause?.let { updatePlayPauseButtonUI(it) }
            tv?.let { animateSongNameScroll(it) }

            song.image?.let {
                val uri = Uri.parse(it)
                albumArt?.setImageURI(uri)
                if (albumArt?.drawable == null) {
                    if (albumArt != null) {
                        albumArt.setImageResource(R.drawable.default_album_art)
                    }
                }
            } ?: albumArt?.setImageResource(R.drawable.default_album_art)
        } catch (e: IOException) {
            Log.e("MusicPlayerManager", "Erreur lors de la lecture: ${e.message}")
            mediaPlayer?.reset()
        } catch (e: IllegalStateException) {
            Log.e("MusicPlayerManager", "État invalide: ${e.message}")
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()
        }
    }

    fun setPlaylistAndPlay(playlist: List<Song>, startIndex: Int = 0) {
        if (playlist.isNotEmpty()) {
            val serviceIntent = Intent(MyApplication.instance, MusicService::class.java).apply {
                action = MusicService.ACTION_PLAY
                putExtra("playlist", ArrayList(playlist)) // Passer la playlist au service
                putExtra("startIndex", startIndex)       // Passer l’index de départ
            }
            MyApplication.instance.startService(serviceIntent)
        }
    }

    fun pauseSong(playPause: ImageView?) {
        mediaPlayer?.pause()
        isPaused = true
        playPause?.let { updatePlayPauseButtonUI(it) }
    }

    fun resumeSong(playPause: ImageView?) {
        mediaPlayer?.start()
        isPaused = false
        playPause?.let { updatePlayPauseButtonUI(it) }
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
    }

    fun getCurrentPosition(): Long = mediaPlayer?.currentPosition?.toLong() ?: 0L

    fun getDuration(): Long = mediaPlayer?.duration?.toLong() ?: 0L

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun updatePlayPauseButtonUI(playPause: ImageView) {
        playPause.setImageResource(if (isPlaying()) R.drawable.ic_pause else R.drawable.ic_play)
    }

    fun animateSongNameScroll(songName: TextView) {
        val animation = TranslateAnimation(
            songName.width.toFloat(),
            -songName.width.toFloat(),
            0f,
            0f
        ).apply {
            duration = 5000
            interpolator = LinearInterpolator()
            repeatCount = Animation.INFINITE
            repeatMode = Animation.RESTART
        }
        songName.startAnimation(animation)
    }

    fun setMediaPlayerListener(listener: MediaPlayerListener?) {
        this.mediaPlayerListener = listener
    }

    override fun onCompletion(mp: MediaPlayer?) {
        isPaused = false
        mediaPlayerListener?.onSongCompletion()
    }
}

interface MediaPlayerListener {
    fun onSongCompletion()
}