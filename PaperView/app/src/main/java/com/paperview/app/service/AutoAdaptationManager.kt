package com.paperview.app.service

import com.paperview.app.data.PaperViewPresets
import com.paperview.app.data.PaperViewSettings
import java.time.LocalTime

/**
 * Implementa AUTO PAPER (sección 11) y el horario de ejemplo de la sección 26.
 * No decide brillo real ni escribe overlays directamente: solo calcula, dado
 * un momento y una lectura de luz ambiental, cuál debería ser la configuración
 * "objetivo". Quien la aplique (OverlayService) es responsable de hacerlo con
 * una transición progresiva usando FilterEngine.lerp, nunca de golpe.
 */
class AutoAdaptationManager {

    data class Schedule(val time: LocalTime, val preset: PaperViewSettings)

    /** Horario por defecto descrito en la sección 26. El usuario puede
     *  sobrescribirlo desde ajustes; aquí solo se documenta el valor inicial. */
    val defaultSchedule = listOf(
        Schedule(LocalTime.of(6, 0), PaperViewPresets.PAPEL_NATURAL),
        Schedule(LocalTime.of(18, 0), PaperViewPresets.LECTURA),
        Schedule(LocalTime.of(21, 0), PaperViewPresets.NOCHE),
    )

    /** Devuelve el preset que correspondería solo por hora del día. */
    fun presetForTime(now: LocalTime, schedule: List<Schedule> = defaultSchedule): PaperViewSettings {
        val sorted = schedule.sortedBy { it.time }
        val applicable = sorted.lastOrNull { it.time <= now } ?: sorted.last()
        return applicable.preset
    }

    /**
     * Ajusta un preset base según la luz ambiental estabilizada (lux), sin
     * cambiar de identidad de preset: solo modula intensidad/oscurecimiento
     * dentro de márgenes conservadores, para cumplir "colores naturales" y
     * evitar que auto-adaptación se sienta como un filtro que aparece y
     * desaparece de forma agresiva.
     *
     * - Ambientes muy oscuros (<10 lux): sube un poco el oscurecimiento visual
     *   y la calidez, ya que una pantalla brillante en un cuarto oscuro es
     *   una fuente conocida de deslumbramiento (sección 27).
     * - Ambientes muy luminosos (>1000 lux, luz solar directa): baja el
     *   oscurecimiento (para no perder legibilidad contra el sol) mientras
     *   mantiene la reducción de azul.
     */
    fun adjustForAmbientLight(base: PaperViewSettings, stableLux: Float?): PaperViewSettings {
        if (stableLux == null) return base
        return when {
            stableLux < 10f -> base.copy(
                overlayDimming = (base.overlayDimming + 0.15f).coerceAtMost(0.5f),
                intensity = (base.intensity + 0.05f).coerceAtMost(1f),
            )
            stableLux > 1000f -> base.copy(
                overlayDimming = (base.overlayDimming - 0.15f).coerceAtLeast(0f),
            )
            else -> base
        }
    }
}
