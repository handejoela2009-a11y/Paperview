package com.paperview.app

import com.paperview.app.data.PaperViewPresets
import com.paperview.app.filter.FilterEngine
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterEngineTest {

    @Test
    fun `tint alpha never exceeds the conservative cap even at maximum intensity`() {
        val extreme = PaperViewPresets.NOCHE.copy(
            temperature = 1f, blueReduction = 1f, intensity = 1f,
        )
        val appearance = FilterEngine.toAppearance(extreme)
        // El propio motor limita el alfa del tinte a 115/255 (~45%) para que
        // el resultado nunca se lea como "pantalla amarilla" (sección 38).
        assertTrue(appearance.tintAlpha <= 115)
    }

    @Test
    fun `zero intensity produces a near-imperceptible tint`() {
        val minimal = PaperViewPresets.PAPEL_NATURAL.copy(intensity = 0f)
        val appearance = FilterEngine.toAppearance(minimal)
        assertTrue(appearance.tintAlpha <= 5)
        assertTrue(appearance.dimAlpha == 0)
    }

    @Test
    fun `lerp at fraction zero equals start and at one equals target`() {
        val start = FilterEngine.toAppearance(PaperViewPresets.PAPEL_BLANCO)
        val target = FilterEngine.toAppearance(PaperViewPresets.NOCHE)

        val atZero = FilterEngine.lerp(start, target, 0f)
        val atOne = FilterEngine.lerp(start, target, 1f)

        assertTrue(atZero.tintAlpha == start.tintAlpha)
        assertTrue(atOne.tintAlpha == target.tintAlpha)
    }

    @Test
    fun `dim alpha stays within a safe upper bound`() {
        val settings = PaperViewPresets.NOCHE.copy(overlayDimming = 1f, intensity = 1f)
        val appearance = FilterEngine.toAppearance(settings)
        assertTrue(appearance.dimAlpha <= 150)
    }
}
