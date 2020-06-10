package edu.usf.sas.pal.muser.ui.screens.songs.list

import edu.usf.sas.pal.muser.data.SongsRepository
import edu.usf.sas.pal.muser.ui.screens.songs.list.SongListContract.View
import edu.usf.sas.pal.muser.ui.screens.songs.menu.SongMenuContract
import edu.usf.sas.pal.muser.ui.screens.songs.menu.SongMenuPresenter
import javax.inject.Inject

class SongListPresenter @Inject constructor(
        private val songsRepository: SongsRepository,
        private val mediaManager: edu.usf.sas.pal.muser.playback.MediaManager,
        private val sortManager: edu.usf.sas.pal.muser.utils.sorting.SortManager,
        private val settingsManager: edu.usf.sas.pal.muser.utils.SettingsManager,
        private val songMenuPresenter: SongMenuPresenter
) : edu.usf.sas.pal.muser.ui.common.Presenter<View>(),
    SongListContract.Presenter,
    SongMenuContract.Presenter by songMenuPresenter {

    private var songs = mutableListOf<edu.usf.sas.pal.muser.model.Song>()

    override fun bindView(view: View) {
        super.bindView(view)
        songMenuPresenter.bindView(view)
    }

    override fun unbindView(view: View) {
        super.unbindView(view)
        songMenuPresenter.unbindView(view)
    }

    override fun loadSongs(scrollToTop: Boolean) {
        addDisposable(songsRepository.getSongs()
            .map { songs ->
                val songs = songs.toMutableList()

                sortManager.sortSongs(songs)

                if (!sortManager.songsAscending) {
                    songs.reverse()
                }
                songs
            }
            .subscribe({ songs ->
                this.songs = songs
                view?.setData(songs, scrollToTop)
            }, { error ->
                edu.usf.sas.pal.muser.utils.LogUtils.logException(TAG, "Failed to load songs", error)
            })
        )
    }

    override fun setSongsSortOrder(order: Int) {
        sortManager.songsSortOrder = order
        loadSongs(true)
        view?.invalidateOptionsMenu()
    }

    override fun setSongsAscending(ascending: Boolean) {
        sortManager.songsAscending = ascending
        loadSongs(true)
        view?.invalidateOptionsMenu()
    }

    override fun setShowArtwork(show: Boolean) {
        settingsManager.setShowArtworkInSongList(show)
        loadSongs(false)
        view?.invalidateOptionsMenu()
    }

    override fun play(song: edu.usf.sas.pal.muser.model.Song) {
        mediaManager.playAll(songs, songs.indexOf(song), true) {
            view?.showPlaybackError()
        }
    }

    override fun shuffleAll() {
        mediaManager.shuffleAll(songsRepository.getSongs().firstOrError()) {
            view?.showPlaybackError()
        }
    }

    companion object {
        const val TAG = "SongListPresenter"
    }

}

