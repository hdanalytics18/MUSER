package edu.usf.sas.pal.muser.di.app.activity.fragment

import android.support.v4.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class FragmentModule {

    @Provides
    @FragmentScope
    fun provideRequestManager(@Named(FRAGMENT) fragment: Fragment): RequestManager {
        return Glide.with(fragment)
    }

    companion object {
        const val FRAGMENT = "FragmentModule.fragment"
    }
}