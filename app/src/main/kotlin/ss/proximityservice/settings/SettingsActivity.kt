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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import com.afollestad.materialdialogs.GravityEnum
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.StackingBehavior
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_settings.*
import ss.proximityservice.ProximityService
import ss.proximityservice.ProximityService.Companion.INTENT_NOTIFY_ACTIVE
import ss.proximityservice.ProximityService.Companion.INTENT_NOTIFY_INACTIVE
import ss.proximityservice.R
import ss.proximityservice.data.AppStorage
import javax.inject.Inject

class SettingsActivity : DaggerAppCompatActivity() {

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                INTENT_NOTIFY_ACTIVE -> setActive()
                INTENT_NOTIFY_INACTIVE -> setInactive()
            }
        }
    }

    @Inject
    lateinit var appStorage: AppStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val taskDescription = ActivityManager.TaskDescription(
                getString(R.string.app_name),
                BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher),
                resources.getColor(R.color.primaryDark)
            )
            setTaskDescription(taskDescription)
        }

        btn_service_on.setOnClickListener {
            startService(
                Intent(this, ProximityService::class.java)
                    .setAction(ProximityService.INTENT_START_ACTION)
            )
            setActive()
        }

        btn_service_off.setOnClickListener {
            startService(
                Intent(this, ProximityService::class.java)
                    .setAction(ProximityService.INTENT_STOP_ACTION)
            )
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
                .onAny { _, which ->
                    when (which.name) {
                        "POSITIVE" -> {
                            appStorage.put(NOTIFICATION_DISMISS, true)
                            notification_behavior_secondary_text.text =
                                    getString(R.string.settings_notification_behavior_secondary_dismiss)
                        }
                        "NEGATIVE" -> {
                            appStorage.put(NOTIFICATION_DISMISS, false)
                            notification_behavior_secondary_text.text =
                                    getString(R.string.settings_notification_behavior_secondary_retain)
                        }
                    }
                }
                .show()
        }

        setting_screen_off_delay_seekbar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                when (progress) {
                    0 -> screen_off_delay_secondary_text.text =
                            getString(R.string.screen_off_delay_zero)
                    1 -> screen_off_delay_secondary_text.text =
                            getString(R.string.screen_off_delay_one)
                    2 -> screen_off_delay_secondary_text.text =
                            getString(R.string.screen_off_delay_two)
                    3 -> screen_off_delay_secondary_text.text =
                            getString(R.string.screen_off_delay_three)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                appStorage.put(SCREEN_OFF_DELAY, seekBar.progress)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_open_source_licenses -> {
                startActivity(Intent(this, OssLicensesMenuActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!ProximityService.running) {
            setInactive()
        } else {
            setActive()
        }

        updateWithCurrentSettingValues()

        val filter = IntentFilter()
        filter.addAction(INTENT_NOTIFY_ACTIVE)
        filter.addAction(INTENT_NOTIFY_INACTIVE)
        LocalBroadcastManager.getInstance(this).registerReceiver(stateReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stateReceiver)
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

    private fun updateWithCurrentSettingValues() {
        if (!appStorage.getBoolean(NOTIFICATION_DISMISS, true)) {
            notification_behavior_secondary_text.text =
                    getString(R.string.settings_notification_behavior_secondary_retain)
        }

        var screenOffDelay = appStorage.getInt(SCREEN_OFF_DELAY, 0)
        when (screenOffDelay) {
            0 -> screen_off_delay_secondary_text.text = getString(R.string.screen_off_delay_zero)
            1 -> screen_off_delay_secondary_text.text = getString(R.string.screen_off_delay_one)
            2 -> screen_off_delay_secondary_text.text = getString(R.string.screen_off_delay_two)
            3 -> screen_off_delay_secondary_text.text = getString(R.string.screen_off_delay_three)
            else -> {
                // reset to zero
                screen_off_delay_secondary_text.text = getString(R.string.screen_off_delay_zero)
                appStorage.put(SCREEN_OFF_DELAY, 0)
                screenOffDelay = 0
            }
        }
        setting_screen_off_delay_seekbar.progress = screenOffDelay
    }
}
