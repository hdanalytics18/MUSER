package edu.usf.sas.pal.muser.ui.screens.search;

import android.support.v4.app.Fragment;

import edu.usf.sas.pal.muser.di.app.activity.fragment.FragmentModule;
import edu.usf.sas.pal.muser.di.app.activity.fragment.FragmentScope;

import javax.inject.Named;

import dagger.Binds;
import dagger.Module;

@Module(includes = FragmentModule.class)
public abstract class SearchFragmentModule {

    @Binds
    @Named(FragmentModule.FRAGMENT)
    @FragmentScope
    abstract Fragment fragment(SearchFragment searchFragment);
}
