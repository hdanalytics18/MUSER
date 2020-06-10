package edu.usf.sas.pal.muser.ui.screens.suggested

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.RequestManager
import edu.usf.sas.pal.muser.R
import edu.usf.sas.pal.muser.R.string
import edu.usf.sas.pal.muser.data.Repository
import edu.usf.sas.pal.muser.ui.dialog.AlbumBiographyDialog
import edu.usf.sas.pal.muser.ui.dialog.SongInfoDialog
import edu.usf.sas.pal.muser.ui.screens.playlist.detail.PlaylistDetailFragment
import edu.usf.sas.pal.muser.ui.screens.playlist.dialog.CreatePlaylistDialog
import edu.usf.sas.pal.muser.ui.screens.suggested.SuggestedPresenter.SuggestedData
import edu.usf.sas.pal.muser.utils.RingtoneManager
import edu.usf.sas.pal.muser.utils.extensions.share
import edu.usf.sas.pal.muser.utils.menu.album.AlbumMenuUtils
import edu.usf.sas.pal.muser.utils.menu.song.SongMenuUtils
import edu.usf.sas.pal.muser.utils.playlists.FavoritesPlaylistManager
import edu.usf.sas.pal.muser.utils.playlists.PlaylistMenuHelper
import edu.usf.sas.pal.muser.utils.withArgs
import dagger.android.support.AndroidSupportInjection
import io.reactivex.disposables.CompositeDisposable
import java.util.ArrayList
import javax.inject.Inject

class SuggestedFragment :
    edu.usf.sas.pal.muser.ui.common.BaseFragment(),
    edu.usf.sas.pal.muser.ui.modelviews.SuggestedHeaderView.ClickListener,
    edu.usf.sas.pal.muser.ui.modelviews.AlbumView.ClickListener,
    SuggestedContract.View {

    @Inject lateinit var presenter: SuggestedPresenter

    @Inject lateinit var songsRepository: Repository.SongsRepository

    @Inject lateinit var sortManager: edu.usf.sas.pal.muser.utils.sorting.SortManager

    @Inject lateinit var settingsManager: edu.usf.sas.pal.muser.utils.SettingsManager

    @Inject lateinit var favoritesPlaylistManager: FavoritesPlaylistManager

    @Inject lateinit var playlistMenuHelper: PlaylistMenuHelper

    @Inject lateinit var requestManager: RequestManager

    private lateinit var adapter: edu.usf.sas.simplecityapps.recycler_adapter.adapter.ViewModelAdapter

    private lateinit var favoriteRecyclerView: edu.usf.sas.pal.muser.ui.modelviews.HorizontalRecyclerView

    private lateinit var mostPlayedRecyclerView: edu.usf.sas.pal.muser.ui.modelviews.HorizontalRecyclerView

    private val disposables = CompositeDisposable()

    private val refreshDisposables = CompositeDisposable()

    private var suggestedClickListener: SuggestedClickListener? = null

    interface SuggestedClickListener {

        fun onAlbumArtistClicked(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist, transitionView: View)

        fun onAlbumClicked(album: edu.usf.sas.pal.muser.model.Album, transitionView: View)
    }


    // Lifecycle

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)

        if (parentFragment is SuggestedClickListener) {
            suggestedClickListener = parentFragment as SuggestedClickListener?
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = edu.usf.sas.simplecityapps.recycler_adapter.adapter.ViewModelAdapter()
        mostPlayedRecyclerView = edu.usf.sas.pal.muser.ui.modelviews.HorizontalRecyclerView("SuggestedFragment - mostPlayed")
        favoriteRecyclerView = edu.usf.sas.pal.muser.ui.modelviews.HorizontalRecyclerView("SuggestedFragment - favorite")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_suggested, container, false) as RecyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view as RecyclerView

        val spanCount = if (edu.usf.sas.pal.muser.utils.ShuttleUtils.isTablet(context!!)) 12 else 6

        val gridLayoutManager = GridLayoutManager(context, spanCount)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                if (!adapter.items.isEmpty() && position >= 0) {
                    val item = adapter.items[position]
                    if (item is edu.usf.sas.pal.muser.ui.modelviews.HorizontalRecyclerView
                        || item is edu.usf.sas.pal.muser.ui.modelviews.SuggestedHeaderView
                        || item is edu.usf.sas.pal.muser.ui.modelviews.AlbumView && item.getViewType() == edu.usf.sas.pal.muser.ui.adapters.ViewType.ALBUM_LIST
                        || item is edu.usf.sas.pal.muser.ui.modelviews.AlbumView && item.getViewType() == edu.usf.sas.pal.muser.ui.adapters.ViewType.ALBUM_LIST_SMALL
                        || item is edu.usf.sas.pal.muser.ui.modelviews.EmptyView
                    ) {
                        return spanCount
                    }
                    if (item is edu.usf.sas.pal.muser.ui.modelviews.AlbumView && item.getViewType() == edu.usf.sas.pal.muser.ui.adapters.ViewType.ALBUM_CARD_LARGE) {
                        return 3
                    }
                }

                return 2
            }
        }

        view.addItemDecoration(edu.usf.sas.pal.muser.ui.views.SuggestedDividerDecoration(resources))
        view.setRecyclerListener(edu.usf.sas.simplecityapps.recycler_adapter.recyclerview.RecyclerListener())
        view.layoutManager = gridLayoutManager
        view.adapter = adapter

        presenter.bindView(this)
    }

    override fun onResume() {
        super.onResume()

        presenter.loadData()
    }

    override fun onPause() {

        disposables.clear()

        refreshDisposables.clear()

        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        presenter.unbindView(this)
    }

    override fun onDetach() {
        super.onDetach()

        suggestedClickListener = null
    }

    inner class SongClickListener(val songs: List<edu.usf.sas.pal.muser.model.Song>) : edu.usf.sas.pal.muser.ui.modelviews.SuggestedSongView.ClickListener {

        override fun onSongClick(song: edu.usf.sas.pal.muser.model.Song, holder: edu.usf.sas.pal.muser.ui.modelviews.SuggestedSongView.ViewHolder) {
            mediaManager.playAll(songs, songs.indexOf(song), true) {
                onPlaybackFailed()
            }
        }

        override fun onSongOverflowClicked(v: View, position: Int, song: edu.usf.sas.pal.muser.model.Song) {
            val popupMenu = PopupMenu(context!!, v)
            SongMenuUtils.setupSongMenu(popupMenu, false, true, playlistMenuHelper)
            popupMenu.setOnMenuItemClickListener(SongMenuUtils.getSongMenuClickListener(song, presenter))
            popupMenu.show()
        }
    }


    // AlbumView.ClickListener implementation

    override fun onAlbumClick(position: Int, albumView: edu.usf.sas.pal.muser.ui.modelviews.AlbumView, viewHolder: edu.usf.sas.pal.muser.ui.modelviews.AlbumView.ViewHolder) {
        suggestedClickListener?.onAlbumClicked(albumView.album, viewHolder.imageOne)
    }

    override fun onAlbumLongClick(position: Int, albumView: edu.usf.sas.pal.muser.ui.modelviews.AlbumView): Boolean {
        return false
    }

    override fun onAlbumOverflowClicked(v: View, album: edu.usf.sas.pal.muser.model.Album) {
        val menu = PopupMenu(context!!, v)
        AlbumMenuUtils.setupAlbumMenu(menu, playlistMenuHelper, true)
        menu.setOnMenuItemClickListener(AlbumMenuUtils.getAlbumMenuClickListener(album, presenter))
        menu.show()
    }

    override fun onSuggestedHeaderClick(suggestedHeader: edu.usf.sas.pal.muser.model.SuggestedHeader) {
        navigationController.pushViewController(PlaylistDetailFragment.newInstance(suggestedHeader.playlist), "PlaylistListFragment")
    }


    // SuggestedContract.View implementation

    override fun setData(suggestedData: SuggestedData) {

        val viewModels = ArrayList<edu.usf.sas.simplecityapps.recycler_adapter.model.ViewModel<*>>()

        if (suggestedData.mostPlayedSongs.isNotEmpty()) {
            val mostPlayedHeader = edu.usf.sas.pal.muser.model.SuggestedHeader(getString(string.mostplayed), getString(string.suggested_most_played_songs_subtitle), suggestedData.mostPlayedPlaylist)
            val mostPlayedHeaderView = edu.usf.sas.pal.muser.ui.modelviews.SuggestedHeaderView(mostPlayedHeader)
            mostPlayedHeaderView.setClickListener(this)
            viewModels.add(mostPlayedHeaderView)
            viewModels.add(mostPlayedRecyclerView)

            val songClickListener = SongClickListener(suggestedData.mostPlayedSongs)

            mostPlayedRecyclerView.setItems(suggestedData.mostPlayedSongs
                .map { song ->
                    val suggestedSongView = edu.usf.sas.pal.muser.ui.modelviews.SuggestedSongView(song, requestManager, settingsManager)
                    suggestedSongView.setClickListener(songClickListener)
                    suggestedSongView
                })
        }

        if (suggestedData.recentlyPlayedAlbums.isNotEmpty()) {
            val recentlyPlayedHeader = edu.usf.sas.pal.muser.model.SuggestedHeader(getString(string.suggested_recent_title), getString(string.suggested_recent_subtitle), suggestedData.recentlyPlayedPlaylist)
            val recentlyPlayedHeaderView = edu.usf.sas.pal.muser.ui.modelviews.SuggestedHeaderView(recentlyPlayedHeader)
            recentlyPlayedHeaderView.setClickListener(this)
            viewModels.add(recentlyPlayedHeaderView)

            viewModels.addAll(
                suggestedData.recentlyPlayedAlbums
                    .map { album ->
                        val albumView = edu.usf.sas.pal.muser.ui.modelviews.AlbumView(album, edu.usf.sas.pal.muser.ui.adapters.ViewType.ALBUM_LIST_SMALL, requestManager, sortManager, settingsManager)
                        albumView.setClickListener(this)
                        albumView
                    }.toList()
            )
        }

        if (suggestedData.favoriteSongs.isNotEmpty()) {
            val favoriteSongsHeader = edu.usf.sas.pal.muser.model.SuggestedHeader(getString(string.fav_title), getString(string.suggested_favorite_subtitle), suggestedData.favoriteSongsPlaylist)
            val favoriteHeaderView = edu.usf.sas.pal.muser.ui.modelviews.SuggestedHeaderView(favoriteSongsHeader)
            favoriteHeaderView.setClickListener(this)
            viewModels.add(favoriteHeaderView)

            viewModels.add(favoriteRecyclerView)

            val songClickListener = SongClickListener(suggestedData.favoriteSongs)
            analyticsManager.dropBreadcrumb(TAG, "favoriteRecyclerView.setItems()")
            favoriteRecyclerView.setItems(
                suggestedData.favoriteSongs
                    .map { song ->
                        val suggestedSongView = edu.usf.sas.pal.muser.ui.modelviews.SuggestedSongView(song, requestManager, settingsManager)
                        suggestedSongView.setClickListener(songClickListener)
                        suggestedSongView
                    }.toList()
            )
        }

        if (suggestedData.recentlyAddedAlbums.isNotEmpty()) {
            val recentlyAddedHeader = edu.usf.sas.pal.muser.model.SuggestedHeader(getString(string.recentlyadded), getString(string.suggested_recently_added_subtitle), suggestedData.recentlyAddedAlbumsPlaylist)
            val recentlyAddedHeaderView = edu.usf.sas.pal.muser.ui.modelviews.SuggestedHeaderView(recentlyAddedHeader)
            recentlyAddedHeaderView.setClickListener(this)
            viewModels.add(recentlyAddedHeaderView)

            viewModels.addAll(
                suggestedData.recentlyAddedAlbums
                    .map { album ->
                        val albumView = edu.usf.sas.pal.muser.ui.modelviews.AlbumView(album, edu.usf.sas.pal.muser.ui.adapters.ViewType.ALBUM_CARD, requestManager, sortManager, settingsManager)
                        albumView.setClickListener(this)
                        albumView
                    }.toList()
            )
        }

        if (viewModels.isEmpty()) {
            refreshDisposables.add(adapter.setItems(listOf<edu.usf.sas.simplecityapps.recycler_adapter.model.ViewModel<*>>(edu.usf.sas.pal.muser.ui.modelviews.EmptyView(R.string.empty_suggested))))
        } else {
            refreshDisposables.add(adapter.setItems(viewModels))
        }
    }


    // SongMenuContract.View Implementation

    override fun presentCreatePlaylistDialog(songs: List<edu.usf.sas.pal.muser.model.Song>) {
        CreatePlaylistDialog.newInstance(songs).show(childFragmentManager, "CreatePlaylistDialog")
    }

    override fun presentSongInfoDialog(song: edu.usf.sas.pal.muser.model.Song) {
        SongInfoDialog.newInstance(song).show(childFragmentManager)
    }

    override fun onSongsAddedToPlaylist(playlist: edu.usf.sas.pal.muser.model.Playlist, numSongs: Int) {
        Toast.makeText(context, context!!.resources.getQuantityString(R.plurals.NNNtrackstoplaylist, numSongs, numSongs), Toast.LENGTH_SHORT).show()
    }

    override fun onSongsAddedToQueue(numSongs: Int) {
        Toast.makeText(context, context!!.resources.getQuantityString(R.plurals.NNNtrackstoqueue, numSongs, numSongs), Toast.LENGTH_SHORT).show()
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


    // AlbumMenuContract.View Implementation

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


    // BaseFragment implementation

    override fun screenName(): String {
        return TAG
    }


    // Static

    companion object {

        private const val ARG_TITLE = "title"

        private const val TAG = "SuggestedFragment"

        fun newInstance(title: String) = SuggestedFragment().withArgs {
            putString(ARG_TITLE, title)
        }
    }
}
