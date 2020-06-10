package edu.usf.sas.pal.muser.ui.widgets;

import android.support.v7.app.AppCompatActivity;
import edu.usf.sas.pal.muser.billing.BillingManager;
import edu.usf.sas.pal.muser.di.app.activity.ActivityModule;
import edu.usf.sas.pal.muser.di.app.activity.ActivityScope;
import edu.usf.sas.pal.muser.di.app.activity.fragment.FragmentScope;
import edu.usf.sas.pal.muser.ui.screens.widgets.WidgetFragment;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module(includes = ActivityModule.class)
public abstract class WidgetConfigureActivityLargeModule {

    @Binds
    @ActivityScope
    abstract AppCompatActivity appCompatActivity(WidgetConfigureActivityLarge activity);

    @Provides
    static BillingManager.BillingUpdatesListener provideBillingUpdatesListener(WidgetConfigureActivityLarge activity) {
        return activity;
    }

    @FragmentScope
    @ContributesAndroidInjector(modules = WidgetFragmentModule.class)
    abstract WidgetFragment widgetFragmentInjector();
}