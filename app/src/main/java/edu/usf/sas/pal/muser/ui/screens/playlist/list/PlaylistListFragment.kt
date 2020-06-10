package edu.usf.sas.pal.muser.ui.screens.playlist.list

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import edu.usf.sas.pal.muser.R
import edu.usf.sas.pal.muser.data.Repository
import edu.usf.sas.pal.muser.ui.dialog.WeekSelectorDialog
import edu.usf.sas.pal.muser.ui.screens.playlist.dialog.DeletePlaylistConfirmationDialog
import edu.usf.sas.pal.muser.ui.screens.playlist.dialog.M3uPlaylistDialog
import edu.usf.sas.pal.muser.ui.screens.playlist.dialog.RenamePlaylistDialog
import edu.usf.sas.pal.muser.utils.menu.playlist.PlaylistMenuUtils
import edu.usf.sas.pal.muser.utils.withArgs
import dagger.android.support.AndroidSupportInjection
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject

class PlaylistListFragment :
    edu.usf.sas.pal.muser.ui.common.BaseFragment(),
    PlaylistListContract.View,
    edu.usf.sas.pal.muser.ui.modelviews.PlaylistView.OnClickListener {

    private lateinit var adapter: edu.usf.sas.simplecityapps.recycler_adapter.adapter.ViewModelAdapter

    private var playlistClickListener: PlaylistClickListener? = null

    private val refreshDisposable: Disposable? = null

    private val disposables = CompositeDisposable()

    @Inject lateinit var presenter: PlaylistListPresenter

    @Inject lateinit var playlistsRepository: Repository.PlaylistsRepository

    interface PlaylistClickListener {

        fun onPlaylistClicked(playlist: edu.usf.sas.pal.muser.model.Playlist)
    }

    // Lifecycle

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)

        if (parentFragment is PlaylistClickListener) {
            playlistClickListener = parentFragment as PlaylistClickListener?
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = edu.usf.sas.simplecityapps.recycler_adapter.adapter.ViewModelAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recycler, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view as RecyclerView).layoutManager = LinearLayoutManager(context)
        view.setRecyclerListener(edu.usf.sas.simplecityapps.recycler_adapter.recyclerview.RecyclerListener())
        view.adapter = adapter

        presenter.bindView(this)
    }

    override fun onResume() {
        super.onResume()

        presenter.loadData()
    }

    override fun onPause() {
        super.onPause()

        refreshDisposable?.dispose()

        disposables.clear()
    }

    override fun onDestroyView() {
        presenter.unbindView(this)
        super.onDestroyView()
    }

    override fun onDetach() {
        super.onDetach()

        playlistClickListener = null
    }


    // PlaylistView.PlaylistClickListener Implementation

    override fun onPlaylistClick(position: Int, playlistView: edu.usf.sas.pal.muser.ui.modelviews.PlaylistView) {
        playlistClickListener?.onPlaylistClicked(playlistView.playlist)
    }

    override fun onPlaylistOverflowClick(position: Int, view: View, playlist: edu.usf.sas.pal.muser.model.Playlist) {
        val menu = PopupMenu(context!!, view)
        PlaylistMenuUtils.setupPlaylistMenu(menu, playlist)
        menu.setOnMenuItemClickListener(PlaylistMenuUtils.getPlaylistPopupMenuClickListener(playlist, presenter))
        menu.show()
    }


    // PlaylistListContract.View Implementation

    override fun setData(playlists: List<edu.usf.sas.pal.muser.model.Playlist>) {
        adapter.setItems(playlists.map { playlist ->
            edu.usf.sas.pal.muser.ui.modelviews.PlaylistView(playlist).apply { setListener(this@PlaylistListFragment) } as edu.usf.sas.simplecityapps.recycler_adapter.model.ViewModel<*>
        })
    }


    // PlaylistMenuContract.View Implementation

    override fun onPlaybackFailed() {
        // Todo: Improve error message
        Toast.makeText(context, R.string.empty_playlist, Toast.LENGTH_SHORT).show()
    }

    override fun presentEditDialog(playlist: edu.usf.sas.pal.muser.model.Playlist) {
        WeekSelectorDialog().show(childFragmentManager)
    }

    override fun presentRenameDialog(playlist: edu.usf.sas.pal.muser.model.Playlist) {
        RenamePlaylistDialog.newInstance(playlist).show(childFragmentManager)
    }

    override fun presentM3uDialog(playlist: edu.usf.sas.pal.muser.model.Playlist) {
        M3uPlaylistDialog.newInstance(playlist).show(childFragmentManager)
    }

    override fun presentDeletePlaylistDialog(playlist: edu.usf.sas.pal.muser.model.Playlist) {
        DeletePlaylistConfirmationDialog.newInstance(playlist).show(childFragmentManager)
    }

    override fun onSongsAddedToQueue(numSongs: Int) {
        Toast.makeText(context, context!!.resources.getQuantityString(R.plurals.NNNtrackstoqueue, numSongs, numSongs), Toast.LENGTH_SHORT).show()
    }


    // BaseFragment Implementation

    override fun screenName(): String {
        return TAG
    }

    // Static

    companion object {

        private const val TAG = "PlaylistListFragment"

        private const val ARG_TITLE = "title"

        fun newInstance(title: String) = PlaylistListFragment().withArgs {
            putString(ARG_TITLE, title)
        }
    }
}