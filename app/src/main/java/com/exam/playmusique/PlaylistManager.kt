package com.exam.playmusique


import android.content.Context
import android.content.SharedPreferences
import com.exam.playmusique.modal.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PlaylistManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("Playlists", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        const val PLAYLISTS_KEY = "playlists"
    }

    data class Playlist(
        val name: String,
        val songs: MutableList<Song> = mutableListOf()
    )

    // Récupérer toutes les playlists
    fun getAllPlaylists(): List<Playlist> {
        val json = prefs.getString(PLAYLISTS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<Playlist>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // Créer une nouvelle playlist
    fun createPlaylist(name: String) {
        val playlists = getAllPlaylists().toMutableList()
        if (playlists.none { it.name == name }) {
            playlists.add(Playlist(name))
            savePlaylists(playlists)
        }
    }

    // Ajouter une chanson à une playlist
    fun addSongToPlaylist(playlistName: String, song: Song) {
        val playlists = getAllPlaylists().toMutableList()
        val playlist = playlists.find { it.name == playlistName }
        playlist?.songs?.add(song)
        savePlaylists(playlists)
    }

    // Supprimer une chanson d’une playlist
    fun removeSongFromPlaylist(playlistName: String, song: Song) {
        val playlists = getAllPlaylists().toMutableList()
        val playlist = playlists.find { it.name == playlistName }
        playlist?.songs?.removeIf { it.id == song.id }
        savePlaylists(playlists)
    }

    // Supprimer une playlist
    fun deletePlaylist(playlistName: String) {
        val playlists = getAllPlaylists().toMutableList()
        playlists.removeIf { it.name == playlistName }
        savePlaylists(playlists)
    }

    private fun savePlaylists(playlists: List<Playlist>) {
        val json = gson.toJson(playlists)
        prefs.edit().putString(PLAYLISTS_KEY, json).apply()
    }
}