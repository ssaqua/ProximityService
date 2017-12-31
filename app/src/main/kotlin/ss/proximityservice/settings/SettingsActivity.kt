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
import android.support.v7.app.AppCompatActivity
import android.view.View

import kotlinx.android.synthetic.main.activity_settings.*
import ss.proximityservice.ProximityService
import ss.proximityservice.R

class SettingsActivity : AppCompatActivity() {
    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                INTENT_SET_ACTIVE_ACTION -> setActive()
                INTENT_SET_INACTIVE_ACTION -> setInactive()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (Build.VERSION.SDK_INT >= 21) {
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
