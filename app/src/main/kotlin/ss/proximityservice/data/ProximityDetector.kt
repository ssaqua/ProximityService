package ss.proximityservice.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class ProximityDetector(context: Context, private val listener: ProximityListener) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var sensor: Sensor? = null

    init {
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        sensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        val distance = event.values[0]
        val max = event.sensor.maximumRange

        if (distance < Math.min(max, 8.toFloat())) {
            listener.onNear()
        } else {
            listener.onFar()
        }
    }

    fun close() = sensorManager.unregisterListener(this)

    interface ProximityListener {
        fun onNear()
        fun onFar()
    }
}
