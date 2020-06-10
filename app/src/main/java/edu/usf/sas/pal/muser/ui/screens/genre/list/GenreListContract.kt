package edu.usf.sas.pal.muser.ui.screens.genre.list

import edu.usf.sas.pal.muser.ui.screens.genre.menu.GenreMenuContract

interface GenreListContract {

    interface View : GenreMenuContract.View {

        fun setData(genres: List<edu.usf.sas.pal.muser.model.Genre>)
    }

    interface Presenter {

        fun loadGenres()
    }
}