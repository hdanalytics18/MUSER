package edu.usf.sas.pal.muser.ui.screens.album.detail

import edu.usf.sas.pal.muser.data.Repository.SongsRepository
import edu.usf.sas.pal.muser.ui.screens.album.menu.AlbumMenuContract
import edu.usf.sas.pal.muser.ui.screens.album.menu.AlbumMenuPresenter
import edu.usf.sas.pal.muser.ui.screens.songs.menu.SongMenuContract
import edu.usf.sas.pal.muser.ui.screens.songs.menu.SongMenuPresenter
import edu.usf.sas.pal.muser.utils.extensions.getSongsSingle
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class AlbumDetailPresenter @AssistedInject constructor(
        private val mediaManager: edu.usf.sas.pal.muser.playback.MediaManager,
        private val songsRepository: SongsRepository,
        private val sortManager: edu.usf.sas.pal.muser.utils.sorting.SortManager,
        private val albumsMenuPresenter: AlbumMenuPresenter,
        private val songsMenuPresenter: SongMenuPresenter,
        @Assisted private val album: edu.usf.sas.pal.muser.model.Album
) : edu.usf.sas.pal.muser.ui.common.Presenter<AlbumDetailView>(),
    AlbumMenuContract.Presenter by albumsMenuPresenter,
    SongMenuContract.Presenter by songsMenuPresenter {

    @AssistedInject.Factory
    interface Factory {
        fun create(album: edu.usf.sas.pal.muser.model.Album): AlbumDetailPresenter
    }

    private var songs: MutableList<edu.usf.sas.pal.muser.model.Song> = mutableListOf()

    override fun bindView(view: AlbumDetailView) {
        super.bindView(view)

        songsMenuPresenter.bindView(view)
        albumsMenuPresenter.bindView(view)
    }

    override fun unbindView(view: AlbumDetailView) {
        super.unbindView(view)

        songsMenuPresenter.unbindView(view)
        albumsMenuPresenter.unbindView(view)
    }

    fun loadData() {
        addDisposable(
            album.getSongsSingle(songsRepository)
                .map { it.toMutableList() }
                .doOnSuccess { sortSongs(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { songs ->
                    this.songs = songs
                    view?.setData(songs)
                }
        )
    }

    fun closeContextualToolbar() {
        view?.closeContextualToolbar()
    }

    fun shuffleAll() {
        mediaManager.shuffleAll(songs) {
            view?.onPlaybackFailed()
        }
    }

    fun play(song: edu.usf.sas.pal.muser.model.Song) {
        mediaManager.playAll(songs, songs.indexOf(song), true) {
            view?.onPlaybackFailed()
        }
    }

    private fun sortSongs(songs: MutableList<edu.usf.sas.pal.muser.model.Song>) {
        @edu.usf.sas.pal.muser.utils.sorting.SortManager.SongSort val songSort = sortManager.albumDetailSongsSortOrder

        val songsAscending = sortManager.albumDetailSongsAscending

        sortManager.sortSongs(songs, songSort)
        if (!songsAscending) {
            songs.reverse()
        }
    }

    override fun <T> transform(src: Single<List<T>>, dst: (List<T>) -> Unit) {
        addDisposable(
            src
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                    { items -> dst(items) },
                    { error -> edu.usf.sas.pal.muser.utils.LogUtils.logException(SongMenuPresenter.TAG, "Failed to transform src single", error) }
                )
        )
    }
}