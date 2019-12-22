package ss.proximityservice

import android.os.Build
import androidx.test.ext.junit.rules.activityScenarioRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import ss.proximityservice.ProximityService.Companion.INTENT_ACTION_START

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class StartActivityTest {

    @get:Rule
    val scenarioRule = activityScenarioRule<StartActivity>()

    @Test
    fun `starts ProximityService`() {
        scenarioRule.scenario.onActivity { activity ->
            val serviceIntent = shadowOf(activity).peekNextStartedService()
            assertThat(serviceIntent.action).isEqualTo(INTENT_ACTION_START)
            assertThat(serviceIntent.component?.className).isEqualTo(ProximityService::class.java.canonicalName)
        }
    }

    @Test
    fun `is finishing immediately`() {
        scenarioRule.scenario.onActivity { activity ->
            assertThat(activity.isFinishing).isTrue()
        }
    }
}
