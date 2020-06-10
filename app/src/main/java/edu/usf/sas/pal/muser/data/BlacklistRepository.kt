package edu.usf.sas.pal.muser.data

import android.content.ContentValues
import com.jakewharton.rxrelay2.BehaviorRelay
import edu.usf.sas.pal.muser.model.InclExclItem
import com.squareup.sqlbrite2.BriteDatabase
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

open class InclExclRepository @Inject constructor(
    private val inclExclDatabase: BriteDatabase,
    @edu.usf.sas.pal.muser.model.InclExclItem.Type private val type: Int
) : Repository.InclExclRepository {

    override fun add(inclExclItem: edu.usf.sas.pal.muser.model.InclExclItem) {
        val values = ContentValues(2)
        values.put(edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.COLUMN_PATH, inclExclItem.path)
        values.put(edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.COLUMN_TYPE, inclExclItem.type)
        inclExclDatabase.insert(edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.TABLE_NAME, values)
    }

    override fun addAll(inclExclItems: List<edu.usf.sas.pal.muser.model.InclExclItem>) {
        val transaction = inclExclDatabase.newTransaction()
        try {
            inclExclItems.map { inclExclItem ->
                val contentValues = ContentValues(2)
                contentValues.put(edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.COLUMN_PATH, inclExclItem.path)
                contentValues.put(edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.COLUMN_TYPE, inclExclItem.type)
                contentValues
            }.forEach { contentValues -> inclExclDatabase.insert(edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.TABLE_NAME, contentValues) }
            transaction.markSuccessful()
        } finally {
            transaction.end()
        }
    }

    override fun delete(inclExclItem: edu.usf.sas.pal.muser.model.InclExclItem) {
        inclExclDatabase.delete(
            edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.TABLE_NAME,
            edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.COLUMN_PATH + " = '" + inclExclItem.path.replace("'".toRegex(), "\''") + "'" +
                " AND " + edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.COLUMN_TYPE + " = " + inclExclItem.type
        )
    }

    override fun addSong(song: edu.usf.sas.pal.muser.model.Song) {
        add(edu.usf.sas.pal.muser.model.InclExclItem(song.path, type))
    }

    override fun addAllSongs(songs: List<edu.usf.sas.pal.muser.model.Song>) {
        addAll(songs.map { song -> edu.usf.sas.pal.muser.model.InclExclItem(song.path, type) }.toList())
    }

    override fun deleteAll() {
        inclExclDatabase.delete(edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.TABLE_NAME, edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.COLUMN_TYPE + " = " + type)
    }

}

class WhitelistRepository @Inject constructor(private val inclExclDatabase: BriteDatabase) : InclExclRepository(inclExclDatabase, edu.usf.sas.pal.muser.model.InclExclItem.Type.INCLUDE), Repository.WhitelistRepository {

    private var inclSubscription: Disposable? = null
    private val inclRelay = BehaviorRelay.create<List<edu.usf.sas.pal.muser.model.InclExclItem>>()

    private fun getIncludeItems(): Observable<List<edu.usf.sas.pal.muser.model.InclExclItem>> {
        return inclExclDatabase.createQuery(
            edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.TABLE_NAME,
            "SELECT * FROM " + edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.TABLE_NAME + " WHERE " + edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.COLUMN_TYPE + " = " + edu.usf.sas.pal.muser.model.InclExclItem.Type.INCLUDE
        ).mapToList { edu.usf.sas.pal.muser.model.InclExclItem(it) }
    }

    /**
     * @return a **continuous** stream of type [InclExclItem.Type.INCLUDE] , backed by a behavior relay for caching query results.
     */
    override fun getWhitelistItems(songsRepository: Repository.SongsRepository): Observable<List<edu.usf.sas.pal.muser.model.InclExclItem>> {
        if (inclSubscription == null || inclSubscription?.isDisposed == true) {
            inclSubscription = getIncludeItems().subscribe(inclRelay)
        }
        return inclRelay.subscribeOn(Schedulers.io())
    }
}

class BlacklistRepository @Inject constructor(private val inclExclDatabase: BriteDatabase) : InclExclRepository(inclExclDatabase, edu.usf.sas.pal.muser.model.InclExclItem.Type.EXCLUDE), Repository.BlacklistRepository {

    private var exclSubscription: Disposable? = null
    private val exclRelay = BehaviorRelay.create<List<edu.usf.sas.pal.muser.model.InclExclItem>>()

    private fun getExcludeItems(): Observable<List<edu.usf.sas.pal.muser.model.InclExclItem>> {
        return inclExclDatabase.createQuery(
            edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.TABLE_NAME,
            "SELECT * FROM " + edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.TABLE_NAME + " WHERE " + edu.usf.sas.pal.muser.sql.databases.BlacklistWhitelistDbOpenHelper.COLUMN_TYPE + " = " + edu.usf.sas.pal.muser.model.InclExclItem.Type.EXCLUDE
        )
            .mapToList { edu.usf.sas.pal.muser.model.InclExclItem(it) }
    }

    /**
     * @return a **continuous** stream of type [InclExclItem.Type.EXCLUDE], backed by a behavior relay for caching query results.
     */
    override fun getBlacklistItems(songsRepository: Repository.SongsRepository): Observable<List<edu.usf.sas.pal.muser.model.InclExclItem>> {
        if (exclSubscription == null || exclSubscription?.isDisposed == true) {
            exclSubscription = getExcludeItems()
                .subscribe(exclRelay)
        }
        return exclRelay.subscribeOn(Schedulers.io())
    }
}