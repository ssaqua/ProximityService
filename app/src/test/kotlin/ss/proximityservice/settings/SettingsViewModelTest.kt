package ss.proximityservice.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*
import ss.proximityservice.ProximityService
import ss.proximityservice.data.Alert
import ss.proximityservice.data.AppStorage
import ss.proximityservice.data.Event
import ss.proximityservice.data.Mode
import ss.proximityservice.mock

class SettingsViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    val mockAppStorage: AppStorage = mock()

    @Test
    fun `serviceState with false initial value`() {
        ProximityService.isRunning = false

        val viewModel = SettingsViewModel(mock())

        assertThat(viewModel.serviceState.value).isFalse()
    }

    @Test
    fun `serviceState with true initial value`() {
        ProximityService.isRunning = true

        val viewModel = SettingsViewModel(mock())

        assertThat(viewModel.serviceState.value).isTrue()
    }

    @Test
    fun `updateState() to active results in observing true on serviceState`() {
        val viewModel = SettingsViewModel(mock())
        val observer: Observer<Boolean> = mock()
        viewModel.serviceState.observeForever(observer)
        reset(observer)

        viewModel.updateState(ProximityService.INTENT_NOTIFY_ACTIVE)

        verify(observer).onChanged(true)
    }

    @Test
    fun `updateState() to inactive results in observing false on serviceState`() {
        val viewModel = SettingsViewModel(mock())
        val observer: Observer<Boolean> = mock()
        viewModel.serviceState.observeForever(observer)
        reset(observer)

        viewModel.updateState(ProximityService.INTENT_NOTIFY_INACTIVE)

        verify(observer).onChanged(false)
    }

    @Test
    fun `getNextIntentAction() returns start action when service is not running`() {
        ProximityService.isRunning = false
        val viewModel = SettingsViewModel(mock())

        assertThat(viewModel.getNextIntentAction()).isEqualTo(ProximityService.INTENT_ACTION_START)
    }

    @Test
    fun `getNextIntentAction() returns stop action when service is running`() {
        ProximityService.isRunning = true
        val viewModel = SettingsViewModel(mock())

        assertThat(viewModel.getNextIntentAction()).isEqualTo(ProximityService.INTENT_ACTION_STOP)
    }

    @Test
    fun `screenOffDelayUpdate() should store the input progress Int`() {
        val viewModel = SettingsViewModel(mockAppStorage)

        viewModel.screenOffDelayUpdate(1)

        verify(mockAppStorage).put(SCREEN_OFF_DELAY, 1)
    }

    @Test
    fun `appStorage operational mode should be reset to default if stored value is not a known value`() {
        `when`(
            mockAppStorage.getInt(
                OPERATIONAL_MODE,
                Mode.DEFAULT.ordinal
            )
        ).thenReturn(Int.MAX_VALUE)

        SettingsViewModel(mockAppStorage)

        verify(mockAppStorage).put(OPERATIONAL_MODE, Mode.DEFAULT.ordinal)
    }

    @Test
    fun `mockAppStorage screen off delay should be reset to zero if stored value is outside known range`() {
        `when`(mockAppStorage.getInt(SCREEN_OFF_DELAY, 0)).thenReturn(Int.MAX_VALUE)

        SettingsViewModel(mockAppStorage)

        verify(mockAppStorage).put(SCREEN_OFF_DELAY, 0)
    }

    @Test
    fun `observe Alert on operationalModeClick()`() {
        val viewModel = SettingsViewModel(mock())
        val observer: Observer<Event<Alert>> = mock()
        viewModel.alert.observeForever(observer)

        viewModel.operationalModeClick()

        verify(observer).onChanged(any())
    }

    @Test
    fun `observe Alert on notificationBehaviorClick()`() {
        val viewModel = SettingsViewModel(mock())
        val observer: Observer<Event<Alert>> = mock()
        viewModel.alert.observeForever(observer)

        viewModel.notificationBehaviorClick()

        verify(observer).onChanged(any())
    }
}
