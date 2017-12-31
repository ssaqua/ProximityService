package ss.proximityservice.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import ss.proximityservice.ProximityApplication
import javax.inject.Singleton

@Singleton
@Component(modules = [AndroidInjectionModule::class])
interface AppComponent : AndroidInjector<ProximityApplication> {
    @Component.Builder interface Builder {
        @BindsInstance fun application(application: Application) : AppComponent.Builder
        fun build() : AppComponent
    }
}
