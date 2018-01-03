package ss.proximityservice.settings

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.view.View
import com.afollestad.materialdialogs.GravityEnum
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.StackingBehavior
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_settings.*
import ss.proximityservice.ProximityService
import ss.proximityservice.R
import ss.proximityservice.data.AppStorage
import javax.inject.Inject

class SettingsActivity : DaggerAppCompatActivity() {
    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                INTENT_SET_ACTIVE_ACTION -> setActive()
                INTENT_SET_INACTIVE_ACTION -> setInactive()
            }
        }
    }

    @Inject lateinit var appStorage: AppStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val taskDescription = ActivityManager.TaskDescription(
                    getString(R.string.app_name),
                    BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher),
                    resources.getColor(R.color.primaryDark)
            )
            setTaskDescription(taskDescription)
        }

        btn_service_on.setOnClickListener {
            startService(Intent(this, ProximityService::class.java)
                    .setAction(ProximityService.INTENT_START_ACTION))
            setActive()
        }

        btn_service_off.setOnClickListener {
            startService(Intent(this, ProximityService::class.java)
                    .setAction(ProximityService.INTENT_STOP_ACTION))
            setInactive()
        }

        setting_notification_behavior.setOnClickListener {
            MaterialDialog.Builder(this)
                    .title(getString(R.string.settings_notification_behavior_title))
                    .content(R.string.settings_notification_behavior_description)
                    .positiveText(R.string.dismiss)
                    .negativeText(R.string.retain)
                    .btnStackedGravity(GravityEnum.START)
                    .stackingBehavior(StackingBehavior.ALWAYS)
                    .onAny { _, which -> when (which.name) {
                        "POSITIVE" -> {
                            appStorage.put(NOTIFICATION_DISMISS, true)
                            notification_behavior_secondary_text.text = getString(R.string.settings_notification_behavior_secondary_dismiss)
                        }
                        "NEGATIVE" -> {
                            appStorage.put(NOTIFICATION_DISMISS, false)
                            notification_behavior_secondary_text.text = getString(R.string.settings_notification_behavior_secondary_retain)
                        }
                    }}
                    .show()
        }
    }

    private fun setActive() {
        condition_card.setBackgroundColor(ContextCompat.getColor(this, R.color.accent))
        tv_condition.text = getText(R.string.condition_active)
        btn_service_on.visibility = View.GONE
        btn_service_off.visibility = View.VISIBLE
    }

    private fun setInactive() {
        condition_card.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryLight))
        tv_condition.text = getText(R.string.condition_inactive)
        btn_service_off.visibility = View.GONE
        btn_service_on.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        if (!ProximityService.running) {
            setInactive()
        } else {
            setActive()
        }

        val filter = IntentFilter()
        filter.addAction(INTENT_SET_ACTIVE_ACTION)
        filter.addAction(INTENT_SET_INACTIVE_ACTION)
        LocalBroadcastManager.getInstance(this).registerReceiver(stateReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stateReceiver)
    }

    companion object {
        const val INTENT_SET_ACTIVE_ACTION = "ss.proximityservice.SET_ACTIVE"
        const val INTENT_SET_INACTIVE_ACTION = "ss.proximityservice.SET_INACTIVE"
    }
}
