package edu.usf.sas.pal.muser.ui.screens.search

import android.view.View
import edu.usf.sas.pal.muser.ui.screens.album.menu.AlbumArtistMenuContract
import edu.usf.sas.pal.muser.ui.screens.album.menu.AlbumMenuContract
import edu.usf.sas.pal.muser.ui.screens.songs.menu.SongMenuContract

interface SearchView : SongMenuContract.View, AlbumMenuContract.View, AlbumArtistMenuContract.View {

    fun setLoading(loading: Boolean)

    fun setData(searchResult: SearchResult)

    fun setFilterFuzzyChecked(checked: Boolean)

    fun setFilterArtistsChecked(checked: Boolean)

    fun setFilterAlbumsChecked(checked: Boolean)

    fun showPlaybackError()

    fun goToArtist(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist, transitionView: View)

    fun goToAlbum(album: edu.usf.sas.pal.muser.model.Album, transitionView: View)

    fun showUpgradeDialog()
}