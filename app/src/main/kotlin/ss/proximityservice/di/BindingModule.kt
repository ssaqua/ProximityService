package ss.proximityservice.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import ss.proximityservice.ProximityService
import ss.proximityservice.settings.SettingsActivity

@Module
abstract class BindingModule {
    @ContributesAndroidInjector
    abstract fun settingsActivity(): SettingsActivity

    @ContributesAndroidInjector
    abstract fun proximityService(): ProximityService
}
