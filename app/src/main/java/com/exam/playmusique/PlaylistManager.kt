package com.exam.playmusique

import android.content.Context
import com.exam.playmusique.modal.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PlaylistManager(private val context: Context) {

    data class Playlist(val name: String, val songs: MutableList<Song>)

    private val PREFS_NAME = "PlaylistPrefs"
    private val KEY_PLAYLISTS = "playlists"
    private val gson = Gson()

    private fun getSharedPreferences() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAllPlaylists(): List<Playlist> {
        val prefs = getSharedPreferences()
        val json = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        val type = object : TypeToken<List<Playlist>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun createPlaylist(name: String) {
        val playlists = getAllPlaylists().toMutableList()
        if (playlists.none { it.name == name }) {
            playlists.add(Playlist(name, mutableListOf()))
            savePlaylists(playlists)
        }
    }

    fun addSongToPlaylist(playlistName: String, song: Song): Boolean {
        val playlists = getAllPlaylists().toMutableList()
        val playlist = playlists.find { it.name == playlistName }
        return if (playlist != null) {
            // Vérifier si la chanson existe déjà dans cette playlist (par ID ou titre/data)
            if (playlist.songs.any { it.id == song.id || (it.title == song.title && it.data == song.data) }) {
                false // Chanson déjà présente, on ne l’ajoute pas
            } else {
                playlist.songs.add(song)
                savePlaylists(playlists)
                true // Chanson ajoutée avec succès
            }
        } else {
            false // Playlist non trouvée
        }
    }

    fun removeSongFromPlaylist(playlistName: String, song: Song): Boolean {
        val playlists = getAllPlaylists().toMutableList()
        val playlist = playlists.find { it.name == playlistName }
        return if (playlist != null) {
            // Supprimer la chanson si elle existe dans la playlist (par ID ou titre/data)
            val removed = playlist.songs.removeAll { it.id == song.id || (it.title == song.title && it.data == song.data) }
            if (removed) {
                savePlaylists(playlists)
                true // Chanson supprimée avec succès
            } else {
                false // Chanson non trouvée dans la playlist
            }
        } else {
            false // Playlist non trouvée
        }
    }

    fun deletePlaylist(playlistName: String) {
        val playlists = getAllPlaylists().toMutableList()
        playlists.removeAll { it.name == playlistName }
        savePlaylists(playlists)
    }

    private fun savePlaylists(playlists: List<Playlist>) {
        val prefs = getSharedPreferences()
        val editor = prefs.edit()
        val json = gson.toJson(playlists)
        editor.putString(KEY_PLAYLISTS, json)
        editor.apply()
    }
}