package edu.usf.sas.pal.muser.ui.screens.drawer

import edu.usf.sas.pal.muser.ui.screens.playlist.menu.PlaylistMenuContract

interface DrawerView :
    edu.usf.sas.pal.muser.ui.views.PurchaseView,
    PlaylistMenuContract.View {

    fun setPlaylistItems(playlists: List<edu.usf.sas.pal.muser.model.Playlist>)

    fun closeDrawer()

    fun setDrawerItemSelected(@edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent.Type type: Int)
}