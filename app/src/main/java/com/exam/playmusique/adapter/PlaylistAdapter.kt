package com.exam.playmusique.adapter

import com.exam.playmusique.PlaylistManager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.exam.playmusique.databinding.ItemPlaylistBinding

class PlaylistAdapter(
    private var playlists: List<PlaylistManager.Playlist>,
    private val onClick: (PlaylistManager.Playlist) -> Unit,
    private val onDelete: (PlaylistManager.Playlist) -> Unit // Callback pour la suppression
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    class PlaylistViewHolder(private val binding: ItemPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            playlist: PlaylistManager.Playlist,
            onClick: (PlaylistManager.Playlist) -> Unit,
            onDelete: (PlaylistManager.Playlist) -> Unit
        ) {
            binding.tvPlaylistName.text = playlist.name
            binding.tvSongCount.text = "${playlist.songs.size} songs"
            binding.root.setOnClickListener { onClick(playlist) }
            binding.btnDeletePlaylist.setOnClickListener { onDelete(playlist) } // Bouton de suppression
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(playlists[position], onClick, onDelete)
    }

    override fun getItemCount(): Int = playlists.size

    fun updatePlaylists(newPlaylists: List<PlaylistManager.Playlist>) {
        playlists = newPlaylists
        notifyDataSetChanged()
    }
}