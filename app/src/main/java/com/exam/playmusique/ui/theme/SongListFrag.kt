package com.exam.playmusique.ui.theme

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.exam.playmusique.MediaPlayerListener
import com.exam.playmusique.MusicPlayerManager
import com.exam.playmusique.MyApplication
import com.exam.playmusique.R
import com.exam.playmusique.adapter.SongAdapter
import com.exam.playmusique.adapter.onSongItemClicked
import com.exam.playmusique.databinding.FragmentSongListBinding
import com.exam.playmusique.modal.Song
import com.exam.playmusique.mvvm.SongRepository
import com.exam.playmusique.mvvm.SongViewModel
import com.exam.playmusique.mvvm.SongViewModelFactory

class SongListFrag : Fragment(), onSongItemClicked, MediaPlayerListener {

    private lateinit var binding: FragmentSongListBinding
    private lateinit var viewModel: SongViewModel
    private val REQUEST_PERMISSION_CODE = 123

    private lateinit var adapter: SongAdapter
    private var currentSongPosition: Int = -1
    private lateinit var musicPlayerManager: MusicPlayerManager

    private lateinit var expandedPlayPause: ImageView
    private lateinit var expandedNext: ImageView
    private lateinit var expandedPrevious: ImageView
    private lateinit var expandedSeekBar: SeekBar
    private lateinit var expandedStartTime: TextView
    private lateinit var expandedEndTime: TextView
    private lateinit var expandedSongTitle: TextView
    private lateinit var expandedArtist: TextView
    private lateinit var expandedAlbumArt: ImageView

    private lateinit var compactPlayPause: ImageView
    private lateinit var compactNext: ImageView
    private lateinit var compactPrevious: ImageView
    private lateinit var compactSongTitle: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            if (musicPlayerManager.isPlaying()) {
                expandedSeekBar.progress = musicPlayerManager.getCurrentPosition().toInt()
                updateStartTimeTextView()
                updateEndTimeTextView()
            }
            handler.postDelayed(this, 200)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_song_list, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        initAnimations()

        if (hasPermissions()) {
            loadSongs()
        } else {
            requestPermissions()
        }
    }

    private fun initUI() {
        musicPlayerManager = (requireActivity().application as MyApplication).musicPlayerManager
        musicPlayerManager.setMediaPlayerListener(this)

        adapter = SongAdapter()
        adapter.setClickListener(this)

        val repository = SongRepository()
        viewModel = ViewModelProvider(this, SongViewModelFactory(repository)).get(SongViewModel::class.java)

        // Initialisation des vues du layout compact
        compactPlayPause = binding.compactPlayerLayout.findViewById(R.id.playPauseButtonMinimized)
        compactNext = binding.compactPlayerLayout.findViewById(R.id.nextButtonMinimized)
        compactPrevious = binding.compactPlayerLayout.findViewById(R.id.previousButtonMinimized)
        compactSongTitle = binding.compactPlayerLayout.findViewById(R.id.songTitleTextViewMinimized)
        val compactUpBtn = binding.compactPlayerLayout.findViewById<ImageView>(R.id.upBtn)

        // Initialisation des vues du layout étendu
        expandedPlayPause = binding.expandedPlayerLayout.findViewById(R.id.playPauseButton)
        expandedNext = binding.expandedPlayerLayout.findViewById(R.id.nextButton)
        expandedPrevious = binding.expandedPlayerLayout.findViewById(R.id.previousButton)
        expandedSeekBar = binding.expandedPlayerLayout.findViewById(R.id.linearProgressBar)
        expandedStartTime = binding.expandedPlayerLayout.findViewById(R.id.startTextView)
        expandedEndTime = binding.expandedPlayerLayout.findViewById(R.id.endTextView)
        expandedSongTitle = binding.expandedPlayerLayout.findViewById(R.id.songTitleTextView)
        expandedArtist = binding.expandedPlayerLayout.findViewById(R.id.songArtistTextView)
        expandedAlbumArt = binding.expandedPlayerLayout.findViewById(R.id.albumCoverImageView)
        val expandedDownBtn = binding.expandedPlayerLayout.findViewById<ImageView>(R.id.downCollapase)

        // Listeners
        compactUpBtn.setOnClickListener {
            binding.expandedPlayerLayout.startAnimation(slideUpAnimation)
            binding.compactPlayerLayout.visibility = View.GONE
            binding.expandedPlayerLayout.visibility = View.VISIBLE
        }

        expandedDownBtn.setOnClickListener {
            binding.expandedPlayerLayout.startAnimation(slideDownAnimation)
            binding.compactPlayerLayout.visibility = View.VISIBLE
            binding.expandedPlayerLayout.visibility = View.GONE
        }

        expandedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicPlayerManager.seekTo(progress.toLong())
                    updateStartTimeTextView()
                    updateEndTimeTextView()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        startUpdatingSeekBarProgress()

        expandedPlayPause.setOnClickListener { handlePlayPauseClick(expandedPlayPause) }
        compactPlayPause.setOnClickListener { handlePlayPauseClick(compactPlayPause) }
        expandedNext.setOnClickListener { handleNextClick() }
        compactNext.setOnClickListener { handleNextClick() }
        expandedPrevious.setOnClickListener { handlePreviousClick() }
        compactPrevious.setOnClickListener { handlePreviousClick() }
    }

    private fun hasPermissions(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, REQUEST_PERMISSION_CODE)
        } else {
            loadSongs()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun loadSongs() {
        viewModel.showtheList(requireContext()).observe(viewLifecycleOwner, Observer {
            adapter.setList(it ?: emptyList())
            binding.rvSongList.adapter = adapter
        })
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
        }
    }

    private fun handlePlayPauseClick(button: ImageView) {
        if (musicPlayerManager.isPlaying()) {
            musicPlayerManager.pauseSong(button)
        } else {
            musicPlayerManager.resumeSong(button)
        }
        updateUI()
    }

    private fun handlePreviousClick() {
        if (currentSongPosition > 0) {
            currentSongPosition--
        } else {
            currentSongPosition = adapter.itemCount - 1
        }
        val previousSong = adapter.getItem(currentSongPosition)
        musicPlayerManager.playSong(previousSong, expandedPlayPause, expandedSongTitle, expandedArtist, expandedAlbumArt)
        updateCompactPlayerUI()
    }

    private fun handleNextClick() {
        if (currentSongPosition < adapter.itemCount - 1) {
            currentSongPosition++
        } else {
            currentSongPosition = 0
        }
        val nextSong = adapter.getItem(currentSongPosition)
        musicPlayerManager.playSong(nextSong, expandedPlayPause, expandedSongTitle, expandedArtist, expandedAlbumArt)
        updateCompactPlayerUI()
    }

    override fun onSongClicked(position: Int, song: Song) {
        currentSongPosition = position
        musicPlayerManager.playSong(song, expandedPlayPause, expandedSongTitle, expandedArtist, expandedAlbumArt)
        expandedSeekBar.max = musicPlayerManager.getDuration().toInt()
        binding.compactPlayerLayout.visibility = View.GONE
        binding.expandedPlayerLayout.startAnimation(slideUpAnimation)
        binding.expandedPlayerLayout.visibility = View.VISIBLE
        updateUI()
    }

    override fun onSongCompletion() {
        handleNextClick()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBarRunnable)
        musicPlayerManager.releaseMediaPlayer()
    }

    private fun updateUI() {
        updateStartTimeTextView()
        updateEndTimeTextView()
        musicPlayerManager.updatePlayPauseButtonUI(expandedPlayPause)
        updateCompactPlayerUI()
    }

    private fun updateCompactPlayerUI() {
        musicPlayerManager.updatePlayPauseButtonUI(compactPlayPause)
        musicPlayerManager.animateSongNameScroll(compactSongTitle)
    }

    private fun updateStartTimeTextView() {
        val currentTime = musicPlayerManager.getCurrentPosition()
        expandedStartTime.text = formatTime(currentTime)
    }

    private fun updateEndTimeTextView() {
        val duration = musicPlayerManager.getDuration()
        val currentTime = musicPlayerManager.getCurrentPosition()
        expandedEndTime.text = "-${formatTime(duration - currentTime)}"
    }

    private fun formatTime(millis: Long): String {
        val minutes = millis / (1000 * 60)
        val seconds = (millis / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun startUpdatingSeekBarProgress() {
        handler.post(updateSeekBarRunnable)
    }

    private lateinit var slideUpAnimation: Animation
    private lateinit var slideDownAnimation: Animation

    private fun initAnimations() {
        slideUpAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_up)
        slideDownAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_down)
    }
}