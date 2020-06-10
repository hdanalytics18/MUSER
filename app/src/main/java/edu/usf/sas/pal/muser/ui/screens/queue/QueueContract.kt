package edu.usf.sas.pal.muser.ui.screens.queue

import android.content.Context
import android.view.MenuItem
import edu.usf.sas.pal.muser.ui.screens.queue.menu.QueueMenuContract

interface QueueContract {

    interface View : QueueMenuContract.View {

        fun setData(queueItems: List<QueueItem>, position: Int)

        fun updateQueuePosition(queuePosition: Int)

        fun showToast(message: String, duration: Int)

        fun showTaggerDialog(taggerDialog: edu.usf.sas.pal.muser.ui.screens.tagger.TaggerDialog)

        fun showDeleteDialog(deleteDialog: edu.usf.sas.pal.muser.ui.dialog.DeleteDialog)

        fun onRemovedFromQueue(queueItem: QueueItem)

        fun onRemovedFromQueue(queueItems: List<QueueItem>)

        fun showUpgradeDialog()

        fun setQueueSwipeLocked(locked: Boolean)

        fun showCreatePlaylistDialog(songs: List<edu.usf.sas.pal.muser.model.Song>)
    }

    interface Presenter {

        fun saveQueue(context: Context)

        fun saveQueue(context: Context, item: MenuItem)

        fun clearQueue()

        fun moveQueueItem(from: Int, to: Int)

        fun loadData()

        fun play(queueItem: QueueItem)

        fun setQueueSwipeLocked(locked: Boolean)
    }
}