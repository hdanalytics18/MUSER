package edu.usf.sas.pal.muser.ui.screens.album.menu

import edu.usf.sas.pal.muser.utils.menu.albumartist.AlbumArtistMenuCallbacks

interface AlbumArtistMenuContract {

    interface View {

        fun presentCreatePlaylistDialog(songs: List<edu.usf.sas.pal.muser.model.Song>)

        fun onSongsAddedToPlaylist(playlist: edu.usf.sas.pal.muser.model.Playlist, numSongs: Int)

        fun onSongsAddedToQueue(numSongs: Int)

        fun onPlaybackFailed()

        fun presentTagEditorDialog(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist)

        fun presentArtistDeleteDialog(albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>)

        fun presentAlbumArtistInfoDialog(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist)

        fun presentArtworkEditorDialog(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist)
    }

    interface Presenter : AlbumArtistMenuCallbacks

}