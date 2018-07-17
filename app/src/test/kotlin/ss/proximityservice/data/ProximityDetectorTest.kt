package ss.proximityservice.data

import android.hardware.Sensor
import android.hardware.SensorEvent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ProximityDetectorTest {

    @Mock
    private lateinit var mockListener: ProximityDetector.ProximityListener

    private lateinit var proximityDetector: ProximityDetector

    @Before
    fun init() {
        proximityDetector = ProximityDetector(mockListener)
    }

    @Test
    fun onNear() {
        proximityDetector.onSensorChanged(mockSensorEvent(0f))
        verify(mockListener, only()).onNear()
    }

    @Test
    fun onFar() {
        proximityDetector.onSensorChanged(mockSensorEvent(8f))
        verify(mockListener, only()).onFar()
    }

    private fun mockSensorEvent(value: Float): SensorEvent {
        val sensorEvent = mock(SensorEvent::class.java)

        try {
            val valuesField = SensorEvent::class.java.getField("values")
            valuesField.isAccessible = true
            try {
                valuesField.set(sensorEvent, floatArrayOf(value))
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        }

        sensorEvent.sensor = mock(Sensor::class.java)
        `when`(sensorEvent.sensor.maximumRange).thenReturn(8f)

        return sensorEvent
    }
}
