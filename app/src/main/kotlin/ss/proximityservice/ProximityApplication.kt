package ss.proximityservice

import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import ss.proximityservice.di.DaggerAppComponent

class ProximityApplication : DaggerApplication() {
    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponent.builder().application(this).build()
    }
}
