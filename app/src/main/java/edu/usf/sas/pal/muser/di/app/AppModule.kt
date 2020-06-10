package edu.usf.sas.pal.muser.di.app

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import edu.usf.sas.pal.muser.di.app.activity.ActivityScope
import edu.usf.sas.pal.muser.ui.screens.shortcut.ShortcutTrampolineActivity
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Module(includes = [AppModuleBinds::class])
class AppModule {

    @Provides
    fun provideContext(application: edu.usf.sas.pal.muser.ShuttleApplication): Context = application.applicationContext

    @Provides
    @Singleton
    fun provideSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}

@Module(includes = [AndroidSupportInjectionModule::class])
abstract class AppModuleBinds {

    @ActivityScope
    @ContributesAndroidInjector(modules = [edu.usf.sas.pal.muser.ui.screens.main.MainActivityModule::class])
    abstract fun mainActivityInjector(): edu.usf.sas.pal.muser.ui.screens.main.MainActivity

    @ContributesAndroidInjector
    abstract fun musicServiceInjector(): edu.usf.sas.pal.muser.playback.MusicService

    @ContributesAndroidInjector
    abstract fun artworkServiceInjector(): edu.usf.sas.pal.muser.services.ArtworkDownloadService

    @ContributesAndroidInjector
    abstract fun mediaButtonIntentReceiverInjector(): edu.usf.sas.pal.muser.utils.MediaButtonIntentReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun shortcutTrampolineActivityInjector(): ShortcutTrampolineActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [edu.usf.sas.pal.muser.ui.widgets.WidgetConfigureActivitySmallModule::class])
    abstract fun widgetConfigureActivitySmallInjector(): edu.usf.sas.pal.muser.ui.widgets.WidgetConfigureActivitySmall

    @ActivityScope
    @ContributesAndroidInjector(modules = [edu.usf.sas.pal.muser.ui.widgets.WidgetConfigureActivityMediumModule::class])
    abstract fun widgetConfigureActivityMediumInjector(): edu.usf.sas.pal.muser.ui.widgets.WidgetConfigureActivityMedium

    @ActivityScope
    @ContributesAndroidInjector(modules = [edu.usf.sas.pal.muser.ui.widgets.WidgetConfigureActivityLargeModule::class])
    abstract fun widgetConfigureActivityLargeInjector(): edu.usf.sas.pal.muser.ui.widgets.WidgetConfigureActivityLarge

    @ActivityScope
    @ContributesAndroidInjector(modules = [edu.usf.sas.pal.muser.ui.widgets.WidgetConfigureActivityExtraLargeModule::class])
    abstract fun widgetConfigureActivityExtraLargeInjector(): edu.usf.sas.pal.muser.ui.widgets.WidgetConfigureActivityExtraLarge
}