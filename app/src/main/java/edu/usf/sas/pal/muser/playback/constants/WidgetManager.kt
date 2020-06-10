package edu.usf.sas.pal.muser.playback.constants

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.preference.PreferenceManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetManager @Inject constructor(
        private val widgetProviderMedium: edu.usf.sas.pal.muser.ui.widgets.WidgetProviderMedium,
        private val widgetProviderSmall: edu.usf.sas.pal.muser.ui.widgets.WidgetProviderSmall,
        private val widgetProviderLarge: edu.usf.sas.pal.muser.ui.widgets.WidgetProviderLarge,
        private val widgetProviderExtraLarge: edu.usf.sas.pal.muser.ui.widgets.WidgetProviderExtraLarge
) {

    fun processCommand(musicService: edu.usf.sas.pal.muser.playback.MusicService, intent: Intent, command: String) {

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(musicService)

        when (command) {
            edu.usf.sas.pal.muser.ui.widgets.WidgetProviderSmall.CMDAPPWIDGETUPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                widgetProviderSmall.update(musicService, sharedPreferences, appWidgetIds, true)
            }
            edu.usf.sas.pal.muser.ui.widgets.WidgetProviderMedium.CMDAPPWIDGETUPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                widgetProviderMedium.update(musicService, sharedPreferences, appWidgetIds, true)
            }
            edu.usf.sas.pal.muser.ui.widgets.WidgetProviderLarge.CMDAPPWIDGETUPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                widgetProviderLarge.update(musicService, sharedPreferences, appWidgetIds, true)
            }
            edu.usf.sas.pal.muser.ui.widgets.WidgetProviderExtraLarge.CMDAPPWIDGETUPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                widgetProviderExtraLarge.update(musicService, sharedPreferences, appWidgetIds, true)
            }
        }
    }

    fun notifyChange(musicService: edu.usf.sas.pal.muser.playback.MusicService, what: String) {
        widgetProviderLarge.notifyChange(musicService, what)
        widgetProviderMedium.notifyChange(musicService, what)
        widgetProviderSmall.notifyChange(musicService, what)
        widgetProviderExtraLarge.notifyChange(musicService, what)
    }
}