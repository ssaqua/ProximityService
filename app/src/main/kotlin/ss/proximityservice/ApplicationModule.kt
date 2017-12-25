package ss.proximityservice

import android.content.Context
import dagger.Module
import dagger.Provides

@Module
class ApplicationModule(private val context: Context) {
    @Provides fun provideContext(): Context = context
}
