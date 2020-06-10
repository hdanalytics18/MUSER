package edu.usf.sas.pal.muser.ui.screens.playlist.detail

import edu.usf.sas.pal.muser.ui.screens.playlist.menu.PlaylistMenuContract
import edu.usf.sas.pal.muser.ui.screens.songs.menu.SongMenuContract

interface PlaylistDetailView :
    PlaylistMenuContract.View,
    SongMenuContract.View {

    fun setData(data: MutableList<edu.usf.sas.pal.muser.model.Song>)

    fun closeContextualToolbar()

    fun fadeInSlideShowAlbum(previousAlbum: edu.usf.sas.pal.muser.model.Album?, newAlbum: edu.usf.sas.pal.muser.model.Album)
}