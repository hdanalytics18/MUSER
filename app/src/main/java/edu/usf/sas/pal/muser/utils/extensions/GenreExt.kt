package edu.usf.sas.pal.muser.utils.extensions

import android.content.Context
import android.provider.MediaStore
import io.reactivex.Single
import java.util.Comparator

fun edu.usf.sas.pal.muser.model.Genre.getSongsObservable(context: Context): Single<List<edu.usf.sas.pal.muser.model.Song>> {
    val query = edu.usf.sas.pal.muser.model.Song.getQuery()
    query.uri = MediaStore.Audio.Genres.Members.getContentUri("external", id)

    return edu.usf.sas.pal.muser.sql.sqlbrite.SqlBriteUtils.createSingleList(context, { edu.usf.sas.pal.muser.model.Song(it) }, query)
}

fun edu.usf.sas.pal.muser.model.Genre.getSongs(context: Context): Single<List<edu.usf.sas.pal.muser.model.Song>> {
    return getSongsObservable(context)
        .map { songs ->
            songs
                .sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compareInt(b.year, a.year) })
                .sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compareInt(a.track, b.track) })
                .sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compareInt(a.discNumber, b.discNumber) })
                .sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compare(a.albumName, b.albumName) })
                .sortedWith(Comparator { a, b -> edu.usf.sas.pal.muser.utils.ComparisonUtils.compare(a.albumArtistName, b.albumArtistName) })
        }
}