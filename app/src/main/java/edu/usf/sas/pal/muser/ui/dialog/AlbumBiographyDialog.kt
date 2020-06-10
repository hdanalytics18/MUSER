package edu.usf.sas.pal.muser.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import edu.usf.sas.pal.muser.R
import edu.usf.sas.pal.muser.R.string
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AlbumBiographyDialog : DialogFragment() {

    private lateinit var album: edu.usf.sas.pal.muser.model.Album

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        album = arguments!!.getSerializable(ARG_ALBUM) as edu.usf.sas.pal.muser.model.Album
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        @SuppressLint("InflateParams")
        val customView = LayoutInflater.from(context).inflate(R.layout.dialog_biography, null, false)

        val progressBar = customView.findViewById<ProgressBar>(R.id.progress)
        val message = customView.findViewById<TextView>(R.id.message)

        edu.usf.sas.pal.muser.http.HttpClient.getInstance().lastFmService.getLastFmAlbumResult(album.albumArtistName, album.name).enqueue(object : Callback<edu.usf.sas.pal.muser.http.lastfm.LastFmAlbum> {
            override fun onResponse(call: Call<edu.usf.sas.pal.muser.http.lastfm.LastFmAlbum>, response: Response<edu.usf.sas.pal.muser.http.lastfm.LastFmAlbum>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    if (response.body() != null && response.body()!!.album != null && response.body()!!.album.wiki != null) {
                        val summary = response.body()!!.album.wiki.summary
                        if (edu.usf.sas.pal.muser.utils.ShuttleUtils.hasNougat()) {
                            message.text = Html.fromHtml(summary, Html.FROM_HTML_MODE_COMPACT)
                        } else {
                            message.text = Html.fromHtml(summary)
                        }
                    } else {
                        message.setText(string.no_album_info)
                    }
                }
            }

            override fun onFailure(call: Call<edu.usf.sas.pal.muser.http.lastfm.LastFmAlbum>, t: Throwable) {
                progressBar.visibility = View.GONE
                message.setText(string.no_album_info)
            }
        })

        val builder = MaterialDialog.Builder(context!!)
            .title(R.string.info)
            .customView(customView, false)
            .negativeText(R.string.close)

        return builder.build()
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {

        private const val TAG = "AlbumBiographyDialog"

        private const val ARG_ALBUM = "album"

        fun newInstance(album: edu.usf.sas.pal.muser.model.Album): AlbumBiographyDialog {
            val args = Bundle()
            args.putSerializable(ARG_ALBUM, album)
            val fragment = AlbumBiographyDialog()
            fragment.arguments = args
            return fragment
        }
    }
}