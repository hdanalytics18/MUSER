package edu.usf.sas.pal.muser.ui.screens.playlist.menu

import edu.usf.sas.pal.muser.utils.menu.playlist.PlaylistMenuCallbacks

interface PlaylistMenuContract {

    interface View {

        fun onPlaybackFailed()

        fun onSongsAddedToQueue(numSongs: Int)

        fun presentEditDialog(playlist: edu.usf.sas.pal.muser.model.Playlist)

        fun presentRenameDialog(playlist: edu.usf.sas.pal.muser.model.Playlist)

        fun presentM3uDialog(playlist: edu.usf.sas.pal.muser.model.Playlist)

        fun presentDeletePlaylistDialog(playlist: edu.usf.sas.pal.muser.model.Playlist)
    }

    interface Presenter : PlaylistMenuCallbacks

}