package com.exam.playmusique

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.TextView
import com.exam.playmusique.modal.Song
import java.io.IOException

class MusicPlayerManager : MediaPlayer.OnCompletionListener {
    private var mediaPlayer: MediaPlayer? = null
    private var isPaused: Boolean = false
    private var currentSong: Song? = null // Suivre la chanson actuelle
    private var mediaPlayerListener: MediaPlayerListener? = null

    init {
        mediaPlayer = MediaPlayer()
    }

    fun playSong(song: Song, playPause: ImageView, tv: TextView, artistName: TextView, albumArt: ImageView) {
        try {
            // Si une chanson est déjà en cours, reset le MediaPlayer
            mediaPlayer?.reset() ?: run { mediaPlayer = MediaPlayer() }

            mediaPlayer?.setDataSource(MyApplication.instance, Uri.parse(song.data))
            mediaPlayer?.prepare()
            mediaPlayer?.start()

            isPaused = false
            currentSong = song

            // Mettre à jour l'UI
            tv.text = song.title
            artistName.text = song.artist ?: "Unknown Artist"
            updatePlayPauseButtonUI(playPause)
            animateSongNameScroll(tv)

            // Charger la pochette d'album si disponible
            song.image?.let {
                val uri = Uri.parse(it)
                albumArt.setImageURI(uri)
                if (albumArt.drawable == null) {
                    albumArt.setImageResource(R.drawable.default_album_art) // Image par défaut si échec
                }
            } ?: albumArt.setImageResource(R.drawable.default_album_art)

            // Configurer le listener pour la fin de la chanson
            mediaPlayer?.setOnCompletionListener(this)
        } catch (e: IOException) {
            Log.e("MusicPlayerManager", "Erreur lors de la lecture de ${song.data}: ${e.message}")
            mediaPlayer?.reset() // Réinitialiser en cas d'erreur
        } catch (e: IllegalStateException) {
            Log.e("MusicPlayerManager", "État invalide du MediaPlayer: ${e.message}")
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()
        }
    }

    fun pauseSong(playPause: ImageView?) {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                isPaused = true
                updatePlayPauseButtonUI(playPause)
            }
        } catch (e: IllegalStateException) {
            Log.e("MusicPlayerManager", "Erreur lors de la pause: ${e.message}")
        }
    }

    fun resumeSong(playPause: ImageView) {
        try {
            if (isPaused && mediaPlayer != null) {
                mediaPlayer?.start()
                isPaused = false
                updatePlayPauseButtonUI(playPause)
            }
        } catch (e: IllegalStateException) {
            Log.e("MusicPlayerManager", "Erreur lors de la reprise: ${e.message}")
        }
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: IllegalStateException) {
            Log.e("MusicPlayerManager", "Erreur lors de la vérification de isPlaying: ${e.message}")
            false
        }
    }

    fun releaseMediaPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPaused = false
            currentSong = null
        } catch (e: IllegalStateException) {
            Log.e("MusicPlayerManager", "Erreur lors de la libération: ${e.message}")
        }
    }

    fun updatePlayPauseButtonUI(playPause: ImageView?) {
        if (playPause != null) {
            playPause.setImageResource(if (isPlaying()) R.drawable.pause else R.drawable.play)
        }
    }

    fun animateSongNameScroll(textView: TextView) {
        textView.text = currentSong?.title ?: ""
        val textWidth = textView.paint.measureText(textView.text.toString())
        val screenWidth = MyApplication.instance.resources.displayMetrics.widthPixels.toFloat()

        if (textWidth > screenWidth) { // Animer uniquement si le texte est trop long
            val translateAnimation = TranslateAnimation(screenWidth, -textWidth, 0f, 0f)
            translateAnimation.duration = (textWidth / screenWidth * 10000).toLong()
            translateAnimation.repeatCount = Animation.INFINITE
            translateAnimation.interpolator = LinearInterpolator()
            textView.startAnimation(translateAnimation)
        } else {
            textView.clearAnimation() // Arrêter l'animation si le texte tient
        }
    }

    fun getCurrentPosition(): Long {
        return try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (e: IllegalStateException) {
            Log.e("MusicPlayerManager", "Erreur lors de getCurrentPosition: ${e.message}")
            0L
        }
    }

    fun seekTo(value: Long) {
        try {
            mediaPlayer?.seekTo(value.toInt())
        } catch (e: IllegalStateException) {
            Log.e("MusicPlayerManager", "Erreur lors de seekTo: ${e.message}")
        }
    }

    fun getDuration(): Long {
        return try {
            mediaPlayer?.duration?.toLong() ?: 0L
        } catch (e: IllegalStateException) {
            Log.e("MusicPlayerManager", "Erreur lors de getDuration: ${e.message}")
            0L
        }
    }

    fun setVolume(vol1: Float, vol2: Float) {
        try {
            mediaPlayer?.setVolume(vol1, vol2)
        } catch (e: IllegalStateException) {
            Log.e("MusicPlayerManager", "Erreur lors de setVolume: ${e.message}")
        }
    }

    fun setMediaPlayerListener(listener: MediaPlayerListener) {
        mediaPlayerListener = listener
    }

    override fun onCompletion(mp: MediaPlayer?) {
        isPaused = false
        mediaPlayerListener?.onSongCompletion()
    }
}

interface MediaPlayerListener {
    fun onSongCompletion()
}