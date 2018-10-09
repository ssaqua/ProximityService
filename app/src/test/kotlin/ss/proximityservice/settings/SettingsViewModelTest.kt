package ss.proximityservice.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import ss.proximityservice.ProximityService
import ss.proximityservice.data.AppStorage
import ss.proximityservice.data.Mode

@RunWith(MockitoJUnitRunner::class)
class SettingsViewModelTest {

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: SettingsViewModel

    @Mock
    private lateinit var appStorage: AppStorage

    @Before
    fun init() {
        viewModel = SettingsViewModel(appStorage)
    }

    @Test
    fun `updateState() to active`() {
        viewModel.updateState(ProximityService.INTENT_NOTIFY_ACTIVE)
        assert(viewModel.serviceState.value == true)
    }

    @Test
    fun `updateState() to inactive`() {
        viewModel.updateState(ProximityService.INTENT_NOTIFY_INACTIVE)
        assert(viewModel.serviceState.value == false)
    }

    @Test
    fun `getNextIntentAction() returns action to invert ProximityService state`() {
        ProximityService.isRunning = false
        assert(viewModel.getNextIntentAction() == ProximityService.INTENT_ACTION_START)
        ProximityService.isRunning = true
        assert(viewModel.getNextIntentAction() == ProximityService.INTENT_ACTION_STOP)
    }

    @Test
    fun `screenOffDelayUpdate() should store the input progress Int`() {
        viewModel.screenOffDelayUpdate(1)
        verify(appStorage).put(SCREEN_OFF_DELAY, 1)
    }

    @Test
    fun `appStorage operational mode should be reset to default if stored value is not a known value`() {
        `when`(appStorage.getInt(OPERATIONAL_MODE, Mode.DEFAULT.ordinal)).thenReturn(Int.MAX_VALUE)
        viewModel = SettingsViewModel(appStorage)
        verify(appStorage).put(OPERATIONAL_MODE, Mode.DEFAULT.ordinal)
    }

    @Test
    fun `appStorage screen off delay should be reset to zero if stored value is outside known range`() {
        `when`(appStorage.getInt(SCREEN_OFF_DELAY, 0)).thenReturn(Int.MAX_VALUE)
        viewModel = SettingsViewModel(appStorage)
        verify(appStorage).put(SCREEN_OFF_DELAY, 0)
        assert(viewModel.screenOffDelayProgress.value == 0)
    }
}
