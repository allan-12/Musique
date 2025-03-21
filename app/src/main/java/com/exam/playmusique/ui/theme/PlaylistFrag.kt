package com.exam.playmusique.ui.theme

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.exam.playmusique.MyApplication
import com.exam.playmusique.PlaylistManager
import com.exam.playmusique.R
import com.exam.playmusique.adapter.PlaylistAdapter
import com.exam.playmusique.databinding.FragmentPlaylistBinding
import com.exam.playmusique.service.MusicService
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PlaylistFrag : Fragment() {

    private lateinit var binding: FragmentPlaylistBinding
    private lateinit var playlistManager: PlaylistManager
    private lateinit var adapter: PlaylistAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playlistManager = PlaylistManager(requireContext())
        setupRecyclerView()
        setupFab()
    }

    private fun setupRecyclerView() {
        adapter = PlaylistAdapter(
            playlistManager.getAllPlaylists(),
            onClick = { playlist ->
                // Démarrer la lecture de la playlist
                val intent = Intent(requireContext(), MusicService::class.java).apply {
                    action = MusicService.ACTION_PLAY
                }
                requireContext().startService(intent)
                (requireActivity().application as MyApplication).musicPlayerManager.setPlaylistAndPlay(playlist.songs)

                // Passer la playlist à SongListFrag via Safe Args
                val action = PlaylistFragDirections.actionPlaylistFragToSongListFrag(playlist.songs.toTypedArray())
                findNavController().navigate(action)
            },
            onDelete = { playlist ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Playlist")
                    .setMessage("Are you sure you want to delete ${playlist.name}?")
                    .setPositiveButton("Delete") { _, _ ->
                        playlistManager.deletePlaylist(playlist.name)
                        adapter.updatePlaylists(playlistManager.getAllPlaylists())
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.rvPlaylists.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlaylists.adapter = adapter
    }

    private fun setupFab() {
        binding.fabCreatePlaylist.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }

    private fun showCreatePlaylistDialog() {
        val editText = layoutInflater.inflate(R.layout.dialog_edit_text, null) as android.widget.EditText
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create Playlist")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    playlistManager.createPlaylist(name)
                    adapter.updatePlaylists(playlistManager.getAllPlaylists())
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}