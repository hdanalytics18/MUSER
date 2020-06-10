package edu.usf.sas.pal.muser.ui.screens.search

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.util.Pair
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.SearchView
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.annimon.stream.Stream
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.jakewharton.rxbinding2.support.v7.widget.RxSearchView
import edu.usf.sas.pal.muser.R
import edu.usf.sas.pal.muser.data.Repository
import edu.usf.sas.pal.muser.ui.dialog.AlbumBiographyDialog
import edu.usf.sas.pal.muser.ui.dialog.ArtistBiographyDialog
import edu.usf.sas.pal.muser.ui.dialog.SongInfoDialog
import edu.usf.sas.pal.muser.ui.dialog.UpgradeDialog
import edu.usf.sas.pal.muser.ui.screens.album.detail.AlbumDetailFragment
import edu.usf.sas.pal.muser.ui.screens.artist.detail.ArtistDetailFragment
import edu.usf.sas.pal.muser.ui.screens.playlist.dialog.CreatePlaylistDialog
import edu.usf.sas.pal.muser.utils.RingtoneManager
import edu.usf.sas.pal.muser.utils.extensions.getSongsSingle
import edu.usf.sas.pal.muser.utils.extensions.share
import edu.usf.sas.pal.muser.utils.menu.album.AlbumMenuUtils
import edu.usf.sas.pal.muser.utils.menu.albumartist.AlbumArtistMenuUtils
import edu.usf.sas.pal.muser.utils.menu.song.SongMenuUtils
import edu.usf.sas.pal.muser.utils.playlists.PlaylistMenuHelper
import edu.usf.sas.pal.muser.utils.withArgs
import io.reactivex.BackpressureStrategy
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_search.*
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_search.contextualToolbar as ctxToolbar

class SearchFragment :
        edu.usf.sas.pal.muser.ui.common.BaseFragment(),
        edu.usf.sas.pal.muser.ui.screens.search.SearchView,
        edu.usf.sas.pal.muser.ui.views.ContextualToolbarHost {

    private var query = ""

    private val adapter = edu.usf.sas.simplecityapps.recycler_adapter.adapter.ViewModelAdapter()

    private val loadingView = edu.usf.sas.pal.muser.ui.modelviews.LoadingView()

    private val disposables = CompositeDisposable()

    private var contextualToolbarHelper: edu.usf.sas.pal.muser.utils.ContextualToolbarHelper<Single<List<edu.usf.sas.pal.muser.model.Song>>>? = null

    private val emptyView = edu.usf.sas.pal.muser.ui.modelviews.EmptyView(R.string.empty_search)

    private lateinit var artistsHeader: edu.usf.sas.pal.muser.ui.modelviews.SearchHeaderView
    private lateinit var albumsHeader: edu.usf.sas.pal.muser.ui.modelviews.SearchHeaderView
    private lateinit var songsHeader: edu.usf.sas.pal.muser.ui.modelviews.SearchHeaderView

    private var prefixHighlighter: edu.usf.sas.pal.muser.format.PrefixHighlighter? = null

    @Inject
    lateinit var requestManager: RequestManager

    @Inject
    lateinit var songsRepository: Repository.SongsRepository

    @Inject
    lateinit var sortManager: edu.usf.sas.pal.muser.utils.sorting.SortManager

    @Inject
    lateinit var settingsManager: edu.usf.sas.pal.muser.utils.SettingsManager

    @Inject
    lateinit var playlistMenuHelper: PlaylistMenuHelper

    @Inject
    lateinit var presenter: SearchPresenter

    private var setDataDisposable: Disposable? = null

    private lateinit var searchView: SearchView

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefixHighlighter = edu.usf.sas.pal.muser.format.PrefixHighlighter(context)

        requestManager = Glide.with(this)

        query = arguments!!.getString(ARG_QUERY, "")

        emptyView.setHeight(edu.usf.sas.pal.muser.utils.ResourceUtils.toPixels(96f))

        artistsHeader = edu.usf.sas.pal.muser.ui.modelviews.SearchHeaderView(edu.usf.sas.pal.muser.model.Header(context!!.getString(R.string.artists_title)))
        albumsHeader = edu.usf.sas.pal.muser.ui.modelviews.SearchHeaderView(edu.usf.sas.pal.muser.model.Header(context!!.getString(R.string.albums_title)))
        songsHeader = edu.usf.sas.pal.muser.ui.modelviews.SearchHeaderView(edu.usf.sas.pal.muser.model.Header(context!!.getString(R.string.tracks_title)))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar!!.inflateMenu(R.menu.menu_search)
        toolbar!!.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.search_fuzzy -> {
                    item.isChecked = !item.isChecked
                    presenter.setSearchFuzzy(item.isChecked)
                }
                R.id.search_artist -> {
                    item.isChecked = !item.isChecked
                    presenter.setSearchArtists(item.isChecked)
                }
                R.id.search_album -> {
                    item.isChecked = !item.isChecked
                    presenter.setSearchAlbums(item.isChecked)
                }
            }
            false
        }

        setupContextualToolbar()

        val searchItem = toolbar!!.menu.findItem(R.id.search)
        searchItem.expandActionView()
        searchView = searchItem.actionView as SearchView

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return false
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                val inputMethodManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                searchItem.actionView!!.handler.postDelayed({ navigationController.popViewController() }, 150)
                return false
            }
        })

        recyclerView!!.layoutManager = LinearLayoutManager(context)
        recyclerView!!.adapter = adapter
    }

    override fun onResume() {
        super.onResume()

        presenter.bindView(this)

        disposables.add(RxSearchView.queryTextChangeEvents(searchView)
                .skip(1)
                .debounce(200, TimeUnit.MILLISECONDS)
                .toFlowable(BackpressureStrategy.LATEST)
                .subscribe { searchViewQueryTextEvent ->
                    query = searchViewQueryTextEvent.queryText().toString()
                    presenter.queryChanged(query)
                })

        presenter.queryChanged(query)
    }

    override fun onPause() {
        disposables.clear()
        presenter.unbindView(this)

        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (setDataDisposable != null) {
            setDataDisposable!!.dispose()
        }
    }

    override fun screenName(): String {
        return TAG
    }

    override fun setLoading(loading: Boolean) {
        analyticsManager.dropBreadcrumb(TAG, "setLoading..")
        adapter.setItems(listOf(loadingView))
    }

    override fun setData(searchResult: SearchResult) {
        val prefix = query.toUpperCase().toCharArray()

        val viewModels = ArrayList<edu.usf.sas.simplecityapps.recycler_adapter.model.ViewModel<*>>()

        if (!searchResult.albumArtists.isEmpty()) {
            viewModels.add(artistsHeader)
            viewModels.addAll(Stream.of(searchResult.albumArtists)
                    .map { albumArtist ->
                        val albumArtistView = edu.usf.sas.pal.muser.ui.modelviews.AlbumArtistView(albumArtist, edu.usf.sas.pal.muser.ui.adapters.ViewType.ARTIST_LIST, requestManager, sortManager, settingsManager)
                        albumArtistView.setClickListener(albumArtistClickListener)
                        albumArtistView.setPrefix(prefixHighlighter, prefix)
                        albumArtistView as edu.usf.sas.simplecityapps.recycler_adapter.model.ViewModel<*>
                    }
                    .toList())
        }

        if (!searchResult.albums.isEmpty()) {
            viewModels.add(albumsHeader)
            viewModels.addAll(Stream.of(searchResult.albums).map { album ->
                val albumView = edu.usf.sas.pal.muser.ui.modelviews.AlbumView(album, edu.usf.sas.pal.muser.ui.adapters.ViewType.ALBUM_LIST, requestManager, sortManager, settingsManager)
                albumView.setClickListener(albumViewClickListener)
                albumView.setPrefix(prefixHighlighter, prefix)
                albumView
            }.toList())
        }

        if (!searchResult.songs.isEmpty()) {
            viewModels.add(songsHeader)
            viewModels.addAll(Stream.of(searchResult.songs).map { song ->
                val songView = edu.usf.sas.pal.muser.ui.modelviews.SongView(song, requestManager, sortManager, settingsManager)
                songView.setClickListener(songViewClickListener)
                songView.setPrefix(prefixHighlighter, prefix)
                songView
            }.toList())
        }

        if (viewModels.isEmpty()) {
            viewModels.add(emptyView)
        }

        analyticsManager!!.dropBreadcrumb(TAG, "setData..")
        setDataDisposable = adapter.setItems(viewModels, object : edu.usf.sas.simplecityapps.recycler_adapter.adapter.CompletionListUpdateCallbackAdapter() {
            override fun onComplete() {
                super.onComplete()

                recyclerView!!.scrollToPosition(0)
            }
        })
    }

    override fun setFilterFuzzyChecked(checked: Boolean) {
        toolbar!!.menu.findItem(R.id.search_fuzzy).isChecked = checked
    }

    override fun setFilterArtistsChecked(checked: Boolean) {
        toolbar!!.menu.findItem(R.id.search_artist).isChecked = checked
    }

    override fun setFilterAlbumsChecked(checked: Boolean) {
        toolbar!!.menu.findItem(R.id.search_album).isChecked = checked
    }

    override fun showPlaybackError() {
        // Todo: Implement
    }


    // AlbumArtistMenuContract.View Implementation

    override fun presentCreatePlaylistDialog(songs: List<edu.usf.sas.pal.muser.model.Song>) {
        CreatePlaylistDialog.newInstance(songs).show(childFragmentManager)
    }

    override fun onSongsAddedToPlaylist(playlist: edu.usf.sas.pal.muser.model.Playlist, numSongs: Int) {
        Toast.makeText(context, context!!.resources.getQuantityString(R.plurals.NNNtrackstoplaylist, numSongs, numSongs), Toast.LENGTH_SHORT).show()
    }

    override fun onSongsAddedToQueue(numSongs: Int) {
        Toast.makeText(context, context!!.resources.getQuantityString(R.plurals.NNNtrackstoqueue, numSongs, numSongs), Toast.LENGTH_SHORT).show()
    }

    override fun onPlaybackFailed() {
        // Todo: Improve error message
        Toast.makeText(context, R.string.emptyplaylist, Toast.LENGTH_SHORT).show()
    }

    override fun presentTagEditorDialog(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
        edu.usf.sas.pal.muser.ui.screens.tagger.TaggerDialog.newInstance(albumArtist).show(childFragmentManager)
    }

    override fun presentArtistDeleteDialog(albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>) {
        edu.usf.sas.pal.muser.ui.dialog.DeleteDialog.newInstance(edu.usf.sas.pal.muser.ui.dialog.DeleteDialog.ListArtistsRef { albumArtists }).show(childFragmentManager)
    }

    override fun presentAlbumArtistInfoDialog(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
        ArtistBiographyDialog.newInstance(albumArtist).show(childFragmentManager)
    }

    override fun presentArtworkEditorDialog(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
        edu.usf.sas.pal.muser.utils.ArtworkDialog.build(context, albumArtist).show()
    }


    // AlbumMenuContract.View Implementation

    override fun presentTagEditorDialog(album: edu.usf.sas.pal.muser.model.Album) {
        edu.usf.sas.pal.muser.ui.screens.tagger.TaggerDialog.newInstance(album).show(childFragmentManager)
    }

    override fun presentDeleteAlbumsDialog(albums: List<edu.usf.sas.pal.muser.model.Album>) {
        edu.usf.sas.pal.muser.ui.dialog.DeleteDialog.newInstance(edu.usf.sas.pal.muser.ui.dialog.DeleteDialog.ListAlbumsRef { albums }).show(childFragmentManager)
    }

    override fun presentAlbumInfoDialog(album: edu.usf.sas.pal.muser.model.Album) {
        AlbumBiographyDialog.newInstance(album).show(childFragmentManager)
    }

    override fun presentArtworkEditorDialog(album: edu.usf.sas.pal.muser.model.Album) {
        edu.usf.sas.pal.muser.utils.ArtworkDialog.build(context, album).show()
    }


    // SongMenuContract.View Implementation

    override fun presentSongInfoDialog(song: edu.usf.sas.pal.muser.model.Song) {
        SongInfoDialog.newInstance(song).show(childFragmentManager)
    }

    override fun presentTagEditorDialog(song: edu.usf.sas.pal.muser.model.Song) {
        edu.usf.sas.pal.muser.ui.screens.tagger.TaggerDialog.newInstance(song).show(childFragmentManager)
    }

    override fun presentDeleteDialog(songs: List<edu.usf.sas.pal.muser.model.Song>) {
        edu.usf.sas.pal.muser.ui.dialog.DeleteDialog.newInstance(edu.usf.sas.pal.muser.ui.dialog.DeleteDialog.ListSongsRef { songs }).show(childFragmentManager)
    }

    override fun shareSong(song: edu.usf.sas.pal.muser.model.Song) {
        song.share(context!!)
    }

    override fun presentRingtonePermissionDialog() {
        RingtoneManager.getDialog(context!!).show()
    }

    override fun showRingtoneSetMessage() {
        Toast.makeText(context, R.string.ringtone_set_new, Toast.LENGTH_SHORT).show()
    }


    override fun goToArtist(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist, transitionView: View) {
        val inputMethodManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(this.view!!.windowToken, 0)
        val transitionName = ViewCompat.getTransitionName(transitionView)
        searchView.handler.postDelayed({ pushDetailFragment(ArtistDetailFragment.newInstance(albumArtist, transitionName!!), transitionView) }, 50)
    }

    override fun goToAlbum(album: edu.usf.sas.pal.muser.model.Album, transitionView: View) {
        val inputMethodManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(this.view!!.windowToken, 0)
        val transitionName = ViewCompat.getTransitionName(transitionView)
        searchView.handler.postDelayed({ pushDetailFragment(AlbumDetailFragment.newInstance(album, transitionName!!), transitionView) }, 50)
    }

    override fun showUpgradeDialog() {
        UpgradeDialog().show(childFragmentManager)
    }

    private fun pushDetailFragment(fragment: Fragment, transitionView: View?) {

        val transitions = ArrayList<Pair<View, String>>()

        if (transitionView != null) {
            val transitionName = ViewCompat.getTransitionName(transitionView)
            transitions.add(Pair(transitionView, transitionName))

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val moveTransition = TransitionInflater.from(context).inflateTransition(R.transition.image_transition)
                fragment.sharedElementEnterTransition = moveTransition
                fragment.sharedElementReturnTransition = moveTransition
            }
        }

        navigationController.pushViewController(fragment, "DetailFragment", transitions)
    }

    override fun getContextualToolbar(): edu.usf.sas.pal.muser.ui.views.ContextualToolbar? {
        return ctxToolbar as edu.usf.sas.pal.muser.ui.views.ContextualToolbar?
    }

    private fun setupContextualToolbar() {

        val contextualToolbar = edu.usf.sas.pal.muser.ui.views.ContextualToolbar.findContextualToolbar(this)
        if (contextualToolbar != null) {

            contextualToolbar.menu.clear()
            contextualToolbar.inflateMenu(R.menu.context_menu_general)
            val sub = contextualToolbar.menu.findItem(R.id.addToPlaylist).subMenu
            disposables.add(playlistMenuHelper.createUpdatingPlaylistMenu(sub).subscribe())

            contextualToolbar.setOnMenuItemClickListener(SongMenuUtils.getSongMenuClickListener(
                    Single.defer { edu.usf.sas.pal.muser.utils.Operators.reduceSongSingles(contextualToolbarHelper!!.items) },
                    presenter
            ))

            contextualToolbarHelper = object : edu.usf.sas.pal.muser.utils.ContextualToolbarHelper<Single<List<edu.usf.sas.pal.muser.model.Song>>>(context!!, contextualToolbar, object : edu.usf.sas.pal.muser.utils.ContextualToolbarHelper.Callback {

                override fun notifyItemChanged(viewModel: edu.usf.sas.pal.muser.ui.modelviews.SelectableViewModel) {
                    val index = adapter.items.indexOf(viewModel as edu.usf.sas.simplecityapps.recycler_adapter.model.ViewModel<*>)
                    if (index >= 0) {
                        adapter.notifyItemChanged(index, 0)
                    }
                }

                override fun notifyDatasetChanged() {
                    adapter.notifyItemRangeChanged(0, adapter.items.size, 0)
                }
            }) {
                override fun start() {
                    super.start()

                    toolbar!!.visibility = View.GONE
                }

                override fun finish() {
                    if (toolbar != null) {
                        toolbar!!.visibility = View.VISIBLE
                    }
                    super.finish()
                }
            }
        }
    }

    private val songViewClickListener = object : edu.usf.sas.pal.muser.ui.modelviews.SongView.ClickListener {

        override fun onSongClick(position: Int, songView: edu.usf.sas.pal.muser.ui.modelviews.SongView) {
            if (!contextualToolbarHelper!!.handleClick(songView, Single.just(listOf(songView.song)))) {
                presenter.onSongClick(
                        adapter.items
                                .filter { item -> item is edu.usf.sas.pal.muser.ui.modelviews.SongView }
                                .map { item -> (item as edu.usf.sas.pal.muser.ui.modelviews.SongView).song }.toList(),
                        songView.song
                )
            }
        }

        override fun onSongLongClick(position: Int, songView: edu.usf.sas.pal.muser.ui.modelviews.SongView): Boolean {
            return contextualToolbarHelper!!.handleLongClick(songView, Single.just(listOf(songView.song)))
        }

        override fun onSongOverflowClick(position: Int, v: View, song: edu.usf.sas.pal.muser.model.Song) {
            val menu = PopupMenu(v.context, v)
            SongMenuUtils.setupSongMenu(menu, false, true, playlistMenuHelper)
            menu.setOnMenuItemClickListener(SongMenuUtils.getSongMenuClickListener(song, presenter))
            menu.show()
        }

        override fun onStartDrag(holder: edu.usf.sas.pal.muser.ui.modelviews.SongView.ViewHolder) {

        }
    }

    private val albumViewClickListener = object : edu.usf.sas.pal.muser.ui.modelviews.AlbumView.ClickListener {
        override fun onAlbumClick(position: Int, albumView: edu.usf.sas.pal.muser.ui.modelviews.AlbumView, viewHolder: edu.usf.sas.pal.muser.ui.modelviews.AlbumView.ViewHolder) {
            if (!contextualToolbarHelper!!.handleClick(albumView, albumView.album.getSongsSingle(songsRepository))) {
                presenter.onAlbumClick(albumView, viewHolder)
            }
        }

        override fun onAlbumLongClick(position: Int, albumView: edu.usf.sas.pal.muser.ui.modelviews.AlbumView): Boolean {
            return contextualToolbarHelper!!.handleLongClick(albumView, albumView.album.getSongsSingle(songsRepository))
        }

        override fun onAlbumOverflowClicked(v: View, album: edu.usf.sas.pal.muser.model.Album) {
            val menu = PopupMenu(v.context, v)
            AlbumMenuUtils.setupAlbumMenu(menu, playlistMenuHelper, true)
            menu.setOnMenuItemClickListener(AlbumMenuUtils.getAlbumMenuClickListener(album, presenter))
            menu.show()
        }
    }

    private val albumArtistClickListener = object : edu.usf.sas.pal.muser.ui.modelviews.AlbumArtistView.ClickListener {
        override fun onAlbumArtistClick(position: Int, albumArtistView: edu.usf.sas.pal.muser.ui.modelviews.AlbumArtistView, viewholder: edu.usf.sas.pal.muser.ui.modelviews.AlbumArtistView.ViewHolder) {
            if (!contextualToolbarHelper!!.handleClick(albumArtistView, albumArtistView.albumArtist.getSongsSingle(songsRepository))) {
                presenter.onArtistClicked(albumArtistView, viewholder)
            }
        }

        override fun onAlbumArtistLongClick(position: Int, albumArtistView: edu.usf.sas.pal.muser.ui.modelviews.AlbumArtistView): Boolean {
            return contextualToolbarHelper!!.handleLongClick(albumArtistView, albumArtistView.albumArtist.getSongsSingle(songsRepository))
        }

        override fun onAlbumArtistOverflowClicked(v: View, albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
            val menu = PopupMenu(v.context, v)
            menu.inflate(R.menu.menu_artist)
            menu.setOnMenuItemClickListener(
                    AlbumArtistMenuUtils.getAlbumArtistClickListener(albumArtist, presenter))
            menu.show()
        }
    }

    companion object {

        private const val TAG = "SearchFragment"

        const val ARG_QUERY = "query"

        fun newInstance(query: String?) = SearchFragment().withArgs {
            putString(ARG_QUERY, query)
        }
    }
}
