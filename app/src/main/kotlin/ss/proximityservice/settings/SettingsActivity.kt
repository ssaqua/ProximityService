package ss.proximityservice.settings

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_settings.*
import ss.proximityservice.ProximityService
import ss.proximityservice.ProximityService.Companion.INTENT_NOTIFY_ACTIVE
import ss.proximityservice.ProximityService.Companion.INTENT_NOTIFY_INACTIVE
import ss.proximityservice.R
import ss.proximityservice.data.EventObserver
import javax.inject.Inject

class SettingsActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: SettingsViewModel

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.updateState(intent.action)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(SettingsViewModel::class.java)

        viewModel.serviceState.observe(this, Observer(::updateConditionCard))
        viewModel.alert.observe(this, EventObserver { dialog -> dialog.show(this) })
        viewModel.operationalModeResId.observe(this, Observer { resId ->
            @SuppressLint("ResourceType")
            operational_mode_secondary_text.text = getString(resId)
        })
        viewModel.notificationBehaviorResId.observe(this, Observer { resId ->
            @SuppressLint("ResourceType")
            notification_behavior_secondary_text.text = getString(resId)
        })
        viewModel.screenOffDelayResId.observe(this, Observer { resId ->
            @SuppressLint("ResourceType")
            screen_off_delay_secondary_text.text = getString(resId)
        })
        viewModel.screenOffDelayProgress.observe(this, Observer { progress ->
            setting_screen_off_delay_seekbar.progress = progress
        })

        btn_service.setOnClickListener {
            startService(
                Intent(this, ProximityService::class.java)
                    .setAction(viewModel.getNextIntentAction())
            )
        }

        setting_operational_mode.setOnClickListener { viewModel.operationalModeClick() }
        setting_notification_behavior.setOnClickListener { viewModel.notificationBehaviorClick() }

        setting_screen_off_delay_seekbar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                viewModel.screenOffDelayProgress(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                viewModel.screenOffDelayUpdate(seekBar.progress)
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val taskDescription = ActivityManager.TaskDescription(
                getString(R.string.app_name),
                BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher),
                resources.getColor(R.color.primaryDark)
            )
            setTaskDescription(taskDescription)
        }
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

        val filter = IntentFilter().apply {
            addAction(INTENT_NOTIFY_ACTIVE)
            addAction(INTENT_NOTIFY_INACTIVE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(stateReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stateReceiver)
    }

    private fun updateConditionCard(serviceState: Boolean) {
        val colorId = if (serviceState) R.color.accent else R.color.primaryLight
        val conditionTextId =
            if (serviceState) R.string.condition_active else R.string.condition_inactive
        val btnTextId = if (serviceState) R.string.button_off else R.string.button_on
        condition_card.setBackgroundColor(ContextCompat.getColor(this, colorId))
        tv_condition.text = getString(conditionTextId)
        btn_service.text = getString(btnTextId)
    }
}
