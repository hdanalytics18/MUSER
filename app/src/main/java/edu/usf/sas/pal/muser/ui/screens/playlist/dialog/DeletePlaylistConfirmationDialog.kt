package edu.usf.sas.pal.muser.ui.screens.playlist.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import com.afollestad.materialdialogs.MaterialDialog
import edu.usf.sas.pal.muser.R
import edu.usf.sas.pal.muser.data.Repository.PlaylistsRepository
import edu.usf.sas.pal.muser.di.app.activity.fragment.FragmentModule
import edu.usf.sas.pal.muser.di.app.activity.fragment.FragmentScope
import dagger.Binds
import dagger.Module
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject
import javax.inject.Named

class DeletePlaylistConfirmationDialog : DialogFragment() {

    private lateinit var playlist: edu.usf.sas.pal.muser.model.Playlist

    @Inject lateinit var playlistsRepository: PlaylistsRepository

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)

        playlist = arguments!!.getSerializable(ARG_PLAYLIST) as edu.usf.sas.pal.muser.model.Playlist
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context!!)
            .title(R.string.dialog_title_playlist_delete)
            .content(R.string.dialog_message_playlist_delete, playlist.name)
            .positiveText(R.string.dialog_button_delete)
            .onPositive { dialog, which -> playlistsRepository.deletePlaylist(playlist) }
            .negativeText(R.string.cancel)
            .build()
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {

        const val TAG = "DeletePlaylistConfirmationDialog"

        const val ARG_PLAYLIST = "playlist"

        fun newInstance(playlist: edu.usf.sas.pal.muser.model.Playlist): DeletePlaylistConfirmationDialog {
            val bundle = Bundle()
            bundle.putSerializable(ARG_PLAYLIST, playlist)
            val fragment = DeletePlaylistConfirmationDialog()
            fragment.arguments = bundle
            return fragment
        }
    }
}

@Module(includes = [FragmentModule::class])
abstract class DeletePlaylistConfirmationDialogFragmentModule {

    @Binds
    @Named(FragmentModule.FRAGMENT)
    @FragmentScope
    internal abstract fun fragment(deletePlaylistConfirmationDialog: DeletePlaylistConfirmationDialog): Fragment
}