package ss.proximityservice.data

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

class ProximityDetector(private val listener: ProximityListener) : SensorEventListener {

    interface ProximityListener {
        fun onNear()
        fun onFar()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        val distance = event.values[0]
        val max = event.sensor.maximumRange

        if (distance < Math.min(max, 8f)) {
            listener.onNear()
        } else {
            listener.onFar()
        }
    }
}
