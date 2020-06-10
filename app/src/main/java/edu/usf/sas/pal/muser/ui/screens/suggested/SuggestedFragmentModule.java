package edu.usf.sas.pal.muser.ui.screens.suggested;

import android.support.v4.app.Fragment;
import edu.usf.sas.pal.muser.di.app.activity.fragment.FragmentModule;
import edu.usf.sas.pal.muser.di.app.activity.fragment.FragmentScope;
import dagger.Binds;
import dagger.Module;
import javax.inject.Named;

@Module(includes = FragmentModule.class)
public abstract class SuggestedFragmentModule {

    @Binds
    @Named(FragmentModule.FRAGMENT)
    @FragmentScope
    abstract Fragment fragment(SuggestedFragment suggestedFragment);
}
