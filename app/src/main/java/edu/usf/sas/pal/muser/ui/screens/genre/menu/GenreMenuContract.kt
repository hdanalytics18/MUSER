package edu.usf.sas.pal.muser.ui.screens.genre.menu

import edu.usf.sas.pal.muser.utils.menu.genre.GenreMenuCallbacks

interface GenreMenuContract {

    interface View {

        fun presentCreatePlaylistDialog(songs: List<edu.usf.sas.pal.muser.model.Song>)

        fun onSongsAddedToPlaylist(playlist: edu.usf.sas.pal.muser.model.Playlist, numSongs: Int)

        fun onSongsAddedToQueue(numSongs: Int)

        fun onPlaybackFailed()
    }

    interface Presenter : GenreMenuCallbacks

}