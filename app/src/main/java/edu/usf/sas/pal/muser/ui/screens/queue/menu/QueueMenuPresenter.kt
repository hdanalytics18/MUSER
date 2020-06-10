package edu.usf.sas.pal.muser.ui.screens.queue.menu

import android.content.Context
import edu.usf.sas.pal.muser.data.Repository.AlbumArtistsRepository
import edu.usf.sas.pal.muser.data.Repository.AlbumsRepository
import edu.usf.sas.pal.muser.data.Repository.BlacklistRepository
import edu.usf.sas.pal.muser.ui.screens.queue.QueueItem
import edu.usf.sas.pal.muser.ui.screens.songs.menu.SongMenuPresenter
import edu.usf.sas.pal.muser.utils.RingtoneManager
import edu.usf.sas.pal.muser.utils.playlists.PlaylistManager
import javax.inject.Inject

class QueueMenuPresenter @Inject constructor(
        context: Context,
        private val mediaManager: edu.usf.sas.pal.muser.playback.MediaManager,
        playlistManager: PlaylistManager,
        blacklistRepository: BlacklistRepository,
        ringtoneManager: RingtoneManager,
        albumArtistsRepository: AlbumArtistsRepository,
        albumsRepository: AlbumsRepository,
        navigationEventRelay: edu.usf.sas.pal.muser.ui.screens.drawer.NavigationEventRelay
) : SongMenuPresenter(
    context,
    mediaManager,
    playlistManager,
    blacklistRepository,
    ringtoneManager,
    albumArtistsRepository,
    albumsRepository,
    navigationEventRelay
), QueueMenuContract.Presenter {

    override fun moveToNext(queueItem: QueueItem) {
        mediaManager.moveToNext(queueItem)
    }

    override fun removeQueueItems(queueItems: List<QueueItem>) {
        mediaManager.removeFromQueue(queueItems)
    }

    companion object {
        const val TAG = "QueueMenuPresenter"
    }
}