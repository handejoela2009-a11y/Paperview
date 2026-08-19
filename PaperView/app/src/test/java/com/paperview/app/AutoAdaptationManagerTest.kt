package com.paperview.app

import com.paperview.app.data.PaperViewPresets
import com.paperview.app.service.AutoAdaptationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class AutoAdaptationManagerTest {

    private val manager = AutoAdaptationManager()

    @Test
    fun `morning time selects papel natural from default schedule`() {
        val preset = manager.presetForTime(LocalTime.of(8, 0))
        assertEquals(PaperViewPresets.PAPEL_NATURAL.id, preset.id)
    }

    @Test
    fun `evening time selects lectura from default schedule`() {
        val preset = manager.presetForTime(LocalTime.of(19, 30))
        assertEquals(PaperViewPresets.LECTURA.id, preset.id)
    }

    @Test
    fun `night time selects noche from default schedule`() {
        val preset = manager.presetForTime(LocalTime.of(22, 0))
        assertEquals(PaperViewPresets.NOCHE.id, preset.id)
    }

    @Test
    fun `very dark ambient light increases dimming conservatively`() {
        val base = PaperViewPresets.PAPEL_NATURAL
        val adjusted = manager.adjustForAmbientLight(base, stableLux = 2f)
        assertTrue(adjusted.overlayDimming > base.overlayDimming)
        assertTrue(adjusted.overlayDimming <= 0.5f)
    }

    @Test
    fun `bright sunlight reduces dimming instead of darkening further`() {
        val base = PaperViewPresets.NOCHE
        val adjusted = manager.adjustForAmbientLight(base, stableLux = 5000f)
        assertTrue(adjusted.overlayDimming < base.overlayDimming)
    }

    @Test
    fun `null light reading leaves settings unchanged`() {
        val base = PaperViewPresets.LIBRO
        val adjusted = manager.adjustForAmbientLight(base, stableLux = null)
        assertEquals(base, adjusted)
    }
}
