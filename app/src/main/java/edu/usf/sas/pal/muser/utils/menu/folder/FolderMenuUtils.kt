package edu.usf.sas.pal.muser.utils.menu.folder

import android.annotation.SuppressLint
import android.content.Context
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v7.widget.PopupMenu
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import edu.usf.sas.pal.muser.R
import edu.usf.sas.pal.muser.data.Repository.SongsRepository
import edu.usf.sas.pal.muser.playback.MediaManager.Defs
import edu.usf.sas.pal.muser.ui.screens.playlist.dialog.CreatePlaylistDialog
import edu.usf.sas.pal.muser.utils.playlists.PlaylistManager
import edu.usf.sas.pal.muser.utils.playlists.PlaylistMenuHelper
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.io.File

object FolderMenuUtils {

    private val TAG = "FolderMenuUtils"

    interface Callbacks {

        fun showToast(message: String)

        fun showToast(@StringRes messageResId: Int)

        fun onSongsAddedToQueue(numSongs: Int)

        fun onPlaybackFailed()

        fun shareSong(song: edu.usf.sas.pal.muser.model.Song)

        fun setRingtone(song: edu.usf.sas.pal.muser.model.Song)

        fun showSongInfo(song: edu.usf.sas.pal.muser.model.Song)

        fun onPlaylistItemsInserted()

        fun showTagEditor(song: edu.usf.sas.pal.muser.model.Song)

        fun onFileNameChanged(folderView: edu.usf.sas.pal.muser.ui.modelviews.FolderView)

        fun onFileDeleted(folderView: edu.usf.sas.pal.muser.ui.modelviews.FolderView)

        fun playNext(songsSingle: Single<List<edu.usf.sas.pal.muser.model.Song>>)

        fun whitelist(songsSingle: Single<List<edu.usf.sas.pal.muser.model.Song>>)

        fun blacklist(songsSingle: Single<List<edu.usf.sas.pal.muser.model.Song>>)

        fun whitelist(song: edu.usf.sas.pal.muser.model.Song)

        fun blacklist(song: edu.usf.sas.pal.muser.model.Song)
    }

    private fun getSongForFile(songsRepository: SongsRepository, fileObject: edu.usf.sas.pal.muser.model.FileObject): Single<edu.usf.sas.pal.muser.model.Song> {
        return edu.usf.sas.pal.muser.utils.FileHelper.getSong(songsRepository, File(fileObject.path))
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun getSongsForFolderObject(songsRepository: SongsRepository, folderObject: edu.usf.sas.pal.muser.model.FolderObject): Single<List<edu.usf.sas.pal.muser.model.Song>> {
        return edu.usf.sas.pal.muser.utils.FileHelper.getSongList(songsRepository, File(folderObject.path), true, false)
    }

    private fun scanFile(context: Context, fileObject: edu.usf.sas.pal.muser.model.FileObject, callbacks: Callbacks) {
        edu.usf.sas.pal.muser.utils.CustomMediaScanner.scanFile(context, fileObject.path, { callbacks.showToast(it) })
    }

    // Todo: Remove context requirement.
    private fun scanFolder(context: Context, folderObject: edu.usf.sas.pal.muser.model.FolderObject) {
        edu.usf.sas.pal.muser.utils.CustomMediaScanner.scanFile(context, folderObject)
    }

    // Todo: Remove context requirement.
    private fun renameFile(context: Context, folderView: edu.usf.sas.pal.muser.ui.modelviews.FolderView, fileObject: edu.usf.sas.pal.muser.model.BaseFileObject, callbacks: Callbacks) {

        @SuppressLint("InflateParams")
        val customView = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null)

        val editText = customView.findViewById<EditText>(R.id.editText)
        editText.setText(fileObject.name)

        val builder = MaterialDialog.Builder(context)
        if (fileObject.fileType == edu.usf.sas.pal.muser.interfaces.FileType.FILE) {
            builder.title(R.string.rename_file)
        } else {
            builder.title(R.string.rename_folder)
        }

        builder.customView(customView, false)
        builder.positiveText(R.string.save)
            .onPositive { materialDialog, dialogAction ->
                if (editText.text != null) {
                    if (edu.usf.sas.pal.muser.utils.FileHelper.renameFile(context, fileObject, editText.text.toString())) {
                        callbacks.onFileNameChanged(folderView)
                    } else {
                        callbacks.showToast(if (fileObject.fileType == edu.usf.sas.pal.muser.interfaces.FileType.FOLDER) R.string.rename_folder_failed else R.string.rename_file_failed)
                    }
                }
            }
        builder.negativeText(R.string.cancel)
            .show()
    }

    // Todo: Remove context requirement.
    private fun deleteFile(context: Context, folderView: edu.usf.sas.pal.muser.ui.modelviews.FolderView, fileObject: edu.usf.sas.pal.muser.model.BaseFileObject, callbacks: Callbacks) {
        val builder = MaterialDialog.Builder(context)
            .title(R.string.delete_item)
            .iconRes(R.drawable.ic_warning_24dp)
        if (fileObject.fileType == edu.usf.sas.pal.muser.interfaces.FileType.FILE) {
            builder.content(
                String.format(
                    context.resources.getString(
                        R.string.delete_file_confirmation_dialog
                    ), fileObject.name
                )
            )
        } else {
            builder.content(
                String.format(
                    context.resources.getString(
                        R.string.delete_folder_confirmation_dialog
                    ), fileObject.path
                )
            )
        }
        builder.positiveText(R.string.button_ok)
            .onPositive { materialDialog, dialogAction ->
                if (edu.usf.sas.pal.muser.utils.FileHelper.deleteFile(File(fileObject.path))) {
                    callbacks.onFileDeleted(folderView)
                    edu.usf.sas.pal.muser.utils.CustomMediaScanner.scanFiles(context, listOf(fileObject.path), null)
                } else {
                    Toast.makeText(
                        context,
                        if (fileObject.fileType == edu.usf.sas.pal.muser.interfaces.FileType.FOLDER) R.string.delete_folder_failed else R.string.delete_file_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        builder.negativeText(R.string.cancel)
            .show()
    }

    fun setupFolderMenu(menu: PopupMenu, fileObject: edu.usf.sas.pal.muser.model.BaseFileObject, playlistMenuHelper: PlaylistMenuHelper) {

        menu.inflate(R.menu.menu_file)

        // Add playlist menu
        val subMenu = menu.menu.findItem(R.id.addToPlaylist).subMenu
        playlistMenuHelper.createPlaylistMenu(subMenu)

        if (!fileObject.canReadWrite()) {
            menu.menu.findItem(R.id.rename).isVisible = false
        }

        when (fileObject.fileType) {
            edu.usf.sas.pal.muser.interfaces.FileType.FILE -> menu.menu.findItem(R.id.play).isVisible = false
            edu.usf.sas.pal.muser.interfaces.FileType.FOLDER -> {
                menu.menu.findItem(R.id.songInfo).isVisible = false
                menu.menu.findItem(R.id.ringtone).isVisible = false
                menu.menu.findItem(R.id.share).isVisible = false
                menu.menu.findItem(R.id.editTags).isVisible = false
            }
            edu.usf.sas.pal.muser.interfaces.FileType.PARENT -> {
            }
        }
    }

    fun getFolderMenuClickListener(
            fragment: Fragment,
            mediaManager: edu.usf.sas.pal.muser.playback.MediaManager,
            songsRepository: SongsRepository,
            folderView: edu.usf.sas.pal.muser.ui.modelviews.FolderView,
            playlistManager: PlaylistManager,
            callbacks: Callbacks
    ): PopupMenu.OnMenuItemClickListener? {
        when (folderView.baseFileObject.fileType) {
            edu.usf.sas.pal.muser.interfaces.FileType.FILE -> return getFileMenuClickListener(fragment, mediaManager, songsRepository, folderView, folderView.baseFileObject as edu.usf.sas.pal.muser.model.FileObject, playlistManager, callbacks)
            edu.usf.sas.pal.muser.interfaces.FileType.FOLDER -> return getFolderMenuClickListener(fragment, mediaManager, songsRepository, folderView, folderView.baseFileObject as edu.usf.sas.pal.muser.model.FolderObject, playlistManager, callbacks)
        }
        return null
    }

    private fun getFolderMenuClickListener(
            fragment: Fragment,
            mediaManager: edu.usf.sas.pal.muser.playback.MediaManager,
            songsRepository: SongsRepository,
            folderView: edu.usf.sas.pal.muser.ui.modelviews.FolderView,
            folderObject: edu.usf.sas.pal.muser.model.FolderObject,
            playlistManager: PlaylistManager,
            callbacks: Callbacks
    ): PopupMenu.OnMenuItemClickListener {

        return PopupMenu.OnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.play -> {
                    edu.usf.sas.pal.muser.utils.menu.MenuUtils.play(mediaManager, getSongsForFolderObject(songsRepository, folderObject)) { callbacks.onPlaybackFailed() }
                    return@OnMenuItemClickListener true
                }
                R.id.playNext -> {
                    callbacks.playNext(getSongsForFolderObject(songsRepository, folderObject))
                    return@OnMenuItemClickListener true
                }
                Defs.NEW_PLAYLIST -> {
                    edu.usf.sas.pal.muser.utils.menu.MenuUtils.newPlaylist(
                        fragment,
                        getSongsForFolderObject(songsRepository, folderObject)
                    )
                    return@OnMenuItemClickListener true
                }
                Defs.PLAYLIST_SELECTED -> {
                    getSongsForFolderObject(songsRepository, folderObject).subscribe { songs ->
                        edu.usf.sas.pal.muser.utils.menu.MenuUtils.addToPlaylist(
                            playlistManager,
                            menuItem.intent.getSerializableExtra(PlaylistManager.ARG_PLAYLIST) as edu.usf.sas.pal.muser.model.Playlist,
                            songs
                        ) { callbacks.onPlaylistItemsInserted() }
                    }
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    edu.usf.sas.pal.muser.utils.menu.MenuUtils.addToQueue(mediaManager, getSongsForFolderObject(songsRepository, folderObject)) { callbacks.onSongsAddedToQueue(it) }
                    return@OnMenuItemClickListener true
                }
                R.id.scan -> {
                    scanFolder(fragment.context!!, folderObject)
                    return@OnMenuItemClickListener true
                }
                R.id.whitelist -> {
                    callbacks.whitelist(getSongsForFolderObject(songsRepository, folderObject))
                    return@OnMenuItemClickListener true
                }
                R.id.blacklist -> {
                    callbacks.blacklist(getSongsForFolderObject(songsRepository, folderObject))
                    return@OnMenuItemClickListener true
                }
                R.id.rename -> {
                    renameFile(fragment.context!!, folderView, folderObject, callbacks)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    deleteFile(fragment.context!!, folderView, folderObject, callbacks)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }

    private fun getFileMenuClickListener(
            fragment: Fragment,
            mediaManager: edu.usf.sas.pal.muser.playback.MediaManager,
            songsRepository: SongsRepository,
            folderView: edu.usf.sas.pal.muser.ui.modelviews.FolderView,
            fileObject: edu.usf.sas.pal.muser.model.FileObject,
            playlistManager: PlaylistManager,
            callbacks: Callbacks
    ): PopupMenu.OnMenuItemClickListener {
        return PopupMenu.OnMenuItemClickListener { menuItem ->

            val errorHandler: (Throwable) -> Unit = { e -> edu.usf.sas.pal.muser.utils.LogUtils.logException(TAG, "getFileMenuClickListener threw error", e) }

            when (menuItem.itemId) {
                R.id.playNext -> {
                    getSongForFile(songsRepository, fileObject)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { song -> mediaManager.playNext(listOf(song)) { callbacks.onSongsAddedToQueue(it) } },
                            errorHandler
                        )
                    return@OnMenuItemClickListener true
                }
                Defs.NEW_PLAYLIST -> {
                    getSongForFile(songsRepository, fileObject).subscribe(
                        { song ->
                            CreatePlaylistDialog.newInstance(listOf(song)).show(fragment.childFragmentManager, "CreatePlaylistDialog")
                        },
                        errorHandler
                    )
                    return@OnMenuItemClickListener true
                }
                Defs.PLAYLIST_SELECTED -> {
                    getSongForFile(songsRepository, fileObject).subscribe({ song ->
                        edu.usf.sas.pal.muser.utils.menu.MenuUtils.addToPlaylist(
                            playlistManager,
                            menuItem.intent.getSerializableExtra(PlaylistManager.ARG_PLAYLIST) as edu.usf.sas.pal.muser.model.Playlist,
                            listOf(song)
                        ) { callbacks.onPlaylistItemsInserted() }
                    }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    getSongForFile(songsRepository, fileObject).subscribe({ song -> edu.usf.sas.pal.muser.utils.menu.MenuUtils.addToQueue(mediaManager, listOf(song), { callbacks.onSongsAddedToQueue(it) }) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.scan -> {
                    scanFile(fragment.context!!, fileObject, callbacks)
                    return@OnMenuItemClickListener true
                }
                R.id.editTags -> {
                    getSongForFile(songsRepository, fileObject).subscribe({ callbacks.showTagEditor(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.share -> {
                    getSongForFile(songsRepository, fileObject).subscribe({ callbacks.shareSong(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.ringtone -> {
                    getSongForFile(songsRepository, fileObject).subscribe({ callbacks.setRingtone(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.songInfo -> {
                    getSongForFile(songsRepository, fileObject).subscribe({ callbacks.showSongInfo(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.blacklist -> {
                    getSongForFile(songsRepository, fileObject).subscribe({ callbacks.blacklist(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.whitelist -> {
                    getSongForFile(songsRepository, fileObject).subscribe({ callbacks.whitelist(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.rename -> {
                    renameFile(fragment.context!!, folderView, fileObject, callbacks)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    deleteFile(fragment.context!!, folderView, fileObject, callbacks)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }
}
