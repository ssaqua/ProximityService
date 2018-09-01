package ss.proximityservice.di

import android.app.Application
import dagger.Module
import dagger.Provides
import ss.proximityservice.data.AppStorage
import ss.proximityservice.data.SharedPreferencesAppStorage
import javax.inject.Singleton

@Module
class AppModule {

    @Singleton
    @Provides
    fun provideAppStorage(context: Application): AppStorage = SharedPreferencesAppStorage(context)
}
