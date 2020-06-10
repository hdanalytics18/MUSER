package edu.usf.sas.pal.muser.data

import edu.usf.sas.pal.muser.model.Album
import edu.usf.sas.pal.muser.model.AlbumArtist
import edu.usf.sas.pal.muser.model.Genre
import edu.usf.sas.pal.muser.model.Playlist
import edu.usf.sas.pal.muser.model.Song
import io.reactivex.Observable

interface Repository {

    interface SongsRepository {

        /**
         * Returns a continuous List of all [Song]s, no filtering is applied.
         */
        fun getAllSongs(): Observable<List<edu.usf.sas.pal.muser.model.Song>>

        /**
         * Returns a continuous List of [Song]s, excluding those which are blacklisted, podcasts, or not-whitelisted.
         */
        fun getSongs(predicate: ((edu.usf.sas.pal.muser.model.Song) -> Boolean)? = null): Observable<List<edu.usf.sas.pal.muser.model.Song>>

        /**
         * Returns a continuous List of [Song]s belonging to the given [Playlist], excluding those which are blacklisted, podcasts, or not-whitelisted.
         */
        fun getSongs(playlist: edu.usf.sas.pal.muser.model.Playlist): Observable<List<edu.usf.sas.pal.muser.model.Song>>

        /**
         * Returns a continuous List of [Song]s belonging to the given [Album], excluding those which are blacklisted, podcasts, or not-whitelisted.
         */
        fun getSongs(album: edu.usf.sas.pal.muser.model.Album): Observable<List<edu.usf.sas.pal.muser.model.Song>>

        /**
         * Returns a continuous List of [Song]s belonging to the given [AlbumArtist], excluding those which are blacklisted, podcasts, or not-whitelisted.
         */
        fun getSongs(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist): Observable<List<edu.usf.sas.pal.muser.model.Song>>

        /**
         * Returns a continuous List of [Song]s belonging to the given [Genre], excluding those which are blacklisted, podcasts, or not-whitelisted.
         */
        fun getSongs(genre: edu.usf.sas.pal.muser.model.Genre): Observable<List<edu.usf.sas.pal.muser.model.Song>>
    }

    interface AlbumsRepository {

        /**
         * Returns a continuous List of [Album]s
         */
        fun getAlbums(): Observable<List<edu.usf.sas.pal.muser.model.Album>>
    }

    interface AlbumArtistsRepository {

        /**
         * Returns a continuous list of [AlbumArtist]s
         */
        fun getAlbumArtists(): Observable<List<edu.usf.sas.pal.muser.model.AlbumArtist>>
    }

    interface GenresRepository {

        /**
         * Returns a continuous List of [Genre]s
         */
        fun getGenres(): Observable<List<edu.usf.sas.pal.muser.model.Genre>>
    }

    interface PlaylistsRepository {

        /**
         * Returns a continuous List of [Playlist]s
         */
        fun getPlaylists(): Observable<List<edu.usf.sas.pal.muser.model.Playlist>>

        /**
         * Returns a continuous List of [Playlist]s, including user-created playlists. Empty playlists are no returned.
         */
        fun getAllPlaylists(songsRepository: SongsRepository): Observable<MutableList<edu.usf.sas.pal.muser.model.Playlist>>

        fun deletePlaylist(playlist: edu.usf.sas.pal.muser.model.Playlist)


        fun getPodcastPlaylist(): edu.usf.sas.pal.muser.model.Playlist

        fun getRecentlyAddedPlaylist(): edu.usf.sas.pal.muser.model.Playlist

        fun getMostPlayedPlaylist(): edu.usf.sas.pal.muser.model.Playlist

        fun getRecentlyPlayedPlaylist(): edu.usf.sas.pal.muser.model.Playlist
    }

    interface InclExclRepository {

        fun add(inclExclItem: edu.usf.sas.pal.muser.model.InclExclItem)

        fun addAll(inclExclItems: List<edu.usf.sas.pal.muser.model.InclExclItem>)

        fun addSong(song: edu.usf.sas.pal.muser.model.Song)

        fun addAllSongs(songs: List<edu.usf.sas.pal.muser.model.Song>)

        fun delete(inclExclItem: edu.usf.sas.pal.muser.model.InclExclItem)

        fun deleteAll()
    }

    interface BlacklistRepository : InclExclRepository {

        fun getBlacklistItems(songsRepository: Repository.SongsRepository): Observable<List<edu.usf.sas.pal.muser.model.InclExclItem>>
    }

    interface WhitelistRepository : InclExclRepository {

        fun getWhitelistItems(songsRepository: Repository.SongsRepository): Observable<List<edu.usf.sas.pal.muser.model.InclExclItem>>
    }
}