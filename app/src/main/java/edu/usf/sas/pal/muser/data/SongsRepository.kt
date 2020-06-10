
package edu.usf.sas.pal.muser.data

import android.content.Context
import android.provider.MediaStore
import android.util.Pair
import com.jakewharton.rxrelay2.BehaviorRelay
import edu.usf.sas.pal.muser.data.Repository.SongsRepository
import edu.usf.sas.pal.muser.utils.playlists.PlaylistManager
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import java.util.ArrayList
import java.util.Arrays
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class SongsRepository @Inject constructor(
    private val context: Context,
    private val blacklistRepository: Repository.BlacklistRepository,
    private val whitelistRepository: Repository.WhitelistRepository,
    private val settingsManager: edu.usf.sas.pal.muser.utils.SettingsManager
) : SongsRepository {

    private var songsSubscription: Disposable? = null
    private val songsRelay = BehaviorRelay.create<List<edu.usf.sas.pal.muser.model.Song>>()

    private var allSongsSubscription: Disposable? = null
    private val allSongsRelay = BehaviorRelay.create<List<edu.usf.sas.pal.muser.model.Song>>()

    override fun getAllSongs(): Observable<List<edu.usf.sas.pal.muser.model.Song>> {
        if (allSongsSubscription == null || allSongsSubscription?.isDisposed == true) {
            allSongsSubscription = edu.usf.sas.pal.muser.sql.sqlbrite.SqlBriteUtils.createObservableList<edu.usf.sas.pal.muser.model.Song>(context, { edu.usf.sas.pal.muser.model.Song(it) }, edu.usf.sas.pal.muser.model.Song.getQuery())
                .subscribe(
                    allSongsRelay,
                    Consumer { error -> edu.usf.sas.pal.muser.utils.LogUtils.logException(PlaylistsRepository.TAG, "Failed to get all songs", error) }
                )
        }

        return allSongsRelay
            .subscribeOn(Schedulers.io())
    }

    override fun getSongs(predicate: ((edu.usf.sas.pal.muser.model.Song) -> Boolean)?): Observable<List<edu.usf.sas.pal.muser.model.Song>> {
        if (songsSubscription == null || songsSubscription?.isDisposed == true) {
            songsSubscription = getAllSongs()
                .compose(getInclExclTransformer())
                .map { songs ->
                    songs
                        .filterNot { song -> song.isPodcast }
                        .toList()
                }
                .subscribe(songsRelay)
        }

        return songsRelay
            .map { songs -> predicate?.let { predicate -> songs.filter(predicate) } ?: songs }
            .subscribeOn(Schedulers.io())
    }

    override fun getSongs(album: edu.usf.sas.pal.muser.model.Album): Observable<List<edu.usf.sas.pal.muser.model.Song>> {
        return getSongs { song -> song.albumId == album.id }
    }

    override fun getSongs(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist): Observable<List<edu.usf.sas.pal.muser.model.Song>> {
        return getSongs { song ->
            albumArtist.albums
                .map { album -> album.id }
                .any { albumId -> albumId == song.albumId }
        }
    }

    override fun getSongs(playlist: edu.usf.sas.pal.muser.model.Playlist): Observable<List<edu.usf.sas.pal.muser.model.Song>> {
        return when (playlist.id) {
            PlaylistManager.PlaylistIds.RECENTLY_ADDED_PLAYLIST -> {
                val numWeeks = settingsManager.numWeeks * 3600 * 24 * 7
                return getSongs { song -> song.dateAdded > System.currentTimeMillis() / 1000 - numWeeks }
                    .map { songs ->
                        songs
                            .sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compare(a.albumArtistName, b.albumArtistName) })
                            .sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compare(a.albumArtistName, b.albumArtistName) })
                            .sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compareInt(b.year, a.year) })
                            .sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compareInt(a.track, b.track) })
                            .sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compareInt(a.discNumber, b.discNumber) })
                            .sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compare(a.albumName, b.albumName) })
                            .sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compareLong(b.dateAdded.toLong(), a.dateAdded.toLong()) })
                    }
            }

            PlaylistManager.PlaylistIds.PODCASTS_PLAYLIST -> {
                getAllSongs()
                    .compose(getInclExclTransformer())
                    .map { songs -> songs.filter { song -> song.isPodcast } }
                    .map { songs -> songs.sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compareLong(a.playlistSongPlayOrder, b.playlistSongPlayOrder) }) }
            }

            PlaylistManager.PlaylistIds.MOST_PLAYED_PLAYLIST -> {
                val query = edu.usf.sas.pal.muser.model.Query.Builder()
                    .uri(edu.usf.sas.pal.muser.sql.providers.PlayCountTable.URI)
                    .projection(arrayOf(edu.usf.sas.pal.muser.sql.providers.PlayCountTable.COLUMN_ID, edu.usf.sas.pal.muser.sql.providers.PlayCountTable.COLUMN_PLAY_COUNT))
                    .sort(edu.usf.sas.pal.muser.sql.providers.PlayCountTable.COLUMN_PLAY_COUNT + " DESC")
                    .build()

                edu.usf.sas.pal.muser.sql.sqlbrite.SqlBriteUtils.createObservableList(context, { cursor ->
                    Pair(
                        cursor.getLong(cursor.getColumnIndexOrThrow(edu.usf.sas.pal.muser.sql.providers.PlayCountTable.COLUMN_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(edu.usf.sas.pal.muser.sql.providers.PlayCountTable.COLUMN_PLAY_COUNT))
                    )
                }, query)
                    .flatMap { pairs ->
                        getSongs { song ->
                            pairs.firstOrNull { pair ->
                                song.playCount = pair.second
                                pair.first == song.id && pair.second >= 2
                            } != null
                        }.map { songs -> songs.sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compareInt(b.playCount, a.playCount) }) }
                    }
            }

            PlaylistManager.PlaylistIds.RECENTLY_PLAYED_PLAYLIST -> {
                val query = edu.usf.sas.pal.muser.model.Query.Builder()
                    .uri(edu.usf.sas.pal.muser.sql.providers.PlayCountTable.URI)
                    .projection(arrayOf(edu.usf.sas.pal.muser.sql.providers.PlayCountTable.COLUMN_ID, edu.usf.sas.pal.muser.sql.providers.PlayCountTable.COLUMN_TIME_PLAYED))
                    .sort(edu.usf.sas.pal.muser.sql.providers.PlayCountTable.COLUMN_TIME_PLAYED + " DESC")
                    .build()

                edu.usf.sas.pal.muser.sql.sqlbrite.SqlBriteUtils.createObservableList(context, { cursor ->
                    Pair(
                        cursor.getLong(cursor.getColumnIndexOrThrow(edu.usf.sas.pal.muser.sql.providers.PlayCountTable.COLUMN_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(edu.usf.sas.pal.muser.sql.providers.PlayCountTable.COLUMN_TIME_PLAYED))
                    )
                }, query)
                    .flatMap { pairs ->
                        getSongs { song ->
                            pairs.filter { pair ->
                                song.lastPlayed = pair.second
                                pair.first == song.id
                            }.firstOrNull() != null
                        }.map { songs -> songs.sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compareLong(b.lastPlayed, a.lastPlayed) }) }
                    }
            }

            else -> {
                val query = edu.usf.sas.pal.muser.model.Song.getQuery()
                query.uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.id)
                val projection = ArrayList(Arrays.asList(*edu.usf.sas.pal.muser.model.Song.getProjection()))
                projection.add(MediaStore.Audio.Playlists.Members._ID)
                projection.add(MediaStore.Audio.Playlists.Members.AUDIO_ID)
                projection.add(MediaStore.Audio.Playlists.Members.PLAY_ORDER)
                query.projection = projection.toTypedArray()

                edu.usf.sas.pal.muser.sql.sqlbrite.SqlBriteUtils.createObservableList<edu.usf.sas.pal.muser.model.Song>(context, { edu.usf.sas.pal.muser.model.Playlist.createSongFromPlaylistCursor(it) }, query)
                    .map { songs -> songs.sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compareLong(a.playlistSongPlayOrder, b.playlistSongPlayOrder) }) }
            }
        }
    }

    override fun getSongs(genre: edu.usf.sas.pal.muser.model.Genre): Observable<List<edu.usf.sas.pal.muser.model.Song>> {
        return getSongs()
            .map { songs ->
                songs.sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compareInt(b.year, a.year) })
                    .sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compareInt(a.track, b.track) })
                    .sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compareInt(a.discNumber, b.discNumber) })
                    .sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compare(a.albumName, b.albumName) })
                    .sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compare(a.albumArtistName, b.albumArtistName) })
            }
    }

    private fun getInclExclTransformer(): ObservableTransformer<List<edu.usf.sas.pal.muser.model.Song>, List<edu.usf.sas.pal.muser.model.Song>> {
        return ObservableTransformer { upstream ->
            Observable.combineLatest<List<edu.usf.sas.pal.muser.model.Song>, List<edu.usf.sas.pal.muser.model.InclExclItem>, List<edu.usf.sas.pal.muser.model.InclExclItem>, List<edu.usf.sas.pal.muser.model.Song>>(
                upstream,
                whitelistRepository.getWhitelistItems(this),
                blacklistRepository.getBlacklistItems(this),
                Function3 { songs: List<edu.usf.sas.pal.muser.model.Song>, inclItems: List<edu.usf.sas.pal.muser.model.InclExclItem>, exclItems: List<edu.usf.sas.pal.muser.model.InclExclItem> ->
                    var result = songs

                    // Filter out excluded paths
                    if (!exclItems.isEmpty()) {
                        result = songs
                            .filterNot { song -> exclItems.any { exclItem -> edu.usf.sas.pal.muser.utils.StringUtils.containsIgnoreCase(song.path, exclItem.path) } }
                            .toList()
                    }

                    // Filter out non-included paths
                    if (!inclItems.isEmpty()) {
                        result = result
                            .filter { song -> inclItems.any { inclItem -> edu.usf.sas.pal.muser.utils.StringUtils.containsIgnoreCase(song.path, inclItem.path) } }
                            .toList()
                    }

                    result
                })
        }
    }

    companion object {
        const val TAG = "SongsRepository"
    }
}