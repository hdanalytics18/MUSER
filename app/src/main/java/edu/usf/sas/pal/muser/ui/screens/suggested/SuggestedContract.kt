package edu.usf.sas.pal.muser.ui.screens.suggested

import edu.usf.sas.pal.muser.ui.screens.album.menu.AlbumMenuContract
import edu.usf.sas.pal.muser.ui.screens.songs.menu.SongMenuContract
import edu.usf.sas.pal.muser.ui.screens.suggested.SuggestedPresenter.SuggestedData

interface SuggestedContract {

    interface Presenter {

        fun loadData()

    }

    interface View : AlbumMenuContract.View, SongMenuContract.View {

        fun setData(suggestedData: SuggestedData)
    }

}