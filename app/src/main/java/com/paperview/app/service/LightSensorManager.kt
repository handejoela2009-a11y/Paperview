package com.paperview.app.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Envuelve el sensor de luz ambiental (Sensor.TYPE_LIGHT), que es un sensor
 * "normal" de Android disponible sin permisos especiales en runtime. No todos
 * los dispositivos lo traen: `isAvailable` refleja eso honestamente para que
 * la UI pueda mostrar "Sensor de luz: no disponible" (sección 19).
 *
 * Aplica una doble suavización, tal como pide la sección 10:
 *  1. Un filtro de media móvil exponencial sobre las lecturas crudas, para
 *     ignorar parpadeos de milisegundos.
 *  2. Una histéresis temporal: un valor nuevo solo se publica como
 *     "estable" si se ha mantenido fuera de la banda muerta durante
 *     STABLE_WINDOW_MS. Esto evita que AutoAdaptationManager dispare una
 *     transición por un cambio de luz de medio segundo (alguien pasando
 *     delante de una ventana, por ejemplo).
 */
class LightSensorManager(context: Context) : SensorEventListener {

    companion object {
        private const val EMA_ALPHA = 0.25f
        private const val DEAD_BAND_LUX = 15f
        private const val STABLE_WINDOW_MS = 4000L
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    val isAvailable: Boolean get() = lightSensor != null

    private val _stableLux = MutableStateFlow<Float?>(null)
    /** Nivel de luz ya estabilizado (histéresis aplicada). Null si el sensor no existe
     *  o aún no hay una lectura estable. */
    val stableLux: StateFlow<Float?> = _stableLux.asStateFlow()

    private var smoothedLux: Float? = null
    private var candidateLux: Float? = null
    private var candidateSince: Long = 0L

    fun start() {
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_LIGHT) return
        val raw = event.values.firstOrNull() ?: return

        val smoothed = smoothedLux?.let { it + EMA_ALPHA * (raw - it) } ?: raw
        smoothedLux = smoothed

        val now = System.currentTimeMillis()
        val currentStable = _stableLux.value

        if (currentStable == null || kotlin.math.abs(smoothed - currentStable) > DEAD_BAND_LUX) {
            // Fuera de la banda muerta: es un candidato a nuevo valor estable.
            if (candidateLux == null || kotlin.math.abs(smoothed - (candidateLux ?: smoothed)) > DEAD_BAND_LUX) {
                candidateLux = smoothed
                candidateSince = now
            }
            if (now - candidateSince >= STABLE_WINDOW_MS) {
                _stableLux.value = candidateLux
                candidateLux = null
            }
        } else {
            // Dentro de la banda muerta respecto al valor estable actual: se descarta el candidato.
            candidateLux = null
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
