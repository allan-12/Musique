package com.exam.playmusique.ui.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.exam.playmusique.PlaylistManager
import com.exam.playmusique.adapter.SongAdapter
import com.exam.playmusique.adapter.onSongItemClicked
import com.exam.playmusique.databinding.FragmentPlaylistDetailBinding
import com.exam.playmusique.modal.Song

class PlaylistDetailFrag : Fragment() {

    private lateinit var binding: FragmentPlaylistDetailBinding
    private lateinit var playlistManager: PlaylistManager
    private lateinit var playlistName: String
    private lateinit var adapter: SongAdapter

    companion object {
        private const val ARG_PLAYLIST_NAME = "playlist_name"
        fun newInstance(playlistName: String) = PlaylistDetailFrag().apply {
            arguments = Bundle().apply { putString(ARG_PLAYLIST_NAME, playlistName) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistName = arguments?.getString(ARG_PLAYLIST_NAME) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaylistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playlistManager = PlaylistManager(requireContext())
        setupRecyclerView()
        binding.tvPlaylistTitle.text = playlistName
    }

    private fun setupRecyclerView() {
        val playlist = playlistManager.getAllPlaylists().find { it.name == playlistName }
        adapter = SongAdapter().apply {
            setList(playlist?.songs ?: emptyList())
            setClickListener(object : onSongItemClicked {
                override fun onSongClicked(position: Int, song: Song) {
                    // Logique pour jouer la chanson
                }
            })
        }
        binding.rvPlaylistSongs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlaylistSongs.adapter = adapter
    }

    fun addSongToPlaylist(song: Song) {
        playlistManager.addSongToPlaylist(playlistName, song)
        adapter.setList(playlistManager.getAllPlaylists().find { it.name == playlistName }?.songs ?: emptyList())
    }

    fun removeSongFromPlaylist(song: Song) {
        playlistManager.removeSongFromPlaylist(playlistName, song)
        adapter.setList(playlistManager.getAllPlaylists().find { it.name == playlistName }?.songs ?: emptyList())
    }
}