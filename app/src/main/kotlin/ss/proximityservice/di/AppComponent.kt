package ss.proximityservice.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import ss.proximityservice.ProximityApplication
import ss.proximityservice.data.AppStorageModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AndroidInjectionModule::class,
    AndroidSupportInjectionModule::class,
    AppModule::class,
    AppStorageModule::class,
    BindingModule::class])
interface AppComponent : AndroidInjector<ProximityApplication> {
    @Component.Builder interface Builder {
        @BindsInstance
        fun application(application: Application) : AppComponent.Builder
        fun build() : AppComponent
    }
}
