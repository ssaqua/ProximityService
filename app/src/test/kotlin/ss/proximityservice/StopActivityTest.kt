package ss.proximityservice

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class StopActivityTest {

    @Test
    fun stopsService() {
        val activity = Robolectric.setupActivity(StopActivity::class.java)
        val intent = Shadows.shadowOf(activity).peekNextStartedService()
        assert(intent.action == ProximityService.INTENT_ACTION_STOP)
        assert(intent.component.className == ProximityService::class.java.canonicalName)
    }
}
