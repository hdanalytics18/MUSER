package edu.usf.sas.pal.muser.ui.screens.album.list

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.PopupMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.RequestManager
import edu.usf.sas.pal.muser.R
import edu.usf.sas.pal.muser.R.string
import edu.usf.sas.pal.muser.data.Repository
import edu.usf.sas.pal.muser.ui.dialog.AlbumBiographyDialog
import edu.usf.sas.pal.muser.ui.screens.playlist.dialog.CreatePlaylistDialog
import edu.usf.sas.pal.muser.utils.menu.album.AlbumMenuUtils
import edu.usf.sas.pal.muser.utils.playlists.PlaylistMenuHelper
import edu.usf.sas.pal.muser.utils.withArgs
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import javax.inject.Inject

class AlbumListFragment :
    AlbumListContract.View,
    edu.usf.sas.pal.muser.ui.common.BaseFragment(),
    edu.usf.sas.pal.muser.ui.modelviews.AlbumView.ClickListener,
    edu.usf.sas.pal.muser.ui.modelviews.ShuffleView.ShuffleClickListener {

    private lateinit var albumClickListener: AlbumClickListener

    private lateinit var recyclerView: FastScrollRecyclerView

    private lateinit var layoutManager: GridLayoutManager

    private lateinit var adapter: edu.usf.sas.pal.muser.ui.adapters.SectionedAdapter

    private lateinit var spanSizeLookup: edu.usf.sas.simplecityapps.recycler_adapter.recyclerview.SpanSizeLookup

    private lateinit var shuffleView: edu.usf.sas.pal.muser.ui.modelviews.ShuffleView

    private var contextualToolbarHelper: edu.usf.sas.pal.muser.utils.ContextualToolbarHelper<edu.usf.sas.pal.muser.model.Album>? = null

    private var playlistMenuDisposable: Disposable? = null

    private var setDataDisposable: Disposable? = null

    @Inject lateinit var presenter: AlbumsPresenter

    @Inject lateinit var requestManager: RequestManager

    @Inject lateinit var sortManager: edu.usf.sas.pal.muser.utils.sorting.SortManager

    @Inject lateinit var songsRepository: Repository.SongsRepository

    @Inject lateinit var settingsManager: edu.usf.sas.pal.muser.utils.SettingsManager

    @Inject lateinit var playlistMenuHelper: PlaylistMenuHelper

    interface AlbumClickListener {
        fun onAlbumClicked(album: edu.usf.sas.pal.muser.model.Album, transitionView: View)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        AndroidSupportInjection.inject(this)

        val parentFragment = parentFragment
        if (parentFragment is AlbumClickListener) {
            albumClickListener = parentFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        adapter = edu.usf.sas.pal.muser.ui.adapters.SectionedAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        recyclerView = inflater.inflate(R.layout.fragment_recycler, container, false) as FastScrollRecyclerView
        return recyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spanCount = settingsManager.getAlbumColumnCount(context)
        layoutManager = GridLayoutManager(context, spanCount)
        spanSizeLookup = edu.usf.sas.simplecityapps.recycler_adapter.recyclerview.SpanSizeLookup(adapter, spanCount)
        spanSizeLookup.isSpanIndexCacheEnabled = true
        layoutManager.spanSizeLookup = spanSizeLookup

        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager
        recyclerView.addItemDecoration(edu.usf.sas.pal.muser.ui.views.recyclerview.GridDividerDecoration(resources, 4, true))
        recyclerView.setRecyclerListener(edu.usf.sas.simplecityapps.recycler_adapter.recyclerview.RecyclerListener())

        shuffleView = edu.usf.sas.pal.muser.ui.modelviews.ShuffleView()
        shuffleView.setTitleResId(R.string.shuffle_albums)
        shuffleView.setClickListener(this)

        presenter.bindView(this)
    }

    override fun onResume() {
        super.onResume()

        presenter.loadAlbums(false)

        if (userVisibleHint) {
            setupContextualToolbar()
        }
    }

    override fun onPause() {
        setDataDisposable?.dispose()

        playlistMenuDisposable?.dispose()

        super.onPause()
    }

    override fun onDestroyView() {
        presenter.unbindView(this)
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater!!.inflate(R.menu.menu_sort_albums, menu)
        inflater.inflate(R.menu.menu_view_as, menu)

        menu!!.addSubMenu(0, MENU_GRID_SIZE, 0, R.string.menu_grid_size)
        val subMenu = menu.findItem(MENU_GRID_SIZE).subMenu

        val spanCountArray = resources.getIntArray(R.array.span_count)
        for (i in spanCountArray.indices) {
            subMenu.add(MENU_GROUP_GRID, spanCountArray[i], i, spanCountArray[i].toString())
        }
        subMenu.setGroupCheckable(MENU_GROUP_GRID, true, true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val sortOrder = sortManager.albumsSortOrder

        when (sortOrder) {
            edu.usf.sas.pal.muser.utils.sorting.SortManager.AlbumSort.DEFAULT -> menu.findItem(R.id.sort_album_default).isChecked = true
            edu.usf.sas.pal.muser.utils.sorting.SortManager.AlbumSort.NAME -> menu.findItem(R.id.sort_album_name).isChecked = true
            edu.usf.sas.pal.muser.utils.sorting.SortManager.AlbumSort.YEAR -> menu.findItem(R.id.sort_album_year).isChecked = true
            edu.usf.sas.pal.muser.utils.sorting.SortManager.AlbumSort.ARTIST_NAME -> menu.findItem(R.id.sort_album_artist_name).isChecked = true
        }

        menu.findItem(R.id.sort_album_ascending).isChecked = sortManager.albumsAscending

        val displayType = settingsManager.getAlbumDisplayType(context)
        when (displayType) {
            edu.usf.sas.pal.muser.ui.adapters.ViewType.ALBUM_LIST -> menu.findItem(R.id.view_as_list).isChecked = true
            edu.usf.sas.pal.muser.ui.adapters.ViewType.ALBUM_GRID -> menu.findItem(R.id.view_as_grid).isChecked = true
            edu.usf.sas.pal.muser.ui.adapters.ViewType.ALBUM_CARD -> menu.findItem(R.id.view_as_grid_card).isChecked = true
            edu.usf.sas.pal.muser.ui.adapters.ViewType.ALBUM_PALETTE -> menu.findItem(R.id.view_as_grid_palette).isChecked = true
        }

        val gridMenuItem = menu.findItem(MENU_GRID_SIZE)
        if (displayType == edu.usf.sas.pal.muser.ui.adapters.ViewType.ALBUM_LIST) {
            gridMenuItem.isVisible = false
        } else {
            gridMenuItem.isVisible = true
            gridMenuItem.subMenu?.findItem(settingsManager.getAlbumColumnCount(context))?.isChecked = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.sort_album_default -> presenter.setAlbumsSortOrder(edu.usf.sas.pal.muser.utils.sorting.SortManager.AlbumSort.DEFAULT)
            R.id.sort_album_name -> presenter.setAlbumsSortOrder(edu.usf.sas.pal.muser.utils.sorting.SortManager.AlbumSort.NAME)
            R.id.sort_album_year -> presenter.setAlbumsSortOrder(edu.usf.sas.pal.muser.utils.sorting.SortManager.AlbumSort.YEAR)
            R.id.sort_album_artist_name -> presenter.setAlbumsSortOrder(edu.usf.sas.pal.muser.utils.sorting.SortManager.AlbumSort.ARTIST_NAME)
            R.id.sort_album_ascending -> presenter.setAlbumsAscending(!item.isChecked)
            R.id.view_as_list -> {
                val viewType = edu.usf.sas.pal.muser.ui.adapters.ViewType.ALBUM_LIST
                settingsManager.setAlbumDisplayType(viewType)
                setupListSpan()
                updateViewType(viewType)
            }
            R.id.view_as_grid -> {
                val viewType = edu.usf.sas.pal.muser.ui.adapters.ViewType.ALBUM_GRID
                settingsManager.setAlbumDisplayType(viewType)
                setupGridSpan()
                updateViewType(viewType)
            }
            R.id.view_as_grid_card -> {
                val viewType = edu.usf.sas.pal.muser.ui.adapters.ViewType.ALBUM_CARD
                settingsManager.setAlbumDisplayType(viewType)
                setupGridSpan()
                updateViewType(viewType)
            }
            R.id.view_as_grid_palette -> {
                val viewType = edu.usf.sas.pal.muser.ui.adapters.ViewType.ALBUM_PALETTE
                settingsManager.setAlbumDisplayType(viewType)
                setupGridSpan()
                updateViewType(viewType)
            }
        }

        if (item.groupId == MENU_GROUP_GRID) {
            settingsManager.setAlbumColumnCount(context, item.itemId)
            spanSizeLookup.setSpanCount(item.itemId)
            (recyclerView.layoutManager as GridLayoutManager).spanCount = settingsManager.getAlbumColumnCount(context)
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
        }

        activity!!.invalidateOptionsMenu()

        return super.onOptionsItemSelected(item)
    }

    private fun setupGridSpan() {
        val spanCount = settingsManager.getAlbumColumnCount(context)
        spanSizeLookup.setSpanCount(spanCount)
        layoutManager.spanCount = spanCount
    }

    private fun setupListSpan() {
        val spanCount = resources.getInteger(R.integer.list_num_columns)
        spanSizeLookup.setSpanCount(spanCount)
        layoutManager.spanCount = spanCount
    }

    private fun updateViewType(@edu.usf.sas.pal.muser.ui.adapters.ViewType viewType: Int) {
        adapter.items
            .filter { viewModel -> viewModel is edu.usf.sas.pal.muser.ui.modelviews.AlbumView }
            .forEach { viewModel -> (viewModel as edu.usf.sas.pal.muser.ui.modelviews.AlbumView).viewType = viewType }
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
    }

    override fun onAlbumClick(position: Int, albumView: edu.usf.sas.pal.muser.ui.modelviews.AlbumView, viewHolder: edu.usf.sas.pal.muser.ui.modelviews.AlbumView.ViewHolder) {
        if (!contextualToolbarHelper!!.handleClick(albumView, albumView.album)) {
            albumClickListener.onAlbumClicked(albumView.album, viewHolder.imageOne)
        }
    }

    override fun onAlbumLongClick(position: Int, albumView: edu.usf.sas.pal.muser.ui.modelviews.AlbumView): Boolean {
        return contextualToolbarHelper!!.handleLongClick(albumView, albumView.album)
    }

    override fun onAlbumOverflowClicked(view: View, album: edu.usf.sas.pal.muser.model.Album) {
        val menu = PopupMenu(context!!, view)
        menu.inflate(R.menu.menu_album)
        val subMenu = menu.menu.findItem(R.id.addToPlaylist).subMenu
        playlistMenuHelper.createPlaylistMenu(subMenu)
        menu.setOnMenuItemClickListener(
            AlbumMenuUtils.getAlbumMenuClickListener(album, presenter)
        )
        menu.show()
    }

    override fun onShuffleItemClick() {
        // Note: For album-shuffle mode, we don't actually turn shuffle on.
        mediaManager.shuffleMode = edu.usf.sas.pal.muser.playback.QueueManager.ShuffleMode.OFF

        mediaManager.playAll(songsRepository.getSongs(null as Function1<edu.usf.sas.pal.muser.model.Song, Boolean>?)
            .firstOrError()
            .map { songs -> edu.usf.sas.pal.muser.utils.Operators.albumShuffleSongs(songs, sortManager) }) {
            // Todo: Show playback failed toast
            Unit
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            setupContextualToolbar()
        } else {
            contextualToolbarHelper?.finish()
        }
    }

    private fun setupContextualToolbar() {
        val contextualToolbar = edu.usf.sas.pal.muser.ui.views.ContextualToolbar.findContextualToolbar(this)
        if (contextualToolbar != null) {
            contextualToolbar.menu.clear()
            contextualToolbar.inflateMenu(R.menu.context_menu_general)
            val submenu = contextualToolbar.menu.findItem(R.id.addToPlaylist).subMenu
            playlistMenuDisposable?.dispose()

            playlistMenuDisposable = playlistMenuHelper.createUpdatingPlaylistMenu(submenu)
                .doOnError { throwable -> edu.usf.sas.pal.muser.utils.LogUtils.logException(TAG, "setupContextualToolbar error", throwable) }
                .onErrorComplete()
                .subscribe()

            contextualToolbar.setOnMenuItemClickListener(
                AlbumMenuUtils.getAlbumMenuClickListener(
                    Single.defer { Single.just(contextualToolbarHelper!!.items) },
                    presenter
                )
            )

            contextualToolbarHelper = edu.usf.sas.pal.muser.utils.ContextualToolbarHelper(context!!, contextualToolbar, object : edu.usf.sas.pal.muser.utils.ContextualToolbarHelper.Callback {

                override fun notifyItemChanged(viewModel: edu.usf.sas.pal.muser.ui.modelviews.SelectableViewModel) {
                    val index = adapter.items.indexOf(viewModel as edu.usf.sas.simplecityapps.recycler_adapter.model.ViewModel<*>)
                    if (index >= 0) {
                        adapter.notifyItemChanged(index, 0)
                    }
                }

                override fun notifyDatasetChanged() {
                    adapter.notifyItemRangeChanged(0, adapter.items.size, 0)
                }
            })
        }
    }

    // AlbumListContract.View Implementation

    override fun setData(albums: List<edu.usf.sas.pal.muser.model.Album>, scrollToTop: Boolean) {
        setDataDisposable?.dispose()

        if (albums.isEmpty()) {
            setDataDisposable = adapter.setItems(listOf(edu.usf.sas.pal.muser.ui.modelviews.EmptyView(string.empty_albums)))
        } else {
            val viewModels = albums.map { album ->
                val albumView = edu.usf.sas.pal.muser.ui.modelviews.AlbumView(album, settingsManager.getAlbumDisplayType(context), requestManager, sortManager, settingsManager)
                albumView.setClickListener(this)
                albumView as edu.usf.sas.simplecityapps.recycler_adapter.model.ViewModel<*>
            }.toMutableList()

            viewModels.add(0, shuffleView)

            setDataDisposable = adapter.setItems(viewModels, object : edu.usf.sas.simplecityapps.recycler_adapter.adapter.CompletionListUpdateCallbackAdapter() {
                override fun onComplete() {
                    super.onComplete()
                    if (scrollToTop) {
                        recyclerView.smoothScrollToPosition(0)
                    }
                }
            })
        }
    }

    override fun invalidateOptionsMenu() {
        activity?.invalidateOptionsMenu()
    }

    // AlbumMenuContract.View Implementation

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

    // BaseFragment Implementation

    override fun screenName(): String {
        return TAG
    }

    // Static

    companion object {

        private const val TAG = "AlbumListFragment"

        private const val ARG_TITLE = "title"

        private const val MENU_GRID_SIZE = 100
        private const val MENU_GROUP_GRID = 1

        fun newInstance(title: String) = AlbumListFragment().withArgs {
            putString(ARG_TITLE, title)
        }
    }
}
