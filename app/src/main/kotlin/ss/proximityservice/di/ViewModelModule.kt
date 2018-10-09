package ss.proximityservice.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import ss.proximityservice.ViewModelFactory
import ss.proximityservice.settings.SettingsViewModel

@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModuleKey(SettingsViewModel::class)
    abstract fun bindSettingsViewModel(settingsViewModel: SettingsViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}
