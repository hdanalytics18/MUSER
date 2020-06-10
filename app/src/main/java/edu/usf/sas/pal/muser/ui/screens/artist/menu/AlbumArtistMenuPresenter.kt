package edu.usf.sas.pal.muser.ui.screens.album.menu

import edu.usf.sas.pal.muser.data.Repository
import edu.usf.sas.pal.muser.ui.screens.album.menu.AlbumArtistMenuContract.View
import edu.usf.sas.pal.muser.ui.screens.drawer.NavigationEventRelay.NavigationEvent
import edu.usf.sas.pal.muser.ui.screens.drawer.NavigationEventRelay.NavigationEvent.Type
import edu.usf.sas.pal.muser.ui.screens.songs.menu.SongMenuPresenter
import edu.usf.sas.pal.muser.utils.extensions.getSongs
import edu.usf.sas.pal.muser.utils.playlists.PlaylistManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class AlbumArtistMenuPresenter @Inject constructor(
        private val playlistManager: PlaylistManager,
        private val songsRepository: Repository.SongsRepository,
        private val mediaManager: edu.usf.sas.pal.muser.playback.MediaManager,
        private val blacklistRepository: Repository.BlacklistRepository,
        private val navigationEventRelay: edu.usf.sas.pal.muser.ui.screens.drawer.NavigationEventRelay,
        private val sortManager: edu.usf.sas.pal.muser.utils.sorting.SortManager

) : edu.usf.sas.pal.muser.ui.common.Presenter<View>(), AlbumArtistMenuContract.Presenter {

    override fun createArtistsPlaylist(albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>) {
        getSongs(albumArtists) { songs ->
            view?.presentCreatePlaylistDialog(songs)
        }
    }

    override fun addArtistsToPlaylist(playlist: edu.usf.sas.pal.muser.model.Playlist, albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>) {
        getSongs(albumArtists) { songs ->
            playlistManager.addToPlaylist(playlist, songs) { numSongs ->
                view?.onSongsAddedToPlaylist(playlist, numSongs)
            }
        }
    }

    override fun addArtistsToQueue(albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>) {
        getSongs(albumArtists) { songs ->
            mediaManager.addToQueue(songs) { numSongs ->
                view?.onSongsAddedToQueue(numSongs)
            }
        }
    }

    override fun playArtistsNext(albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>) {
        getSongs(albumArtists) { songs ->
            mediaManager.playNext(songs) { numSongs ->
                view?.onSongsAddedToQueue(numSongs)
            }
        }
    }

    override fun play(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
        mediaManager.playAll(albumArtist.getSongsSingle(songsRepository)) { view?.onPlaybackFailed() }
    }

    override fun editTags(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
        view?.presentTagEditorDialog(albumArtist)
    }

    override fun albumArtistInfo(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
        view?.presentAlbumArtistInfoDialog(albumArtist)
    }

    override fun editArtwork(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
        view?.presentArtworkEditorDialog(albumArtist)
    }

    override fun blacklistArtists(albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>) {
        getSongs(albumArtists) { songs -> blacklistRepository.addAllSongs(songs) }
    }

    override fun deleteArtists(albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>) {
        view?.presentArtistDeleteDialog(albumArtists)
    }

    override fun goToArtist(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
        navigationEventRelay.sendEvent(NavigationEvent(Type.GO_TO_ARTIST, albumArtist, true))
    }

    override fun albumShuffle(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
        mediaManager.playAll(albumArtist.getSongs(songsRepository)
            .map { songs -> edu.usf.sas.pal.muser.utils.Operators.albumShuffleSongs(songs, sortManager) }) {
            view?.onPlaybackFailed()
            Unit
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

    private fun getSongs(albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>, onSuccess: (songs: List<edu.usf.sas.pal.muser.model.Song>) -> Unit) {
        addDisposable(
            albumArtists.getSongs(songsRepository)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    onSuccess,
                    { error -> edu.usf.sas.pal.muser.utils.LogUtils.logException(TAG, "Failed to retrieve songs", error) }
                )
        )
    }

    companion object {
        const val TAG = "AlbumMenuContract"
    }

}