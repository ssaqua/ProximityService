package ss.proximityservice

import android.content.Context
import dagger.Module
import dagger.Provides

@Module
class ApplicationModule(val context: Context) {
    @Provides fun provideContext(): Context = context
}
