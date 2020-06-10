package edu.usf.sas.pal.muser.utils.menu.queue

import edu.usf.sas.pal.muser.ui.screens.queue.QueueItem
import edu.usf.sas.pal.muser.utils.menu.song.SongsMenuCallbacks
import io.reactivex.Single

interface QueueMenuCallbacks : SongsMenuCallbacks {

    fun moveToNext(queueItem: QueueItem)

    fun removeQueueItems(queueItems: List<QueueItem>)
}


fun QueueMenuCallbacks.removeQueueItem(queueItem: QueueItem) {
    removeQueueItems(listOf(queueItem))
}


fun QueueMenuCallbacks.removeQueueItems(queueItems: Single<List<QueueItem>>) {
    transform(queueItems) { queueItems -> removeQueueItems(queueItems) }
}