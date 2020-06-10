package edu.usf.sas.pal.muser.data

import com.jakewharton.rxrelay2.BehaviorRelay
import edu.usf.sas.pal.muser.data.Repository.AlbumsRepository
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumsRepository @Inject constructor(private val songsRepository: Repository.SongsRepository) : AlbumsRepository {

    private var albumsSubscription: Disposable? = null
    private val albumsRelay = BehaviorRelay.create<List<edu.usf.sas.pal.muser.model.Album>>()

    override fun getAlbums(): Observable<List<edu.usf.sas.pal.muser.model.Album>> {
        if (albumsSubscription == null || albumsSubscription?.isDisposed == true) {
            albumsSubscription = songsRepository.getSongs()
                .flatMap { songs -> Observable.just(edu.usf.sas.pal.muser.utils.Operators.songsToAlbums(songs)) }
                .subscribe(
                    albumsRelay,
                    Consumer { error -> edu.usf.sas.pal.muser.utils.LogUtils.logException(PlaylistsRepository.TAG, "Failed to get albums", error) }
                )
        }
        return albumsRelay.subscribeOn(Schedulers.io())
    }

    companion object {
        const val TAG = "AlbumsRepository"
    }
}