package ss.proximityservice.settings

import android.app.ActivityManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View

import kotlinx.android.synthetic.main.activity_settings.*
import ss.proximityservice.ProximityService
import ss.proximityservice.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        btn_service_on.setOnClickListener {
            startService(Intent(this, ProximityService::class.java))
            setActive()
        }

        btn_service_off.setOnClickListener {
            stopService(Intent(this, ProximityService::class.java))
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
        if (Build.VERSION.SDK_INT >= 21) {
            val taskDescription = ActivityManager.TaskDescription(
                    getString(R.string.app_name),
                    BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher),
                    resources.getColor(R.color.primaryDark)
            )
            setTaskDescription(taskDescription)
        }
        if (!ProximityService.running) {
            setInactive()
        } else {
            setActive()
        }
    }
}
