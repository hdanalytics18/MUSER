package edu.usf.sas.pal.muser.ui.screens.genre.detail

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.SharedElementCallback
import android.support.v4.util.Pair
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.Toolbar
import android.transition.Transition
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import edu.usf.sas.afollestad.aesthetic.Rx.distinctToMainThread
import com.annimon.stream.Stream
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.cast.framework.CastButtonFactory
import edu.usf.sas.pal.muser.R
import edu.usf.sas.pal.muser.data.Repository
import edu.usf.sas.pal.muser.ui.dialog.AlbumBiographyDialog
import edu.usf.sas.pal.muser.ui.dialog.SongInfoDialog
import edu.usf.sas.pal.muser.ui.screens.album.detail.AlbumDetailFragment
import edu.usf.sas.pal.muser.ui.screens.playlist.dialog.CreatePlaylistDialog
import edu.usf.sas.pal.muser.utils.RingtoneManager
import edu.usf.sas.pal.muser.utils.StringUtils
import edu.usf.sas.pal.muser.utils.extensions.getSongsSingle
import edu.usf.sas.pal.muser.utils.extensions.share
import edu.usf.sas.pal.muser.utils.menu.album.AlbumMenuUtils
import edu.usf.sas.pal.muser.utils.menu.genre.GenreMenuUtils
import edu.usf.sas.pal.muser.utils.menu.song.SongMenuUtils
import edu.usf.sas.pal.muser.utils.playlists.PlaylistManager
import edu.usf.sas.pal.muser.utils.playlists.PlaylistMenuHelper
import edu.usf.sas.pal.muser.utils.sorting.AlbumSortHelper
import edu.usf.sas.pal.muser.utils.sorting.SongSortHelper
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_detail.background
import kotlinx.android.synthetic.main.fragment_detail.fab
import kotlinx.android.synthetic.main.fragment_detail.recyclerView
import kotlinx.android.synthetic.main.fragment_detail.textProtectionScrim
import kotlinx.android.synthetic.main.fragment_detail.textProtectionScrim2
import kotlinx.android.synthetic.main.fragment_detail.toolbar
import kotlinx.android.synthetic.main.fragment_detail.toolbar_layout
import java.util.ArrayList
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_detail.contextualToolbar as ctxToolbar

@SuppressLint("RestrictedApi")
class GenreDetailFragment :
    edu.usf.sas.pal.muser.ui.common.BaseFragment(),
    GenreDetailView,
    Toolbar.OnMenuItemClickListener,
    edu.usf.sas.pal.muser.ui.screens.drawer.DrawerLockManager.DrawerLock,
        edu.usf.sas.pal.muser.ui.views.ContextualToolbarHost {

    private lateinit var genre: edu.usf.sas.pal.muser.model.Genre

    private lateinit var adapter: edu.usf.sas.simplecityapps.recycler_adapter.adapter.ViewModelAdapter

    private val disposables = CompositeDisposable()

    private var collapsingToolbarTextColor: ColorStateList? = null
    private var collapsingToolbarSubTextColor: ColorStateList? = null

    private val emptyView = edu.usf.sas.pal.muser.ui.modelviews.EmptyView(R.string.empty_songlist)

    private val horizontalRecyclerView = edu.usf.sas.pal.muser.ui.modelviews.HorizontalRecyclerView("BaseDetail - horizontal")

    private var setHorizontalItemsDisposable: Disposable? = null

    private var setItemsDisposable: Disposable? = null

    private var contextualToolbarHelper: edu.usf.sas.pal.muser.utils.ContextualToolbarHelper<Single<List<edu.usf.sas.pal.muser.model.Song>>>? = null

    lateinit var presenter: GenreDetailPresenter

    @Inject lateinit var presenterFactory: GenreDetailPresenter.Factory

    @Inject lateinit var requestManager: RequestManager

    @Inject lateinit var sortManager: edu.usf.sas.pal.muser.utils.sorting.SortManager

    @Inject lateinit var settingsManager: edu.usf.sas.pal.muser.utils.SettingsManager

    @Inject lateinit var songsRepository: Repository.SongsRepository

    @Inject lateinit var playlistManager: PlaylistManager

    @Inject lateinit var playlistMenuHelper: PlaylistMenuHelper

    private var isFirstLoad = true

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)

        genre = arguments!!.getSerializable(ARG_GENRE) as edu.usf.sas.pal.muser.model.Genre

        presenter = presenterFactory.create(genre)
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        requestManager = Glide.with(this)

        setHasOptionsMenu(true)

        setEnterSharedElementCallback(enterSharedElementCallback)

        isFirstLoad = true

        adapter = edu.usf.sas.simplecityapps.recycler_adapter.adapter.ViewModelAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener { navigationController.popViewController() }

        if (edu.usf.sas.pal.muser.utils.ShuttleUtils.canDrawBehindStatusBar()) {
            toolbar.layoutParams.height = (edu.usf.sas.pal.muser.utils.ActionBarUtils.getActionBarHeight(context!!) + edu.usf.sas.pal.muser.utils.ActionBarUtils.getStatusBarHeight(context!!)).toInt()
            toolbar.setPadding(toolbar.paddingLeft, (toolbar.paddingTop + edu.usf.sas.pal.muser.utils.ActionBarUtils.getStatusBarHeight(context!!)).toInt(), toolbar.paddingRight, toolbar.paddingBottom)
        }

        setupToolbarMenu(toolbar!!)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setRecyclerListener(edu.usf.sas.simplecityapps.recycler_adapter.recyclerview.RecyclerListener())
        recyclerView.adapter = adapter

        if (isFirstLoad) {
            recyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_from_bottom)
        }

        toolbar_layout.title = genre.name
        toolbar_layout.setSubtitle(null)
        toolbar_layout.setExpandedTitleTypeface(edu.usf.sas.pal.muser.utils.TypefaceManager.getInstance().getTypeface(context, edu.usf.sas.pal.muser.utils.TypefaceManager.SANS_SERIF_LIGHT))
        toolbar_layout.setCollapsedTitleTypeface(edu.usf.sas.pal.muser.utils.TypefaceManager.getInstance().getTypeface(context, edu.usf.sas.pal.muser.utils.TypefaceManager.SANS_SERIF))

        setupContextualToolbar()

        val transitionName = arguments!!.getString(ARG_TRANSITION_NAME)
        ViewCompat.setTransitionName(background!!, transitionName)

        if (isFirstLoad) {
            fab.visibility = View.GONE
        }

        fab.setOnClickListener {
            presenter.shuffleAll()
        }

        if (transitionName == null) {
            fadeInUi()
        }

        loadBackgroundImage()

        disposables.add(edu.usf.sas.afollestad.aesthetic.Aesthetic.get(context)
            .colorPrimary()
            .compose(distinctToMainThread())
            .subscribe { primaryColor ->
                toolbar_layout!!.setContentScrimColor(primaryColor!!)
                toolbar_layout!!.setBackgroundColor(primaryColor)
            })

        presenter.bindView(this)
    }

    override fun onResume() {
        super.onResume()

        presenter.loadData()

        edu.usf.sas.pal.muser.ui.screens.drawer.DrawerLockManager.getInstance().addDrawerLock(this)
    }

    override fun onPause() {

        edu.usf.sas.pal.muser.ui.screens.drawer.DrawerLockManager.getInstance().removeDrawerLock(this)

        super.onPause()
    }

    override fun onDestroyView() {
        setItemsDisposable?.dispose()

        setHorizontalItemsDisposable?.dispose()

        disposables.clear()

        presenter.unbindView(this)

        isFirstLoad = false

        super.onDestroyView()
    }

    private fun setupToolbarMenu(toolbar: Toolbar) {
        toolbar.inflateMenu(R.menu.menu_detail_sort)

        if (edu.usf.sas.pal.muser.cast.CastManager.isCastAvailable(context!!, settingsManager)) {
            val menuItem = CastButtonFactory.setUpMediaRouteButton(context, toolbar.menu, R.id.media_route_menu_item)
            menuItem.isVisible = true
        }

        toolbar.setOnMenuItemClickListener(this)

        // Create playlist menu
        val sub = toolbar.menu.findItem(R.id.addToPlaylist).subMenu
        disposables.add(playlistMenuHelper.createUpdatingPlaylistMenu(sub).subscribe())

        // Inflate sorting menus
        val item = toolbar.menu.findItem(R.id.sorting)

        activity!!.menuInflater.inflate(R.menu.menu_detail_sort_albums, item.subMenu)
        activity!!.menuInflater.inflate(R.menu.menu_detail_sort_songs, item.subMenu)

        AlbumSortHelper.updateAlbumSortMenuItems(toolbar.menu, sortManager.genreDetailAlbumsSortOrder, sortManager.genreDetailAlbumsAscending)
        SongSortHelper.updateSongSortMenuItems(toolbar.menu, sortManager.genreDetailSongsSortOrder, sortManager.genreDetailSongsAscending)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (!GenreMenuUtils.getGenreClickListener(genre, presenter).onMenuItemClick(item)) {
            val albumSortOrder = AlbumSortHelper.handleAlbumDetailMenuSortOrderClicks(item)
            if (albumSortOrder != null) {
                sortManager.genreDetailAlbumsSortOrder = albumSortOrder
                presenter.loadData()
            }
            val albumsAsc = AlbumSortHelper.handleAlbumDetailMenuSortOrderAscClicks(item)
            if (albumsAsc != null) {
                sortManager.genreDetailAlbumsAscending = albumsAsc
                presenter.loadData()
            }
            val songSortOrder = SongSortHelper.handleSongMenuSortOrderClicks(item)
            if (songSortOrder != null) {
                sortManager.genreDetailSongsSortOrder = songSortOrder
                presenter.loadData()
            }
            val songsAsc = SongSortHelper.handleSongDetailMenuSortOrderAscClicks(item)
            if (songsAsc != null) {
                sortManager.genreDetailSongsAscending = songsAsc
                presenter.loadData()
            }

            AlbumSortHelper.updateAlbumSortMenuItems(toolbar!!.menu, sortManager.genreDetailAlbumsSortOrder, sortManager.genreDetailAlbumsAscending)
            SongSortHelper.updateSongSortMenuItems(toolbar!!.menu, sortManager.genreDetailSongsSortOrder, sortManager.genreDetailSongsAscending)
        }

        return super.onOptionsItemSelected(item)
    }

    private fun loadBackgroundImage() {

        val width = edu.usf.sas.pal.muser.utils.ResourceUtils.getScreenSize().width + edu.usf.sas.pal.muser.utils.ResourceUtils.toPixels(60f)
        val height = resources.getDimensionPixelSize(R.dimen.header_view_height)

        requestManager.load<edu.usf.sas.pal.muser.model.ArtworkProvider>(null as edu.usf.sas.pal.muser.model.ArtworkProvider?)
            // Need to override the height/width, as the shared element transition tricks Glide into thinking this ImageView has
            // the same dimensions as the ImageView that the transition starts with.
            // So we'll set it to screen width (plus a little extra, which might fix an issue on some devices..)
            .override(width, height)
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
            .priority(Priority.HIGH)
            .placeholder(edu.usf.sas.pal.muser.utils.PlaceholderProvider.getInstance(context).getPlaceHolderDrawable(genre.name, true, settingsManager))
            .centerCrop()
            .animate(edu.usf.sas.pal.muser.glide.utils.AlwaysCrossFade(false))
            .into(background)
    }

    override fun setSharedElementEnterTransition(transition: Any?) {
        super.setSharedElementEnterTransition(transition)
        (transition as Transition).addListener(sharedElementEnterTransitionListenerAdapter)
    }

    internal fun fadeInUi() {

        if (textProtectionScrim == null || textProtectionScrim2 == null || fab == null) {
            return
        }

        //Fade in the text protection scrim
        textProtectionScrim!!.alpha = 0f
        textProtectionScrim!!.visibility = View.VISIBLE
        var fadeAnimator = ObjectAnimator.ofFloat(textProtectionScrim, View.ALPHA, 0f, 1f)
        fadeAnimator.duration = 600
        fadeAnimator.start()

        textProtectionScrim2!!.alpha = 0f
        textProtectionScrim2!!.visibility = View.VISIBLE
        fadeAnimator = ObjectAnimator.ofFloat(textProtectionScrim2, View.ALPHA, 0f, 1f)
        fadeAnimator.duration = 600
        fadeAnimator.start()

        //Fade & grow the FAB
        fab!!.alpha = 0f
        fab!!.visibility = View.VISIBLE

        fadeAnimator = ObjectAnimator.ofFloat(fab, View.ALPHA, 0.5f, 1f)
        val scaleXAnimator = ObjectAnimator.ofFloat(fab, View.SCALE_X, 0f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(fab, View.SCALE_Y, 0f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fadeAnimator, scaleXAnimator, scaleYAnimator)
        animatorSet.duration = 250
        animatorSet.start()
    }

    override fun getContextualToolbar(): edu.usf.sas.pal.muser.ui.views.ContextualToolbar? {
        return ctxToolbar as edu.usf.sas.pal.muser.ui.views.ContextualToolbar
    }

    private fun setupContextualToolbar() {

        val contextualToolbar = edu.usf.sas.pal.muser.ui.views.ContextualToolbar.findContextualToolbar(this)
        if (contextualToolbar != null) {

            contextualToolbar.setTransparentBackground(true)

            contextualToolbar.menu.clear()
            contextualToolbar.inflateMenu(R.menu.context_menu_general)
            val sub = contextualToolbar.menu.findItem(R.id.addToPlaylist).subMenu
            disposables.add(playlistMenuHelper.createUpdatingPlaylistMenu(sub).subscribe())

            contextualToolbar.setOnMenuItemClickListener(
                SongMenuUtils.getSongMenuClickListener(Single.defer { edu.usf.sas.pal.muser.utils.Operators.reduceSongSingles(contextualToolbarHelper!!.items) }, presenter)
            )

            contextualToolbarHelper = object : edu.usf.sas.pal.muser.utils.ContextualToolbarHelper<Single<List<edu.usf.sas.pal.muser.model.Song>>>(context!!, contextualToolbar, object : edu.usf.sas.pal.muser.utils.ContextualToolbarHelper.Callback {

                override fun notifyItemChanged(viewModel: edu.usf.sas.pal.muser.ui.modelviews.SelectableViewModel) {
                    if (adapter.items.contains(viewModel as edu.usf.sas.simplecityapps.recycler_adapter.model.ViewModel<*>)) {
                        val index = adapter.items.indexOf(viewModel)
                        if (index >= 0) {
                            adapter.notifyItemChanged(index, 0)
                        }
                    } else if (horizontalRecyclerView.viewModelAdapter.items.contains(viewModel)) {
                        val index = horizontalRecyclerView.viewModelAdapter.items.indexOf(viewModel)
                        if (index >= 0) {
                            horizontalRecyclerView.viewModelAdapter.notifyItemChanged(index, 0)
                        }
                    }
                }

                override fun notifyDatasetChanged() {
                    adapter.notifyItemRangeChanged(0, adapter.items.size, 0)
                    horizontalRecyclerView.viewModelAdapter.notifyItemRangeChanged(0, horizontalRecyclerView.viewModelAdapter.items.size, 0)
                }
            }) {
                override fun start() {
                    super.start()
                    // Need to hide the collapsed text, as it overlaps the contextual toolbar
                    collapsingToolbarTextColor = toolbar_layout!!.collapsedTitleTextColor
                    collapsingToolbarSubTextColor = toolbar_layout!!.collapsedSubTextColor
                    toolbar_layout!!.setCollapsedTitleTextColor(0x01FFFFFF)
                    toolbar_layout!!.setCollapsedSubTextColor(0x01FFFFFF)

                    toolbar!!.visibility = View.GONE
                }

                override fun finish() {
                    if (toolbar_layout != null && collapsingToolbarTextColor != null && collapsingToolbarSubTextColor != null) {
                        toolbar_layout!!.collapsedTitleTextColor = collapsingToolbarTextColor!!
                        toolbar_layout!!.collapsedSubTextColor = collapsingToolbarSubTextColor!!
                    }
                    if (toolbar != null) {
                        toolbar!!.visibility = View.VISIBLE
                    }
                    super.finish()
                }
            }
        }
    }

    private val sharedElementEnterTransitionListenerAdapter: edu.usf.sas.pal.muser.ui.common.TransitionListenerAdapter
        get() = object : edu.usf.sas.pal.muser.ui.common.TransitionListenerAdapter() {
            override fun onTransitionEnd(transition: Transition) {
                transition.removeListener(this)
                fadeInUi()
            }
        }

    private val enterSharedElementCallback = object : SharedElementCallback() {
        override fun onSharedElementStart(sharedElementNames: List<String>?, sharedElements: List<View>?, sharedElementSnapshots: List<View>?) {
            super.onSharedElementStart(sharedElementNames, sharedElements, sharedElementSnapshots)

            fab?.visibility = View.GONE
        }
    }

    private var songClickListener: edu.usf.sas.pal.muser.ui.modelviews.SongView.ClickListener = object : edu.usf.sas.pal.muser.ui.modelviews.SongView.ClickListener {
        override fun onSongClick(position: Int, songView: edu.usf.sas.pal.muser.ui.modelviews.SongView) {
            if (!contextualToolbarHelper!!.handleClick(songView, Single.just(listOf(songView.song)))) {
                presenter.songClicked(songView.song)
            }
        }

        override fun onSongLongClick(position: Int, songView: edu.usf.sas.pal.muser.ui.modelviews.SongView): Boolean {
            return contextualToolbarHelper!!.handleLongClick(songView, Single.just(listOf(songView.song)))
        }

        override fun onSongOverflowClick(position: Int, v: View, song: edu.usf.sas.pal.muser.model.Song) {
            val popupMenu = PopupMenu(v.context, v)
            SongMenuUtils.setupSongMenu(popupMenu, false, true, playlistMenuHelper)
            popupMenu.setOnMenuItemClickListener(SongMenuUtils.getSongMenuClickListener(song, presenter))
            popupMenu.show()
        }

        override fun onStartDrag(holder: edu.usf.sas.pal.muser.ui.modelviews.SongView.ViewHolder) {

        }
    }

    private var albumClickListener: edu.usf.sas.pal.muser.ui.modelviews.AlbumView.ClickListener = object : edu.usf.sas.pal.muser.ui.modelviews.AlbumView.ClickListener {

        override fun onAlbumClick(position: Int, albumView: edu.usf.sas.pal.muser.ui.modelviews.AlbumView, viewHolder: edu.usf.sas.pal.muser.ui.modelviews.AlbumView.ViewHolder) {
            if (!contextualToolbarHelper!!.handleClick(albumView, albumView.album.getSongsSingle(songsRepository))) {
                pushDetailFragment(AlbumDetailFragment.newInstance(albumView.album, ViewCompat.getTransitionName(viewHolder.imageOne)!!), viewHolder.imageOne)
            }
        }

        override fun onAlbumLongClick(position: Int, albumView: edu.usf.sas.pal.muser.ui.modelviews.AlbumView): Boolean {
            return contextualToolbarHelper!!.handleLongClick(albumView, albumView.album.getSongsSingle(songsRepository))
        }

        override fun onAlbumOverflowClicked(v: View, album: edu.usf.sas.pal.muser.model.Album) {
            val popupMenu = PopupMenu(v.context, v)
            AlbumMenuUtils.setupAlbumMenu(popupMenu, playlistMenuHelper, true)
            popupMenu.setOnMenuItemClickListener(AlbumMenuUtils.getAlbumMenuClickListener(album, presenter))
            popupMenu.show()
        }
    }

    private fun pushDetailFragment(fragment: Fragment, transitionView: View?) {

        val transitions = ArrayList<Pair<View, String>>()

        if (transitionView != null) {
            val transitionName = ViewCompat.getTransitionName(transitionView)
            transitions.add(Pair(transitionView, transitionName))
            //            transitions.add(new Pair<>(toolbar, "toolbar"));

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val moveTransition = TransitionInflater.from(context).inflateTransition(R.transition.image_transition)
                fragment.sharedElementEnterTransition = moveTransition
                fragment.sharedElementReturnTransition = moveTransition
            }
        }

        navigationController.pushViewController(fragment, "DetailFragment", transitions)
    }


    // GenreDetailView implementation

    override fun closeContextualToolbar() {
        contextualToolbarHelper?.finish()
    }

    override fun setData(albums: List<edu.usf.sas.pal.muser.model.Album>, songs: List<edu.usf.sas.pal.muser.model.Song>) {
        val viewModels = ArrayList<edu.usf.sas.simplecityapps.recycler_adapter.model.ViewModel<*>>()

        if (!albums.isEmpty()) {

            val items = ArrayList<edu.usf.sas.simplecityapps.recycler_adapter.model.ViewModel<*>>()

            setHorizontalItemsDisposable?.dispose()
            analyticsManager.dropBreadcrumb(TAG, "horizontalRecyclerView.setItems()")
            setHorizontalItemsDisposable = horizontalRecyclerView.setItems(albums
                .map { album ->
                    val horizontalAlbumView = edu.usf.sas.pal.muser.ui.modelviews.HorizontalAlbumView(album, requestManager, sortManager, settingsManager)
                    horizontalAlbumView.setClickListener(albumClickListener)
                    horizontalAlbumView.showYear(true)
                    horizontalAlbumView
                })
            items.add(edu.usf.sas.pal.muser.ui.modelviews.SubheaderView(StringUtils.makeAlbumsLabel(context!!, albums.size)))
            items.add(horizontalRecyclerView)

            viewModels.addAll(items)
        }

        if (!songs.isEmpty()) {
            val items = ArrayList<edu.usf.sas.simplecityapps.recycler_adapter.model.ViewModel<*>>()

            items.add(edu.usf.sas.pal.muser.ui.modelviews.SubheaderView(StringUtils.makeSongsAndTimeLabel(context!!, songs.size, Stream.of(songs).mapToLong { song -> song.duration / 1000 }.sum())))

            items.addAll(
                songs
                    .map { song ->
                        val songView = edu.usf.sas.pal.muser.ui.modelviews.SongView(song, requestManager, sortManager, settingsManager)
                        songView.setClickListener(songClickListener)
                        songView
                    }.toList()
            )

            viewModels.addAll(items)
        }
        if (viewModels.isEmpty()) {
            viewModels.add(emptyView)
        }

        setItemsDisposable = adapter.setItems(viewModels, object : edu.usf.sas.simplecityapps.recycler_adapter.adapter.CompletionListUpdateCallbackAdapter() {
            override fun onComplete() {
                recyclerView?.scheduleLayoutAnimation()
            }
        })
    }

    override fun fadeInSlideShowAlbum(previousAlbum: edu.usf.sas.pal.muser.model.Album?, newAlbum: edu.usf.sas.pal.muser.model.Album) {
        //This crazy business is what's required to have a smooth Glide crossfade with no 'white flicker'
        requestManager.load(newAlbum)
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
            .priority(Priority.HIGH)
            .error(edu.usf.sas.pal.muser.utils.PlaceholderProvider.getInstance(context).getPlaceHolderDrawable(newAlbum.name, true, settingsManager))
            .centerCrop()
            .thumbnail(
                Glide
                    .with(this)
                    .load<edu.usf.sas.pal.muser.model.Album>(previousAlbum)
                    .centerCrop()
            )
            .crossFade(600)
            .into(background)
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

    override fun onPlaybackFailed() {
        // Todo: Improve error message
        Toast.makeText(context, R.string.empty_playlist, Toast.LENGTH_SHORT).show()
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


    // BaseFragment Implementation

    public override fun screenName(): String {
        return "GenreDetailFragment"
    }


    // Static

    companion object {

        private const val TAG = "BaseDetailFragment"

        private const val ARG_TRANSITION_NAME = "transition_name"

        var ARG_GENRE = "genre"

        fun newInstance(genre: edu.usf.sas.pal.muser.model.Genre): GenreDetailFragment {
            val args = Bundle()
            val fragment = GenreDetailFragment()
            args.putSerializable(ARG_GENRE, genre)
            fragment.arguments = args
            return fragment
        }
    }
}



