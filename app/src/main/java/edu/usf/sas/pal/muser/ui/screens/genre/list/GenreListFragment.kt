package edu.usf.sas.pal.muser.ui.screens.genre.list

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import edu.usf.sas.pal.muser.R
import edu.usf.sas.pal.muser.ui.screens.playlist.dialog.CreatePlaylistDialog
import edu.usf.sas.pal.muser.ui.settings.SettingsParentFragment.ARG_TITLE
import edu.usf.sas.pal.muser.utils.menu.genre.GenreMenuUtils
import edu.usf.sas.pal.muser.utils.playlists.PlaylistMenuHelper
import edu.usf.sas.pal.muser.utils.withArgs
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import dagger.android.support.AndroidSupportInjection
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject

class GenreListFragment :
    edu.usf.sas.pal.muser.ui.common.BaseFragment(),
    edu.usf.sas.pal.muser.ui.modelviews.GenreView.ClickListener,
    GenreListContract.View {

    private var genreClickListener: GenreClickListener? = null

    private lateinit var recyclerView: FastScrollRecyclerView

    private lateinit var adapter: edu.usf.sas.pal.muser.ui.adapters.SectionedAdapter

    private var refreshDisposable: Disposable? = null

    private val disposables = CompositeDisposable()

    @Inject lateinit var presenter: GenreListPresenter

    @Inject lateinit var playlistMenuHelper: PlaylistMenuHelper

    interface GenreClickListener {
        fun onGenreClicked(genre: edu.usf.sas.pal.muser.model.Genre)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)

        if (parentFragment is GenreClickListener) {
            genreClickListener = parentFragment as GenreClickListener?
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = edu.usf.sas.pal.muser.ui.adapters.SectionedAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        recyclerView = inflater.inflate(R.layout.fragment_recycler, container, false) as FastScrollRecyclerView
        return recyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setRecyclerListener(edu.usf.sas.simplecityapps.recycler_adapter.recyclerview.RecyclerListener())
        recyclerView.adapter = adapter

        presenter.bindView(this)
    }

    override fun onResume() {
        super.onResume()

        presenter.loadGenres()
    }

    override fun onPause() {

        refreshDisposable?.dispose()

        disposables.clear()

        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        presenter.unbindView(this)
    }

    override fun onItemClick(genre: edu.usf.sas.pal.muser.model.Genre) {
        genreClickListener?.onGenreClicked(genre)
    }

    override fun onOverflowClick(v: View, genre: edu.usf.sas.pal.muser.model.Genre) {
        val popupMenu = PopupMenu(context!!, v)
        popupMenu.inflate(R.menu.menu_genre)

        // Add playlist menu
        val subMenu = popupMenu.menu.findItem(R.id.addToPlaylist).subMenu
        playlistMenuHelper.createPlaylistMenu(subMenu)

        popupMenu.setOnMenuItemClickListener(GenreMenuUtils.getGenreClickListener(genre, presenter))
        popupMenu.show()
    }

    // GenreListContract.View Implementation

    override fun setData(genres: List<edu.usf.sas.pal.muser.model.Genre>) {
        if (genres.isEmpty()) {
            adapter.setItems(listOf(edu.usf.sas.pal.muser.ui.modelviews.EmptyView(R.string.empty_genres)))
        } else {
            adapter.setItems(genres.map {
                val genreView = edu.usf.sas.pal.muser.ui.modelviews.GenreView(it)
                genreView.setClickListener(this)
                genreView
            })
        }
    }

    // GenreMenuContract.View Implementation

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

    // BaseFragment Implementation

    override fun screenName(): String {
        return TAG
    }

    // Static

    companion object {

        private const val TAG = "GenreListFragment"

        fun newInstance(title: String) = GenreListFragment().withArgs {
            putString(ARG_TITLE, title)
        }
    }
}
