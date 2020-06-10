package edu.usf.sas.pal.muser.utils.menu.song

import android.support.v7.widget.PopupMenu
import android.support.v7.widget.Toolbar
import edu.usf.sas.pal.muser.R
import edu.usf.sas.pal.muser.playback.MediaManager.Defs
import edu.usf.sas.pal.muser.utils.playlists.PlaylistManager
import edu.usf.sas.pal.muser.utils.playlists.PlaylistMenuHelper
import io.reactivex.Single

object SongMenuUtils {

    const val TAG = "SongMenuUtils"

    fun setupSongMenu(
        menu: PopupMenu,
        showGoToAlbum: Boolean = true,
        showGoToArtist: Boolean = true,
        playlistMenuHelper: PlaylistMenuHelper
    ) {
        menu.inflate(R.menu.menu_song)

        if (!showGoToAlbum) {
            menu.menu.findItem(R.id.goToAlbum).isVisible = false
        }

        if (!showGoToArtist) {
            menu.menu.findItem(R.id.goToArtist).isVisible = false
        }

        // Add playlist menu
        val subMenu = menu.menu.findItem(R.id.addToPlaylist).subMenu
        playlistMenuHelper.createPlaylistMenu(subMenu)
    }

    fun getSongMenuClickListener(songs: Single<List<edu.usf.sas.pal.muser.model.Song>>, callbacks: SongsMenuCallbacks): Toolbar.OnMenuItemClickListener {
        return Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
                Defs.NEW_PLAYLIST -> {
                    callbacks.createPlaylist(songs)
                    return@OnMenuItemClickListener true
                }
                Defs.PLAYLIST_SELECTED -> {
                    callbacks.addToPlaylist(item.intent.getSerializableExtra(PlaylistManager.ARG_PLAYLIST) as edu.usf.sas.pal.muser.model.Playlist, songs)
                    return@OnMenuItemClickListener true
                }
                R.id.playNext -> {
                    callbacks.playNext(songs)
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    callbacks.addToQueue(songs)
                    return@OnMenuItemClickListener true
                }
                R.id.blacklist -> {
                    callbacks.blacklist(songs)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    callbacks.delete(songs)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }

    fun getSongMenuClickListener(song: edu.usf.sas.pal.muser.model.Song, callbacks: SongsMenuCallbacks): PopupMenu.OnMenuItemClickListener {
        return PopupMenu.OnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.playNext -> {
                    callbacks.playNext(song)
                    return@OnMenuItemClickListener true
                }
                Defs.NEW_PLAYLIST -> {
                    callbacks.createPlaylist(song)
                    return@OnMenuItemClickListener true
                }
                Defs.PLAYLIST_SELECTED -> {
                    callbacks.addToPlaylist(item.intent.getSerializableExtra(PlaylistManager.ARG_PLAYLIST) as edu.usf.sas.pal.muser.model.Playlist, song)
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    callbacks.addToQueue(song)
                    return@OnMenuItemClickListener true
                }
                R.id.editTags -> {
                    callbacks.editTags(song)
                    return@OnMenuItemClickListener true
                }
                R.id.share -> {
                    callbacks.share(song)
                    return@OnMenuItemClickListener true
                }
                R.id.ringtone -> {
                    callbacks.setRingtone(song)
                    return@OnMenuItemClickListener true
                }
                R.id.songInfo -> {
                    callbacks.songInfo(song)
                    return@OnMenuItemClickListener true
                }
                R.id.blacklist -> {
                    callbacks.blacklist(song)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    callbacks.delete(song)
                    return@OnMenuItemClickListener true
                }
                R.id.goToAlbum -> {
                    callbacks.goToAlbum(song)
                    return@OnMenuItemClickListener true
                }
                R.id.goToArtist -> {
                    callbacks.goToArtist(song)
                    return@OnMenuItemClickListener true
                }
                R.id.goToGenre -> {
                    callbacks.goToGenre(song)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }
}