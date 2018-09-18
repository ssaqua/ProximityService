package ss.proximityservice

import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import ss.proximityservice.di.DaggerTestAppComponent

class TestApplication : ProximityApplication() {

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerTestAppComponent.builder().application(this).build()
    }
}
