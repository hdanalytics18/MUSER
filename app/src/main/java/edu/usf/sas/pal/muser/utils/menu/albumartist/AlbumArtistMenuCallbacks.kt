package edu.usf.sas.pal.muser.utils.menu.albumartist

import io.reactivex.Single

interface AlbumArtistMenuCallbacks {

    fun createArtistsPlaylist(albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>)

    fun addArtistsToPlaylist(playlist: edu.usf.sas.pal.muser.model.Playlist, albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>)

    fun addArtistsToQueue(albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>)

    fun playArtistsNext(albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>)

    fun play(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist)

    fun editTags(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist)

    fun albumArtistInfo(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist)

    fun editArtwork(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist)

    fun blacklistArtists(albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>)

    fun deleteArtists(albumArtists: List<edu.usf.sas.pal.muser.model.AlbumArtist>)

    fun goToArtist(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist)

    fun albumShuffle(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist)

    fun <T> transform(src: Single<List<T>>, dst: (List<T>) -> Unit)
}

fun AlbumArtistMenuCallbacks.createArtistsPlaylist(albumArtists: Single<List<edu.usf.sas.pal.muser.model.AlbumArtist>>) {
    transform(albumArtists) { albumArtists -> createArtistsPlaylist(albumArtists) }
}

fun AlbumArtistMenuCallbacks.addArtistsToPlaylist(playlist: edu.usf.sas.pal.muser.model.Playlist, albumArtists: Single<List<edu.usf.sas.pal.muser.model.AlbumArtist>>) {
    transform(albumArtists) { albumArtists -> addArtistsToPlaylist(playlist, albumArtists) }
}

fun AlbumArtistMenuCallbacks.playArtistsNext(albumArtists: Single<List<edu.usf.sas.pal.muser.model.AlbumArtist>>) {
    transform(albumArtists) { albumArtists -> playArtistsNext(albumArtists) }
}

fun AlbumArtistMenuCallbacks.addArtistsToQueue(albumArtists: Single<List<edu.usf.sas.pal.muser.model.AlbumArtist>>) {
    transform(albumArtists) { albumArtists -> addArtistsToQueue(albumArtists) }
}

fun AlbumArtistMenuCallbacks.deleteArtists(albumArtists: Single<List<edu.usf.sas.pal.muser.model.AlbumArtist>>) {
    transform(albumArtists) { albumArtists -> deleteArtists(albumArtists) }
}

fun AlbumArtistMenuCallbacks.playArtistsNext(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
    playArtistsNext(listOf(albumArtist))
}

fun AlbumArtistMenuCallbacks.createArtistsPlaylist(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
    createArtistsPlaylist(listOf(albumArtist))
}

fun AlbumArtistMenuCallbacks.addArtistsToPlaylist(playlist: edu.usf.sas.pal.muser.model.Playlist, albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
    addArtistsToPlaylist(playlist, listOf(albumArtist))
}

fun AlbumArtistMenuCallbacks.addArtistsToQueue(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
    addArtistsToQueue(listOf(albumArtist))
}

fun AlbumArtistMenuCallbacks.blacklistArtists(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
    blacklistArtists(listOf(albumArtist))
}

fun AlbumArtistMenuCallbacks.deleteArtists(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
    deleteArtists(listOf(albumArtist))
}

fun AlbumArtistMenuCallbacks.albumShuffle(albumArtist: edu.usf.sas.pal.muser.model.AlbumArtist) {
    albumShuffle(albumArtist)
}