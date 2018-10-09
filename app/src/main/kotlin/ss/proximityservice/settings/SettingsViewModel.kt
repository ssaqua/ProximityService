package ss.proximityservice.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.afollestad.materialdialogs.GravityEnum
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.StackingBehavior
import ss.proximityservice.ProximityService
import ss.proximityservice.ProximityService.Companion.INTENT_ACTION_START
import ss.proximityservice.ProximityService.Companion.INTENT_ACTION_STOP
import ss.proximityservice.ProximityService.Companion.INTENT_NOTIFY_ACTIVE
import ss.proximityservice.ProximityService.Companion.INTENT_NOTIFY_INACTIVE
import ss.proximityservice.R
import ss.proximityservice.data.Alert
import ss.proximityservice.data.AppStorage
import ss.proximityservice.data.Event
import javax.inject.Inject

class SettingsViewModel @Inject constructor(private val appStorage: AppStorage) : ViewModel() {

    private val _serviceState = MutableLiveData<Boolean>()
    private val _alert = MutableLiveData<Event<Alert>>()
    private val _notificationBehaviorResId = MutableLiveData<Int>()
    private val _screenOffDelayResId = MutableLiveData<Int>()
    private val _screenOffDelayProgress = MutableLiveData<Int>()

    val serviceState: LiveData<Boolean>
        get() = _serviceState

    val alert: LiveData<Event<Alert>>
        get() = _alert

    val notificationBehaviorResId: LiveData<Int>
        get() = _notificationBehaviorResId

    val screenOffDelayResId: LiveData<Int>
        get() = _screenOffDelayResId

    val screenOffDelayProgress: LiveData<Int>
        get() = _screenOffDelayProgress

    init {
        _serviceState.value = ProximityService.isRunning
        _notificationBehaviorResId.value = if (appStorage.getBoolean(
                NOTIFICATION_DISMISS,
                true
            )
        ) R.string.settings_notification_behavior_secondary_dismiss else R.string.settings_notification_behavior_secondary_dismiss
        var screenOffDelay = appStorage.getInt(SCREEN_OFF_DELAY, 0)
        _screenOffDelayResId.value = when (screenOffDelay) {
            0 -> R.string.screen_off_delay_zero
            1 -> R.string.screen_off_delay_one
            2 -> R.string.screen_off_delay_two
            3 -> R.string.screen_off_delay_three
            else -> {
                // reset to zero
                appStorage.put(SCREEN_OFF_DELAY, 0)
                screenOffDelay = 0
                R.string.screen_off_delay_zero
            }
        }
        _screenOffDelayProgress.value = screenOffDelay
    }

    fun updateState(intentAction: String?) {
        when (intentAction) {
            INTENT_NOTIFY_ACTIVE -> _serviceState.postValue(true)
            INTENT_NOTIFY_INACTIVE -> _serviceState.postValue(false)
        }
    }

    fun getNextIntentAction(): String =
        if (ProximityService.isRunning) INTENT_ACTION_STOP else INTENT_ACTION_START

    fun notificationBehaviorClick() {
        _alert.postValue(Event(object : Alert {
            override fun show(context: Context) {
                MaterialDialog.Builder(context)
                    .title(R.string.settings_notification_behavior_title)
                    .content(R.string.settings_notification_behavior_description)
                    .positiveText(R.string.dismiss)
                    .negativeText(R.string.retain)
                    .btnStackedGravity(GravityEnum.START)
                    .stackingBehavior(StackingBehavior.ALWAYS)
                    .onPositive { _, _ ->
                        appStorage.put(NOTIFICATION_DISMISS, true)
                        _notificationBehaviorResId.postValue(R.string.settings_notification_behavior_secondary_dismiss)
                    }
                    .onNegative { _, _ ->
                        appStorage.put(NOTIFICATION_DISMISS, false)
                        _notificationBehaviorResId.postValue(R.string.settings_notification_behavior_secondary_retain)
                    }
                    .show()
            }
        }))
    }

    fun screenOffDelayProgress(progress: Int) {
        when (progress) {
            0 -> _screenOffDelayResId.postValue(R.string.screen_off_delay_zero)
            1 -> _screenOffDelayResId.postValue(R.string.screen_off_delay_one)
            2 -> _screenOffDelayResId.postValue(R.string.screen_off_delay_two)
            3 -> _screenOffDelayResId.postValue(R.string.screen_off_delay_three)
        }
    }

    fun screenOffDelayUpdate(progress: Int) {
        appStorage.put(SCREEN_OFF_DELAY, progress)
    }

}
