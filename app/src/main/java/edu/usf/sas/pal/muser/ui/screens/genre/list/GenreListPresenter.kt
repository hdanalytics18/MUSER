package edu.usf.sas.pal.muser.ui.screens.genre.list

import edu.usf.sas.pal.muser.data.GenresRepository
import edu.usf.sas.pal.muser.ui.screens.genre.list.GenreListContract.View
import edu.usf.sas.pal.muser.ui.screens.genre.menu.GenreMenuContract
import edu.usf.sas.pal.muser.ui.screens.genre.menu.GenreMenuPresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class GenreListPresenter @Inject constructor(
    private val genreMenuPresenter: GenreMenuPresenter,
    private val genresRepository: GenresRepository
) : edu.usf.sas.pal.muser.ui.common.Presenter<GenreListContract.View>(),
    GenreListContract.Presenter,
    GenreMenuContract.Presenter by genreMenuPresenter {

    override fun bindView(view: View) {
        super.bindView(view)

        genreMenuPresenter.bindView(view)
    }

    override fun unbindView(view: View) {
        super.unbindView(view)

        genreMenuPresenter.unbindView(view)
    }

    override fun loadGenres() {
        addDisposable(genresRepository
            .getGenres()
            .map { genres -> genres.sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compare(a.name, b.name) }) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { genres -> view?.setData(genres) },
                { error -> edu.usf.sas.pal.muser.utils.LogUtils.logException(TAG, "Error refreshing adapter items", error) }
            ))
    }

    companion object {
        const val TAG = "GenreListPresenter"
    }
}