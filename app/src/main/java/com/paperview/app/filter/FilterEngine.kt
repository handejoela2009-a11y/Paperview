package com.paperview.app.filter

import android.graphics.Color
import android.graphics.PorterDuff
import com.paperview.app.data.PaperViewSettings
import kotlin.math.roundToInt

object FilterEngine {

    data class OverlayAppearance(
        val tintColor: Int,
        val tintAlpha: Int,
        val tintBlendMode: PorterDuff.Mode,
        val dimColor: Int,
        val dimAlpha: Int,
    )

    fun toAppearance(settings: PaperViewSettings): OverlayAppearance {
        val intensity = settings.intensity.coerceIn(0f, 1f)

        val warmth = (settings.temperature * 0.6f + settings.blueReduction * 0.4f).coerceIn(0f, 1f)
        val tintAlpha = ((0.08f + warmth * 0.25f) * intensity * 255f).roundToInt().coerceIn(0, 90)

        val desaturationPull = (0.5f - settings.saturation.coerceIn(0f, 1f)).coerceAtLeast(0f)
        val red = 250
        val green = (248 - warmth * 28 - desaturationPull * 10).roundToInt().coerceIn(210, 250)
        val blue = (240 - warmth * 75 - settings.blueReduction * 35).roundToInt().coerceIn(150, 240)
        val tintColor = Color.rgb(red, green, blue)

        val dimAlpha = (settings.overlayDimming.coerceIn(0f, 1f) * intensity * 255f * 0.55f)
            .roundToInt().coerceIn(0, 140)

        return OverlayAppearance(
            tintColor = tintColor,
            tintAlpha = tintAlpha,
            tintBlendMode = PorterDuff.Mode.MULTIPLY,
            dimColor = Color.BLACK,
            dimAlpha = dimAlpha,
        )
    }

    fun compose(appearance: OverlayAppearance): Int {
        if (appearance.dimAlpha <= 0) {
            return Color.argb(appearance.tintAlpha, Color.red(appearance.tintColor), Color.green(appearance.tintColor), Color.blue(appearance.tintColor))
        }
        val totalAlpha = (appearance.tintAlpha + appearance.dimAlpha).coerceAtMost(200)
        val dimWeight = appearance.dimAlpha.toFloat() / (appearance.tintAlpha + appearance.dimAlpha).coerceAtLeast(1)
        val r = (Color.red(appearance.tintColor) * (1 - dimWeight)).roundToInt().coerceIn(0, 255)
        val g = (Color.green(appearance.tintColor) * (1 - dimWeight)).roundToInt().coerceIn(0, 255)
        val b = (Color.blue(appearance.tintColor) * (1 - dimWeight)).roundToInt().coerceIn(0, 255)
        return Color.argb(totalAlpha, r, g, b)
    }

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
