package com.exam.playmusique.service


import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.exam.playmusique.MainActivity
import com.exam.playmusique.MediaPlayerListener
import com.exam.playmusique.MusicPlayerManager
import com.exam.playmusique.MyApplication
import com.exam.playmusique.R
import com.exam.playmusique.modal.Song

class MusicService : Service() {

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var musicPlayerManager: MusicPlayerManager
    private var playlist: List<Song> = emptyList()
    private var currentSongIndex: Int = -1

    companion object {
        const val CHANNEL_ID = "MusicServiceChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "com.exam.playmusique.ACTION_PLAY"
        const val ACTION_PAUSE = "com.exam.playmusique.ACTION_PAUSE"
        const val ACTION_NEXT = "com.exam.playmusique.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.exam.playmusique.ACTION_PREVIOUS"
    }

    override fun onCreate() {
        super.onCreate()
        musicPlayerManager = (application as MyApplication).musicPlayerManager
        musicPlayerManager.setMediaPlayerListener(object : MediaPlayerListener {
            override fun onSongCompletion() {
                handleNext()
            }
        })
        setupMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_PLAY -> {
                    val newPlaylist = it.getSerializableExtra("playlist") as? ArrayList<Song>
                    val startIndex = it.getIntExtra("startIndex", 0)
                    if (newPlaylist != null && newPlaylist.isNotEmpty()) {
                        setPlaylistAndPlay(newPlaylist, startIndex)
                    } else {
                        playCurrentSong()
                    }
                }
                ACTION_PAUSE -> musicPlayerManager.pauseSong(null)
                ACTION_NEXT -> handleNext()
                ACTION_PREVIOUS -> handlePrevious()
            }
        }
        updateNotification()
        return START_STICKY
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    playCurrentSong()
                }

                override fun onPause() {
                    musicPlayerManager.pauseSong(null)
                    updateNotification()
                }

                override fun onSkipToNext() {
                    handleNext()
                }

                override fun onSkipToPrevious() {
                    handlePrevious()
                }
            })
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        }
    }

    private fun playCurrentSong() {
        if (playlist.isNotEmpty() && currentSongIndex in playlist.indices) {
            val song = playlist[currentSongIndex]
            musicPlayerManager.playSong(song, null, null, null, null)
            updateMediaSessionMetadata(song)
            updateNotification()
        }
    }

    private fun handleNext() {
        if (playlist.isNotEmpty() && currentSongIndex < playlist.size - 1) {
            currentSongIndex++
            playCurrentSong()
        }
    }

    private fun handlePrevious() {
        if (playlist.isNotEmpty() && currentSongIndex > 0) {
            currentSongIndex--
            playCurrentSong()
        }
    }

    private fun updateMediaSessionMetadata(song: Song) {
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, song.image)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, musicPlayerManager.getDuration())
                .build()
        )

        val state = PlaybackStateCompat.Builder()
            .setState(
                if (musicPlayerManager.isPlaying()) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                musicPlayerManager.getCurrentPosition(),
                1.0f
            )
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .build()
        mediaSession.setPlaybackState(state)
        mediaSession.isActive = true
    }

    private fun updateNotification() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotification(): android.app.Notification {
        val playPauseIntent = Intent(this, MusicService::class.java).apply {
            action = if (musicPlayerManager.isPlaying()) ACTION_PAUSE else ACTION_PLAY
        }
        val nextIntent = Intent(this, MusicService::class.java).apply { action = ACTION_NEXT }
        val previousIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PREVIOUS }

        val playPausePendingIntent = PendingIntent.getService(this, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val previousPendingIntent = PendingIntent.getService(this, 0, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val intent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val currentSong = if (currentSongIndex in playlist.indices) playlist[currentSongIndex] else null

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(currentSong?.title ?: "No Song")
            .setContentText(currentSong?.artist ?: "Unknown Artist")
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_previous, "Previous", previousPendingIntent)
            .addAction(
                if (musicPlayerManager.isPlaying()) R.drawable.ic_pause else R.drawable.ic_play,
                if (musicPlayerManager.isPlaying()) "Pause" else "Play",
                playPausePendingIntent
            )
            .addAction(R.drawable.ic_next, "Next", nextPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    fun setPlaylistAndPlay(playlist: List<Song>, startIndex: Int = 0) {
        this.playlist = playlist
        this.currentSongIndex = if (startIndex in playlist.indices) startIndex else 0
        playCurrentSong()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        musicPlayerManager.releaseMediaPlayer()
        stopForeground(true)
    }
}