package edu.usf.sas.pal.muser.ui.screens.songs.menu

import edu.usf.sas.pal.muser.utils.menu.song.SongsMenuCallbacks

interface SongMenuContract {

    interface View {

        fun presentCreatePlaylistDialog(songs: List<edu.usf.sas.pal.muser.model.Song>)

        fun presentSongInfoDialog(song: edu.usf.sas.pal.muser.model.Song)

        fun onSongsAddedToPlaylist(playlist: edu.usf.sas.pal.muser.model.Playlist, numSongs: Int)

        fun onSongsAddedToQueue(numSongs: Int)

        fun presentTagEditorDialog(song: edu.usf.sas.pal.muser.model.Song)

        fun presentDeleteDialog(songs: List<edu.usf.sas.pal.muser.model.Song>)

        fun presentRingtonePermissionDialog()

        fun showRingtoneSetMessage()

        fun shareSong(song: edu.usf.sas.pal.muser.model.Song)
    }

    interface Presenter : SongsMenuCallbacks
}