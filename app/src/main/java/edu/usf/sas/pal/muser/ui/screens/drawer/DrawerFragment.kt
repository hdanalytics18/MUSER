package edu.usf.sas.pal.muser.ui.screens.drawer

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bignerdranch.expandablerecyclerview.model.Parent
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import edu.usf.sas.pal.muser.R
import edu.usf.sas.pal.muser.data.Repository
import edu.usf.sas.pal.muser.ui.dialog.UpgradeDialog
import edu.usf.sas.pal.muser.ui.dialog.WeekSelectorDialog
import edu.usf.sas.pal.muser.ui.screens.nowplaying.PlayerPresenter
import edu.usf.sas.pal.muser.ui.screens.playlist.dialog.DeletePlaylistConfirmationDialog
import edu.usf.sas.pal.muser.ui.screens.playlist.dialog.M3uPlaylistDialog
import edu.usf.sas.pal.muser.ui.screens.playlist.dialog.RenamePlaylistDialog
import edu.usf.sas.pal.muser.ui.views.PlayerViewAdapter
import edu.usf.sas.pal.muser.utils.menu.playlist.PlaylistMenuUtils
import edu.usf.sas.pal.muser.utils.playlists.FavoritesPlaylistManager
import edu.usf.sas.pal.muser.utils.playlists.PlaylistManager
import dagger.android.support.AndroidSupportInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.drawer_header.artist_image
import kotlinx.android.synthetic.main.drawer_header.background_image
import kotlinx.android.synthetic.main.drawer_header.line1
import kotlinx.android.synthetic.main.drawer_header.line2
import kotlinx.android.synthetic.main.drawer_header.placeholder_text
import kotlinx.android.synthetic.main.fragment_drawer.recyclerView
import java.util.ArrayList
import javax.inject.Inject

class DrawerFragment : edu.usf.sas.pal.muser.ui.common.BaseFragment(), DrawerView, View.OnCreateContextMenuListener, edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent.ClickListener {

    private lateinit var adapter: edu.usf.sas.pal.muser.ui.screens.drawer.DrawerAdapter

    private var drawerLayout: DrawerLayout? = null

    private var playlistDrawerParent: edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent? = null

    @edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent.Type
    private var selectedDrawerParent = edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent.Type.LIBRARY

    private var currentSelectedPlaylist: edu.usf.sas.pal.muser.model.Playlist? = null

    @Inject lateinit var playerPresenter: PlayerPresenter

    @Inject lateinit var drawerPresenter: DrawerPresenter

    @Inject lateinit var billingManager: edu.usf.sas.pal.muser.billing.BillingManager

    @Inject lateinit var songsRepository: Repository.SongsRepository

    @Inject lateinit var playlistsRepository: Repository.PlaylistsRepository

    @Inject lateinit var settingsManager: edu.usf.sas.pal.muser.utils.SettingsManager

    @Inject lateinit var requestManager: RequestManager

    @Inject lateinit var playlistManager: PlaylistManager

    @Inject lateinit var favoritesPlaylistManager: FavoritesPlaylistManager

    private var backgroundPlaceholder: Drawable? = null

    private val disposables = CompositeDisposable()

    private var drawerParents: MutableList<Parent<edu.usf.sas.pal.muser.ui.screens.drawer.DrawerChild>>? = null

    // Lifecycle

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            selectedDrawerParent = savedInstanceState.getInt(STATE_SELECTED_DRAWER_PARENT, edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent.Type.LIBRARY)
            currentSelectedPlaylist = savedInstanceState.get(STATE_SELECTED_PLAYLIST) as edu.usf.sas.pal.muser.model.Playlist?
        }

        backgroundPlaceholder = ContextCompat.getDrawable(context!!, R.drawable.ic_drawer_header_placeholder)

        playlistDrawerParent = edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent.getPlaylistsParent(settingsManager)

        drawerParents = ArrayList()
        drawerParents!!.add(edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent.getLibraryParent(settingsManager))
        drawerParents!!.add(edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent.getFolderParent(context!!, settingsManager))
        drawerParents!!.add(playlistDrawerParent!!)
        drawerParents!!.add(edu.usf.sas.pal.muser.ui.screens.drawer.DrawerDivider())
        drawerParents!!.add(edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent.getSleepTimerParent(settingsManager))
        drawerParents!!.add(edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent.getEqualizerParent(settingsManager))
        drawerParents!!.add(edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent.getSettingsParent(settingsManager))
        drawerParents!!.add(edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent.getSupportParent(settingsManager))

        adapter = edu.usf.sas.pal.muser.ui.screens.drawer.DrawerAdapter(drawerParents!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_drawer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView!!.layoutManager = LinearLayoutManager(context)
        recyclerView!!.adapter = adapter

        setDrawerItemSelected(selectedDrawerParent)

        drawerPresenter.bindView(this)
        playerPresenter.bindView(playerViewAdapter)

        drawerLayout = getParentDrawerLayout(view)
    }

    override fun onResume() {
        super.onResume()

        // Todo: Move this crap to presenter
        disposables.add(edu.usf.sas.afollestad.aesthetic.Aesthetic.get(context)
            .colorPrimary()
            .compose(edu.usf.sas.afollestad.aesthetic.Rx.distinctToMainThread())
            .subscribe { color ->
                backgroundPlaceholder!!.setColorFilter(color!!, PorterDuff.Mode.MULTIPLY)
                if (mediaManager.song == null) {
                    background_image.setImageDrawable(backgroundPlaceholder)
                }
            })

        playerPresenter.updateTrackInfo()

        disposables.add(
            edu.usf.sas.pal.muser.utils.SleepTimer.getInstance().currentTimeObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ aLong ->
                    drawerParents!!
                        .forEachIndexed { i, drawerParent ->
                            if (aLong > 0 && drawerParent is edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent && drawerParent.type == edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent.Type.SLEEP_TIMER) {
                                drawerParent.setTimeRemaining(aLong!!)
                                adapter.notifyParentChanged(i)
                            }
                        }
                }, { throwable -> edu.usf.sas.pal.muser.utils.LogUtils.logException(TAG, "Error observing sleep time", throwable) })
        )

        disposables.add(
            edu.usf.sas.pal.muser.utils.SleepTimer.getInstance().timerActiveSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ active ->
                    drawerParents!!
                        .forEachIndexed { i, drawerParent ->
                            if (drawerParent is edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent && drawerParent.type == edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent.Type.SLEEP_TIMER) {
                                drawerParent.setTimerActive(active!!)
                                adapter.notifyParentChanged(i)
                            }
                        }
                },
                    { throwable -> edu.usf.sas.pal.muser.utils.LogUtils.logException(TAG, "Error observing sleep state", throwable) })
        )

        drawerParents!!
            .filter { parent -> parent is edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent }
            .forEach { parent -> (parent as edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent).setListener(this) }
    }

    override fun onPause() {
        disposables.clear()

        drawerParents!!
            .filter { parent -> parent is edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent }
            .forEach { parent -> (parent as edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent).setListener(null) }

        super.onPause()
    }

    override fun onDestroyView() {
        drawerPresenter.unbindView(this)
        playerPresenter.unbindView(playerViewAdapter)

        super.onDestroyView()
    }

    private val playerViewAdapter: PlayerViewAdapter = object : PlayerViewAdapter() {
        override fun trackInfoChanged(song: edu.usf.sas.pal.muser.model.Song?) {

            if (song == null) {
                return
            }

            line1.text = song.name
            line2.text = String.format("%s - %s", song.albumArtistName, song.albumName)
            placeholder_text.setText(R.string.app_name)

            requestManager.load(song)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .error(backgroundPlaceholder)
                .into(background_image)

            requestManager.load<edu.usf.sas.pal.muser.model.AlbumArtist>(song.albumArtist)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(edu.usf.sas.pal.muser.utils.PlaceholderProvider.getInstance(context).mediumPlaceHolderResId)
                .into(artist_image)

            if (song.name == null || song.albumName == null && song.albumArtistName == null) {
                placeholder_text.visibility = View.VISIBLE
                line1.visibility = View.GONE
                line2.visibility = View.GONE
            } else {
                placeholder_text.visibility = View.GONE
                line1.visibility = View.VISIBLE
                line2.visibility = View.VISIBLE
            }
        }
    }

    override fun onClick(drawerParent: edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent) {
        drawerPresenter.onDrawerItemClicked(drawerParent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(STATE_SELECTED_DRAWER_PARENT, selectedDrawerParent)
        outState.putSerializable(STATE_SELECTED_PLAYLIST, currentSelectedPlaylist)
    }

    internal fun onPlaylistClicked(playlist: edu.usf.sas.pal.muser.model.Playlist) {
        drawerPresenter.onPlaylistClicked(playlist)
    }

    override fun setPlaylistItems(playlists: List<edu.usf.sas.pal.muser.model.Playlist>) {

        val parentPosition = adapter.parentList.indexOf(playlistDrawerParent)

        val prevItemCount = playlistDrawerParent!!.children.size
        playlistDrawerParent!!.children.clear()
        adapter.notifyChildRangeRemoved(parentPosition, 0, prevItemCount)

        val drawerChildren = playlists
            .map { playlist ->
                val drawerChild = edu.usf.sas.pal.muser.ui.screens.drawer.DrawerChild(playlist)
                drawerChild.setListener(object : edu.usf.sas.pal.muser.ui.screens.drawer.DrawerChild.ClickListener {
                    override fun onClick(playlist: edu.usf.sas.pal.muser.model.Playlist) {
                        onPlaylistClicked(playlist)
                    }

                    override fun onOverflowClick(view: View, playlist: edu.usf.sas.pal.muser.model.Playlist) {
                        val popupMenu = PopupMenu(view.context, view)
                        PlaylistMenuUtils.setupPlaylistMenu(popupMenu, playlist)
                        popupMenu.setOnMenuItemClickListener(PlaylistMenuUtils.getPlaylistPopupMenuClickListener(playlist, drawerPresenter))
                        popupMenu.show()
                    }
                })
                drawerChild
            }.toList()

        playlistDrawerParent!!.children.addAll(drawerChildren)
        adapter.notifyChildRangeInserted(parentPosition, 0, drawerChildren.size)

        adapter.notifyParentChanged(parentPosition)
    }

    override fun closeDrawer() {
        drawerLayout?.closeDrawer(Gravity.START)
    }

    override fun setDrawerItemSelected(@edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent.Type type: Int) {
        adapter.parentList
            .forEachIndexed { i, drawerParent ->
                if (drawerParent is edu.usf.sas.pal.muser.ui.screens.drawer.DrawerParent) {
                    if (drawerParent.type == type) {
                        if (!drawerParent.isSelected) {
                            drawerParent.isSelected = true
                            adapter.notifyParentChanged(i)
                        }
                    } else {
                        if (drawerParent.isSelected) {
                            drawerParent.isSelected = false
                            adapter.notifyParentChanged(i)
                        }
                    }
                }
            }
    }

    override fun showUpgradeDialog() {
        UpgradeDialog().show(childFragmentManager)
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

    // BaseDetailFragment Implementation

    override fun screenName(): String {
        return TAG
    }

    // Static

    companion object {

        private const val TAG = "DrawerFragment"

        private const val STATE_SELECTED_DRAWER_PARENT = "selected_drawer_parent"

        private const val STATE_SELECTED_PLAYLIST = "selected_drawer_playlist"

        fun getParentDrawerLayout(v: View?): DrawerLayout? {
            if (v == null) return null

            if (v is DrawerLayout) {
                return v
            }

            return if (v.parent is View) {
                getParentDrawerLayout(v.parent as View)
            } else null
        }
    }
}
