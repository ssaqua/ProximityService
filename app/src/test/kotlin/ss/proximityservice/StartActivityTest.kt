package ss.proximityservice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class StartActivityTest {

    @Test
    fun `starts service in onCreate()`() {
        val controller = buildActivity(StartActivity::class.java)
        val activity = controller.get()

        controller.create()

        val intent = shadowOf(activity).peekNextStartedService()
        assertEquals(ProximityService.INTENT_ACTION_START, intent.action)
        assertEquals(ProximityService::class.java.canonicalName, intent.component?.className)
    }

    @Test
    fun `finishing after onCreate()`() {
        val controller = buildActivity(StartActivity::class.java)
        val activity = controller.get()

        controller.create()

        assertTrue(activity.isFinishing)
    }
}
