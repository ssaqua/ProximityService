package ss.proximityservice

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric.setupActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class StartActivityTest {

    @Test
    fun startsService() {
        val activity = setupActivity(StartActivity::class.java)
        val intent = shadowOf(activity).peekNextStartedService()
        assert(intent.action == ProximityService.INTENT_START_ACTION)
        assert(intent.component.className == ProximityService::class.java.canonicalName)
    }
}
