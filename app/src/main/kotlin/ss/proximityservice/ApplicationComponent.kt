package ss.proximityservice

import dagger.Component
import dagger.android.AndroidInjectionModule
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
        AndroidInjectionModule::class,
        ApplicationModule::class))
interface ApplicationComponent {
    fun inject(application: ProximityApplication)
}
