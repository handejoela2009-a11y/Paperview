package com.paperview.app.filter

import android.graphics.Color
import android.graphics.PorterDuff
import com.paperview.app.data.PaperViewSettings
import kotlin.math.roundToInt

/**
 * NOTA TÉCNICA IMPORTANTE (léase antes de tocar este archivo):
 *
 * Android NO ofrece ninguna API pública para aplicar una matriz de color
 * (temperatura/saturación/contraste/tono) a la totalidad de la pantalla,
 * incluyendo el contenido de otras apps. Esa capacidad existe únicamente
 * a nivel de sistema (Settings > Accesibilidad > Corrección de color, o
 * Settings.Secure.ACCESSIBILITY_DISPLAY_COLOR_MATRIX) y requiere permisos
 * de sistema (WRITE_SECURE_SETTINGS) o el propio ajuste manual del usuario;
 * una app de terceros no puede escribirlo sin root.
 *
 * Lo único que una app normal puede hacer, dentro de las reglas de Android,
 * es dibujar una ventana overlay (TYPE_APPLICATION_OVERLAY, concedida por
 * SYSTEM_ALERT_WINDOW) por encima de todo lo demás, y tintarla con un color
 * ARGB usando un modo de mezcla (PorterDuff / BlendMode). Esa es la técnica
 * real que usan apps de referencia como Twilight o Night Owl, y es también
 * la que usa PaperView.
 *
 * Esto significa que PaperView SIMULA temperatura/saturación/contraste
 * combinando:
 *   - un color de tinte cálido (sube rojo/verde, baja azul) con MULTIPLY
 *     para calentar y oscurecer ligeramente a la vez (similar a mirar a
 *     través de un cristal ahumado cálido), y
 *   - un alfa de oscurecimiento con SRC_OVER cuando el brillo real no está
 *     disponible.
 *
 * Esto NO es una matriz de color real: no puede aumentar el contraste de
 * negros que ya son negros puros, ni "desaturar" un color por debajo de lo
 * que el blend matemáticamente permite. Por eso los presets están calibrados
 * de forma conservadora, y por eso `intensity` limita el efecto máximo en
 * vez de dejar que el usuario lo lleve a un extremo que dejaría de parecer
 * papel para parecer "una pantalla amarilla" (sección 38 del proyecto).
 */
object FilterEngine {

    data class OverlayAppearance(
        val tintColor: Int,
        val tintAlpha: Int, // 0..255, fuerza del tinte de color (temperatura/saturación simuladas)
        val tintBlendMode: PorterDuff.Mode,
        val dimColor: Int,
        val dimAlpha: Int, // 0..255, oscurecimiento visual puro (no brillo real)
    )

    /**
     * Convierte los sliders (0f..1f, ver PaperViewSettings) en algo que un
     * View real puede dibujar. Todo clamp está aquí para que la UI nunca
     * pueda producir un overlay ilegible o "quemado" en amarillo.
     */
    fun toAppearance(settings: PaperViewSettings): OverlayAppearance {
        val intensity = settings.intensity.coerceIn(0f, 1f)

        // Componente de temperatura: reduce azul y sube ligeramente rojo,
        // proporcional a temperature + blueReduction, pero nunca a costa de
        // dejar el tinte con más de ~45% de alfa (más allá de eso deja de
        // leerse como "papel" y empieza a leerse como "filtro amarillo").
        val warmth = (settings.temperature * 0.6f + settings.blueReduction * 0.4f).coerceIn(0f, 1f)
        val tintAlpha = ((0.10f + warmth * 0.35f) * intensity * 255f).roundToInt().coerceIn(0, 115)

        // A mayor "saturation" objetivo más baja (target lejos de 0.5 hacia 0),
        // más rojo/naranja se necesita en el tinte para desaturar percibidamente
        // sin necesitar una matriz real.
        val desaturationPull = (0.5f - settings.saturation.coerceIn(0f, 1f)).coerceAtLeast(0f)
        val red = (222 + desaturationPull * 20).roundToInt().coerceIn(200, 255)
        val green = (196 - warmth * 40).roundToInt().coerceIn(150, 210)
        val blue = (150 - warmth * 70 - settings.blueReduction * 40).roundToInt().coerceIn(60, 170)
        val tintColor = Color.rgb(red, green, blue)

        // Oscurecimiento visual puro: solo entra si overlayDimming > 0
        // (p.ej. cuando el brillo real del sistema no se pudo ajustar, o en
        // el modo Noche). Nunca se mezcla con el tinte de color para no
        // duplicar el efecto de "bajar contraste" dos veces.
        val dimAlpha = (settings.overlayDimming.coerceIn(0f, 1f) * intensity * 255f * 0.6f)
            .roundToInt().coerceIn(0, 150)

        return OverlayAppearance(
            tintColor = tintColor,
            tintAlpha = tintAlpha,
            tintBlendMode = PorterDuff.Mode.MULTIPLY,
            dimColor = Color.BLACK,
            dimAlpha = dimAlpha,
        )
    }

    /** Interpola linealmente entre dos apariencias, usado por AutoAdaptationManager
     *  para las transiciones progresivas exigidas en las secciones 10 y 12. */
    fun lerp(from: OverlayAppearance, to: OverlayAppearance, fraction: Float): OverlayAppearance {
        val f = fraction.coerceIn(0f, 1f)
        return OverlayAppearance(
            tintColor = lerpColor(from.tintColor, to.tintColor, f),
            tintAlpha = lerpInt(from.tintAlpha, to.tintAlpha, f),
            tintBlendMode = to.tintBlendMode,
            dimColor = to.dimColor,
            dimAlpha = lerpInt(from.dimAlpha, to.dimAlpha, f),
        )
    }

    private fun lerpInt(a: Int, b: Int, f: Float) = (a + (b - a) * f).roundToInt()

    private fun lerpColor(a: Int, b: Int, f: Float): Int {
        val ar = Color.red(a); val ag = Color.green(a); val ab = Color.blue(a)
        val br = Color.red(b); val bg = Color.green(b); val bb = Color.blue(b)
        return Color.rgb(
            (ar + (br - ar) * f).roundToInt().coerceIn(0, 255),
            (ag + (bg - ag) * f).roundToInt().coerceIn(0, 255),
            (ab + (bb - ab) * f).roundToInt().coerceIn(0, 255),
        )
    }
}
