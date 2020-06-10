package edu.usf.sas.pal.muser.ui.settings

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import com.afollestad.materialdialogs.MaterialDialog
import com.annimon.stream.Stream
import edu.usf.sas.pal.muser.R
import edu.usf.sas.pal.muser.ui.dialog.UpgradeDialog
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class TabChooserDialog : DialogFragment() {

    @Inject lateinit var analyticsManager: edu.usf.sas.pal.muser.utils.AnalyticsManager

    @Inject lateinit var settingsManager: edu.usf.sas.pal.muser.utils.SettingsManager

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val adapter = edu.usf.sas.simplecityapps.recycler_adapter.adapter.ViewModelAdapter()

        val itemTouchHelper = ItemTouchHelper(
                edu.usf.sas.pal.muser.ui.views.recyclerview.ItemTouchHelperCallback(
                        edu.usf.sas.pal.muser.ui.views.recyclerview.ItemTouchHelperCallback.OnItemMoveListener { fromPosition, toPosition -> adapter.moveItem(fromPosition, toPosition) },
                        edu.usf.sas.pal.muser.ui.views.recyclerview.ItemTouchHelperCallback.OnDropListener { _, _ -> },
                        edu.usf.sas.pal.muser.ui.views.recyclerview.ItemTouchHelperCallback.OnClearListener { },
                        edu.usf.sas.pal.muser.ui.views.recyclerview.ItemTouchHelperCallback.OnSwipeListener { }
                ))

        val listener = object : edu.usf.sas.pal.muser.ui.modelviews.TabViewModel.Listener {
            override fun onStartDrag(holder: edu.usf.sas.pal.muser.ui.modelviews.TabViewModel.ViewHolder) {
                itemTouchHelper.startDrag(holder)
            }

            override fun onFolderChecked(tabViewModel: edu.usf.sas.pal.muser.ui.modelviews.TabViewModel, viewHolder: edu.usf.sas.pal.muser.ui.modelviews.TabViewModel.ViewHolder) {
                if (!edu.usf.sas.pal.muser.utils.ShuttleUtils.isUpgraded(context!!.applicationContext as edu.usf.sas.pal.muser.ShuttleApplication, settingsManager)) {
                    viewHolder.checkBox.isChecked = false
                    tabViewModel.categoryItem.isChecked = false
                    UpgradeDialog().show(fragmentManager!!)
                }
            }
        }

        val items = edu.usf.sas.pal.muser.model.CategoryItem.getCategoryItems(sharedPreferences)
            .map { categoryItem ->
                val tabViewModel = edu.usf.sas.pal.muser.ui.modelviews.TabViewModel(categoryItem, settingsManager)
                tabViewModel.setListener(listener)
                tabViewModel
            }

        analyticsManager.dropBreadcrumb(TAG, "setItems()")
        adapter.setItems(items)

        val recyclerView = RecyclerView(context!!)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        itemTouchHelper.attachToRecyclerView(recyclerView)

        return MaterialDialog.Builder(context!!)
            .title(R.string.pref_title_choose_tabs)
            .customView(recyclerView, false)
            .positiveText(R.string.button_done)
            .onPositive { dialog, which ->
                val editor = sharedPreferences.edit()
                Stream.of<edu.usf.sas.simplecityapps.recycler_adapter.model.ViewModel<*>>(adapter.items)
                    .indexed()
                    .forEach { viewModelIntPair ->
                        (viewModelIntPair.second as edu.usf.sas.pal.muser.ui.modelviews.TabViewModel).categoryItem.sortOrder = viewModelIntPair.first
                        (viewModelIntPair.second as edu.usf.sas.pal.muser.ui.modelviews.TabViewModel).categoryItem.savePrefs(editor)
                    }
                LocalBroadcastManager.getInstance(context!!).sendBroadcast(Intent(edu.usf.sas.pal.muser.ui.screens.main.LibraryController.EVENT_TABS_CHANGED))
            }
            .negativeText(R.string.close)
            .build()
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {

        private const val TAG = "TabChooserDialog"
    }
}