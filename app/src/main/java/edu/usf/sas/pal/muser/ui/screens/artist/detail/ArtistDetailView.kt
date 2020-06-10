package edu.usf.sas.pal.muser.ui.screens.artist.detail

import edu.usf.sas.pal.muser.ui.screens.album.menu.AlbumArtistMenuContract
import edu.usf.sas.pal.muser.ui.screens.album.menu.AlbumMenuContract
import edu.usf.sas.pal.muser.ui.screens.songs.menu.SongMenuContract

interface ArtistDetailView :
    SongMenuContract.View,
    AlbumMenuContract.View,
    AlbumArtistMenuContract.View {

    fun setData(albums: List<edu.usf.sas.pal.muser.model.Album>, songs: List<edu.usf.sas.pal.muser.model.Song>)

    fun closeContextualToolbar()
}