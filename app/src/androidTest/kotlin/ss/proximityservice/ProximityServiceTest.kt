package ss.proximityservice

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(AndroidJUnit4::class)
class ProximityServiceTest {

    @Rule
    @JvmField
    val rule: ServiceTestRule = ServiceTestRule.withTimeout(1, TimeUnit.SECONDS)

    @Test(expected = TimeoutException::class)
    fun bindingNotSupported() {
        val intent = Intent(getApplicationContext(), ProximityService::class.java)
        rule.bindService(intent)
    }

    @Test
    fun handleStartAction() {
        val intent = Intent(getApplicationContext(), TestProximityService::class.java)
        intent.action = ProximityService.INTENT_ACTION_START
        rule.startService(intent)
        assertTrue(ProximityService.isRunning)
    }

    /**
     * Test service which supports binding to allow [ServiceTestRule] to manage its lifecycle.
     */
    class TestProximityService : ProximityService() {

        inner class LocalBinder : Binder()

        override fun onBind(intent: Intent): IBinder? = LocalBinder()
    }
}
