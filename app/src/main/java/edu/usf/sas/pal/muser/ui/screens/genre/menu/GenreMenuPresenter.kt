package edu.usf.sas.pal.muser.ui.screens.genre.menu

import android.content.Context
import edu.usf.sas.pal.muser.ui.screens.album.menu.AlbumMenuPresenter
import edu.usf.sas.pal.muser.utils.extensions.getSongs
import edu.usf.sas.pal.muser.utils.playlists.PlaylistManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class GenreMenuPresenter @Inject constructor(
        private val context: Context,
        private val mediaManager: edu.usf.sas.pal.muser.playback.MediaManager,
        private val playlistManager: PlaylistManager
) : edu.usf.sas.pal.muser.ui.common.Presenter<GenreMenuContract.View>(), GenreMenuContract.Presenter {
    override fun createPlaylist(genre: edu.usf.sas.pal.muser.model.Genre) {
        getSongs(genre) { songs ->
            view?.presentCreatePlaylistDialog(songs)
        }
    }

    override fun addToPlaylist(playlist: edu.usf.sas.pal.muser.model.Playlist, genre: edu.usf.sas.pal.muser.model.Genre) {
        getSongs(genre) { songs ->
            playlistManager.addToPlaylist(playlist, songs) { numSongs ->
                view?.onSongsAddedToPlaylist(playlist, numSongs)
            }
        }
    }

    override fun addToQueue(genre: edu.usf.sas.pal.muser.model.Genre) {
        getSongs(genre) { songs ->
            mediaManager.addToQueue(songs) { numSongs ->
                view?.onSongsAddedToQueue(numSongs)
            }
        }
    }

    override fun play(genre: edu.usf.sas.pal.muser.model.Genre) {
        mediaManager.playAll(genre.getSongs(context)) {
            view?.onPlaybackFailed()
        }
    }

    override fun playNext(genre: edu.usf.sas.pal.muser.model.Genre) {
        getSongs(genre) { songs ->
            mediaManager.playNext(songs) { numSongs ->
                view?.onSongsAddedToQueue(numSongs)
            }
        }
    }

    private fun getSongs(genre: edu.usf.sas.pal.muser.model.Genre, onSuccess: (songs: List<edu.usf.sas.pal.muser.model.Song>) -> Unit) {
        addDisposable(
            genre.getSongs(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    onSuccess,
                    { error -> edu.usf.sas.pal.muser.utils.LogUtils.logException(AlbumMenuPresenter.TAG, "Failed to retrieve songs", error) }
                )
        )
    }

}