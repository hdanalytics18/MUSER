package edu.usf.sas.pal.muser.di.app

import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        AppModule::class,
        AppAssistedModule::class,
        RepositoryModule::class
    ]
)
interface AppComponent : AndroidInjector<edu.usf.sas.pal.muser.ShuttleApplication> {
    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<edu.usf.sas.pal.muser.ShuttleApplication>()
}