package edu.usf.sas.pal.muser.utils.extensions

import android.content.Context
import android.content.Intent
import android.support.v4.content.FileProvider
import edu.usf.sas.pal.muser.R
import java.io.File

const val TAG = "SongExtensions"

fun edu.usf.sas.pal.muser.model.Song.share(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_SEND).setType("audio/*")
        val uri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", File(path))
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_via)))
    } catch (e: IllegalArgumentException) {
        edu.usf.sas.pal.muser.utils.LogUtils.logException(TAG, "Failed to share track", e)
    }
}

fun edu.usf.sas.pal.muser.model.Song.delete(): Boolean {

    if (path == null) return false

    var success = false

    val file = File(path)
    if (file.exists()) {
        success = file.delete()
    }

    return success
}

