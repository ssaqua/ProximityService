package ss.proximityservice.data

import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppStorageModule {
    @Singleton
    @Provides
    fun provideSharedPerferencesAppStorage(context: Context) : AppStorage {
        return SharedPreferencesAppStorage(context)
    }
}
