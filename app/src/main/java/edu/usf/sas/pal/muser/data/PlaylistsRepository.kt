package edu.usf.sas.pal.muser.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.jakewharton.rxrelay2.BehaviorRelay
import edu.usf.sas.pal.muser.R
import edu.usf.sas.pal.muser.data.Repository.SongsRepository
import edu.usf.sas.pal.muser.model.Playlist.Type
import edu.usf.sas.pal.muser.utils.playlists.PlaylistManager
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistsRepository @Inject constructor(
    private val context: Context
) : Repository.PlaylistsRepository {

    private var playlistsSubscription: Disposable? = null
    private val playlistsRelay = BehaviorRelay.create<List<edu.usf.sas.pal.muser.model.Playlist>>()

    override fun getPlaylists(): Observable<List<edu.usf.sas.pal.muser.model.Playlist>> {
        if (playlistsSubscription == null || playlistsSubscription?.isDisposed == true) {
            playlistsSubscription = edu.usf.sas.pal.muser.sql.sqlbrite.SqlBriteUtils.createObservableList(
                context,
                { cursor -> edu.usf.sas.pal.muser.model.Playlist(context, cursor) },
                edu.usf.sas.pal.muser.model.Playlist.getQuery()
            )
                .subscribe(
                    playlistsRelay,
                    Consumer { error -> edu.usf.sas.pal.muser.utils.LogUtils.logException(TAG, "Failed to get playlists", error) }
                )
        }
        return playlistsRelay.subscribeOn(Schedulers.io())
    }

    override fun getAllPlaylists(songsRepository: SongsRepository): Observable<MutableList<edu.usf.sas.pal.muser.model.Playlist>> {
        val defaultPlaylistsObservable = Observable.fromCallable<List<edu.usf.sas.pal.muser.model.Playlist>> {
            val playlists = mutableListOf<edu.usf.sas.pal.muser.model.Playlist>()

            // Todo: Hide Podcasts if there are no songs
            playlists.add(getPodcastPlaylist())
            playlists.add(getRecentlyAddedPlaylist())
            playlists.add(getMostPlayedPlaylist())

            playlists
        }.subscribeOn(Schedulers.io())

        val playlistsObservable = getPlaylists()

        return Observable.combineLatest<List<edu.usf.sas.pal.muser.model.Playlist>, List<edu.usf.sas.pal.muser.model.Playlist>, MutableList<edu.usf.sas.pal.muser.model.Playlist>>(
            defaultPlaylistsObservable, playlistsObservable, BiFunction { defaultPlaylists: List<edu.usf.sas.pal.muser.model.Playlist>, playlists1: List<edu.usf.sas.pal.muser.model.Playlist> ->
                val list = mutableListOf<edu.usf.sas.pal.muser.model.Playlist>()
                list.addAll(defaultPlaylists)
                list.addAll(playlists1)
                list
            })
            .concatMap { playlists ->
                Observable.fromIterable<edu.usf.sas.pal.muser.model.Playlist?>(playlists)
                    .concatMap<edu.usf.sas.pal.muser.model.Playlist> { playlist ->
                        songsRepository.getSongs(playlist)
                            .first(emptyList())
                            .flatMapObservable { songs ->
                                if (playlist.type != Type.USER_CREATED && playlist.type != Type.FAVORITES && songs.isEmpty()
                                ) {
                                    Observable.empty()
                                } else {
                                    Observable.just(playlist)
                                }
                            }
                    }
                    .toList()
                    .toObservable()
            }

    }

    override fun deletePlaylist(playlist: edu.usf.sas.pal.muser.model.Playlist) {
        if (!playlist.canDelete) {
            Log.e(TAG, "Playlist cannot be deleted")
            return
        }

        ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, playlist.id)?.let { uri ->
            context.contentResolver.delete(uri, null, null)
        }
    }

    override fun getPodcastPlaylist(): edu.usf.sas.pal.muser.model.Playlist {
        return edu.usf.sas.pal.muser.model.Playlist(
                Type.PODCAST,
                PlaylistManager.PlaylistIds.PODCASTS_PLAYLIST,
                context.getString(R.string.podcasts_title),
                false,
                false,
                false,
                false,
                false
        )
    }

    override fun getRecentlyAddedPlaylist(): edu.usf.sas.pal.muser.model.Playlist {
        return edu.usf.sas.pal.muser.model.Playlist(
                Type.RECENTLY_ADDED,
                PlaylistManager.PlaylistIds.RECENTLY_ADDED_PLAYLIST,
                context.getString(R.string.recentlyadded),
                false,
                false,
                false,
                false,
                false
        )
    }

    override fun getMostPlayedPlaylist(): edu.usf.sas.pal.muser.model.Playlist {
        return edu.usf.sas.pal.muser.model.Playlist(
                Type.MOST_PLAYED,
                PlaylistManager.PlaylistIds.MOST_PLAYED_PLAYLIST,
                context.getString(R.string.mostplayed),
                false,
                true,
                false,
                false,
                false
        )
    }

    override fun getRecentlyPlayedPlaylist(): edu.usf.sas.pal.muser.model.Playlist {
        return edu.usf.sas.pal.muser.model.Playlist(
                Type.RECENTLY_PLAYED,
                PlaylistManager.PlaylistIds.RECENTLY_PLAYED_PLAYLIST,
                context.getString(R.string.suggested_recent_title),
                false,
                false,
                false,
                false,
                false
        )
    }

    companion object {
        const val TAG = "PlaylistsRepository"
    }

}