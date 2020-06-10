package edu.usf.sas.pal.muser.ui.screens.search

import android.text.TextUtils
import edu.usf.sas.pal.muser.data.Repository
import edu.usf.sas.pal.muser.ui.screens.album.menu.AlbumArtistMenuContract
import edu.usf.sas.pal.muser.ui.screens.album.menu.AlbumArtistMenuPresenter
import edu.usf.sas.pal.muser.ui.screens.album.menu.AlbumMenuContract
import edu.usf.sas.pal.muser.ui.screens.album.menu.AlbumMenuPresenter
import edu.usf.sas.pal.muser.ui.screens.songs.menu.SongMenuContract
import edu.usf.sas.pal.muser.ui.screens.songs.menu.SongMenuPresenter
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.SingleOperator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import java.util.*
import javax.inject.Inject

class SearchPresenter @Inject
constructor(
        private val mediaManager: edu.usf.sas.pal.muser.playback.MediaManager,
        private val songsRepository: Repository.SongsRepository,
        private val albumsRepository: Repository.AlbumsRepository,
        private val albumArtistsRepository: Repository.AlbumArtistsRepository,
        private val settingsManager: edu.usf.sas.pal.muser.utils.SettingsManager,
        private val songMenuPresenter: SongMenuPresenter,
        private val albumMenuPresenter: AlbumMenuPresenter,
        private val albumArtistsMenuPresenter: AlbumArtistMenuPresenter

) : edu.usf.sas.pal.muser.ui.common.Presenter<SearchView>(),
        SongMenuContract.Presenter by songMenuPresenter,
        AlbumMenuContract.Presenter by albumMenuPresenter,
        AlbumArtistMenuContract.Presenter by albumArtistsMenuPresenter {

    private var performSearchSubscription: Disposable? = null

    private var query: String? = null

    override fun bindView(view: SearchView) {
        super.bindView(view)

        songMenuPresenter.bindView(view)
        albumMenuPresenter.bindView(view)
        albumArtistsMenuPresenter.bindView(view)

        view.setFilterFuzzyChecked(settingsManager.searchFuzzy)
        view.setFilterArtistsChecked(settingsManager.searchArtists)
        view.setFilterAlbumsChecked(settingsManager.searchAlbums)
    }

    override fun unbindView(view: SearchView) {
        super.unbindView(view)
        songMenuPresenter.unbindView(view)
        albumMenuPresenter.unbindView(view)
        albumArtistsMenuPresenter.unbindView(view)
    }

    fun queryChanged(query: String?) {
        var query = query

        if (TextUtils.isEmpty(query)) {
            query = ""
        }

        if (query == this.query) {
            return
        }

        loadData(query!!)

        this.query = query
    }

    private fun loadData(query: String) {

        val searchView = view

        if (searchView != null) {

            searchView.setLoading(true)

            //We've received a new refresh call. Unsubscribe the in-flight subscription if it exists.
            if (performSearchSubscription != null) {
                performSearchSubscription!!.dispose()
            }

            val albumArtistsObservable = if (settingsManager.searchArtists)
                albumArtistsRepository.getAlbumArtists()
                        .first(emptyList())
                        .lift(AlbumArtistFilterOperator(query))
            else
                Single.just(emptyList())

            val albumsObservable = if (settingsManager.searchAlbums)
                albumsRepository.getAlbums()
                        .first(emptyList())
                        .lift(AlbumFilterOperator(query))
            else
                Single.just(emptyList())

            val songsObservable = songsRepository.getSongs(null as Function1<edu.usf.sas.pal.muser.model.Song, Boolean>?)
                    .first(emptyList())
                    .lift(SongFilterOperator(query))

            performSearchSubscription = Single.zip<List<edu.usf.sas.pal.muser.model.AlbumArtist>, List<edu.usf.sas.pal.muser.model.Album>, List<edu.usf.sas.pal.muser.model.Song>, SearchResult>(albumArtistsObservable, albumsObservable, songsObservable, Function3 { albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>, albums: List<edu.usf.sas.pal.muser.model.Album>, songs: List<edu.usf.sas.pal.muser.model.Song> -> SearchResult(albumArtists, albums, songs) })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { searchView.setData(it) },
                            { error -> edu.usf.sas.pal.muser.utils.LogUtils.logException(TAG, "Error refreshing adapter", error) }
                    )

            addDisposable(performSearchSubscription!!)
        }
    }

    fun setSearchFuzzy(searchFuzzy: Boolean) {
        settingsManager.searchFuzzy = searchFuzzy
        loadData(query!!)
    }

    fun setSearchArtists(searchArtists: Boolean) {
        settingsManager.searchArtists = searchArtists
        loadData(query!!)
    }

    fun setSearchAlbums(searchAlbums: Boolean) {
        settingsManager.searchAlbums = searchAlbums
        loadData(query!!)
    }

    fun onSongClick(songs: List<edu.usf.sas.pal.muser.model.Song>, song: edu.usf.sas.pal.muser.model.Song) {
        val view = view

        mediaManager.playAll(songs, songs.indexOf(song), true) {
            view?.showPlaybackError()
            Unit
        }
    }

    fun onArtistClicked(albumArtistView: edu.usf.sas.pal.muser.ui.modelviews.AlbumArtistView, viewholder: edu.usf.sas.pal.muser.ui.modelviews.AlbumArtistView.ViewHolder) {
        val view = view
        view?.goToArtist(albumArtistView.albumArtist, viewholder.imageOne)
    }

    fun onAlbumClick(albumView: edu.usf.sas.pal.muser.ui.modelviews.AlbumView, viewHolder: edu.usf.sas.pal.muser.ui.modelviews.AlbumView.ViewHolder) {
        val view = view
        view?.goToAlbum(albumView.album, viewHolder.imageOne)
    }

    private inner class SongFilterOperator internal constructor(internal var filterString: String) : SingleOperator<List<edu.usf.sas.pal.muser.model.Song>, List<edu.usf.sas.pal.muser.model.Song>> {

        override fun apply(observer: SingleObserver<in List<edu.usf.sas.pal.muser.model.Song>>): SingleObserver<in List<edu.usf.sas.pal.muser.model.Song>> {
            return object : SingleObserver<List<edu.usf.sas.pal.muser.model.Song>> {
                override fun onSubscribe(d: Disposable) {
                    observer.onSubscribe(d)
                }

                override fun onSuccess(songs: List<edu.usf.sas.pal.muser.model.Song>) {
                    var songs = songs
                    val songList = songs.filter { song -> song.name != null }
                    songs = (if (settingsManager.searchFuzzy) applyJaroWinklerFilter(songList) else applySongFilter(songList)).toList()
                    observer.onSuccess(songs)
                }

                override fun onError(e: Throwable) {
                    observer.onError(e)
                }
            }
        }

        internal fun applyJaroWinklerFilter(songList: List<edu.usf.sas.pal.muser.model.Song>): List<edu.usf.sas.pal.muser.model.Song> {
            return songList.map { song -> edu.usf.sas.pal.muser.ui.screens.search.JaroWinklerObject(song, filterString, song.name) }
                    .filter { jaroWinklerObject -> jaroWinklerObject.score > SCORE_THRESHOLD || TextUtils.isEmpty(filterString) }
                    .sortedWith(Comparator { a, b -> a.`object`.compareTo(b.`object`) })
                    .sortedWith(Comparator { a, b -> java.lang.Double.compare(b.score, a.score) })
                    .map { jaroWinklerObject -> jaroWinklerObject.`object` }
        }

        internal fun applySongFilter(songStream: List<edu.usf.sas.pal.muser.model.Song>): List<edu.usf.sas.pal.muser.model.Song> {
            return songStream.filter { song -> edu.usf.sas.pal.muser.utils.StringUtils.containsIgnoreCase(song.name, filterString) }
        }
    }

    private inner class AlbumFilterOperator internal constructor(internal var filterString: String) : SingleOperator<List<edu.usf.sas.pal.muser.model.Album>, List<edu.usf.sas.pal.muser.model.Album>> {

        override fun apply(observer: SingleObserver<in List<edu.usf.sas.pal.muser.model.Album>>): SingleObserver<in List<edu.usf.sas.pal.muser.model.Album>> {
            return object : SingleObserver<List<edu.usf.sas.pal.muser.model.Album>> {
                override fun onSubscribe(d: Disposable) {
                    observer.onSubscribe(d)
                }

                override fun onSuccess(albums: List<edu.usf.sas.pal.muser.model.Album>) {
                    albums.sortedWith(Comparator { a, b -> a.compareTo(b) })
                    val albumStream = albums.filter { album -> album.name != null }
                    val filteredStream = if (settingsManager.searchFuzzy) applyJaroWinklerAlbumFilter(albumStream) else applyAlbumFilter(albumStream)
                    observer.onSuccess(filteredStream.toList())
                }

                override fun onError(e: Throwable) {
                    observer.onError(e)
                }
            }
        }

        internal fun applyJaroWinklerAlbumFilter(albums: List<edu.usf.sas.pal.muser.model.Album>): List<edu.usf.sas.pal.muser.model.Album> {
            return albums.map { album -> edu.usf.sas.pal.muser.ui.screens.search.JaroWinklerObject(album, filterString, album.name) }
                    .filter { jaroWinklerObject -> jaroWinklerObject.score > SCORE_THRESHOLD || TextUtils.isEmpty(filterString) }
                    .sortedWith(Comparator { a, b -> a.`object`.compareTo(b.`object`) })
                    .sortedWith(Comparator { a, b -> java.lang.Double.compare(b.score, a.score) })
                    .map { jaroWinklerObject -> jaroWinklerObject.`object` }
        }

        internal fun applyAlbumFilter(stream: List<edu.usf.sas.pal.muser.model.Album>): List<edu.usf.sas.pal.muser.model.Album> {
            return stream.filter { album -> edu.usf.sas.pal.muser.utils.StringUtils.containsIgnoreCase(album.name, filterString) }
        }
    }

    private inner class AlbumArtistFilterOperator internal constructor(internal var filterString: String) : SingleOperator<List<edu.usf.sas.pal.muser.model.AlbumArtist>, List<edu.usf.sas.pal.muser.model.AlbumArtist>> {

        override fun apply(observer: SingleObserver<in List<edu.usf.sas.pal.muser.model.AlbumArtist>>): SingleObserver<in List<edu.usf.sas.pal.muser.model.AlbumArtist>> {
            return object : SingleObserver<List<edu.usf.sas.pal.muser.model.AlbumArtist>> {
                override fun onSubscribe(d: Disposable) {
                    observer.onSubscribe(d)
                }

                override fun onSuccess(albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>) {
                    Collections.sort(albumArtists) { obj, albumArtist -> obj.compareTo(albumArtist) }
                    val albumArtistList = albumArtists.filter { albumArtist -> albumArtist.name != null }
                    val filteredList = if (settingsManager.searchFuzzy) applyJaroWinklerAlbumArtistFilter(albumArtistList) else applyAlbumArtistFilter(albumArtistList)
                    observer.onSuccess(filteredList.toList())
                }

                override fun onError(e: Throwable) {
                    observer.onError(e)
                }
            }
        }

        internal fun applyJaroWinklerAlbumArtistFilter(stream: List<edu.usf.sas.pal.muser.model.AlbumArtist>): List<edu.usf.sas.pal.muser.model.AlbumArtist> {
            return stream.map { albumArtist -> edu.usf.sas.pal.muser.ui.screens.search.JaroWinklerObject(albumArtist, filterString, albumArtist.name) }
                    .filter { jaroWinklerObject -> jaroWinklerObject.score > SCORE_THRESHOLD || TextUtils.isEmpty(filterString) }
                    .sortedWith(Comparator { a, b -> a.`object`.compareTo(b.`object`) })
                    .sortedWith(Comparator { a, b -> java.lang.Double.compare(b.score, a.score) })
                    .map { jaroWinklerObject -> jaroWinklerObject.`object` }
        }

        internal fun applyAlbumArtistFilter(stream: List<edu.usf.sas.pal.muser.model.AlbumArtist>): List<edu.usf.sas.pal.muser.model.AlbumArtist> {
            return stream.filter { albumArtist -> edu.usf.sas.pal.muser.utils.StringUtils.containsIgnoreCase(albumArtist.name, filterString) }
        }
    }

    override fun <T> transform(src: Single<List<T>>, dst: (List<T>) -> Unit) {
        addDisposable(
                src
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                { items -> dst(items) },
                                { error -> edu.usf.sas.pal.muser.utils.LogUtils.logException(SearchPresenter.TAG, "Failed to transform src single", error) }
                        )
        )
    }

    companion object {

        private const val TAG = "SearchPresenter"

        private const val SCORE_THRESHOLD = 0.80
    }
}
