package com.paperview.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "paperview_settings")

/**
 * Única fuente de verdad persistida (sección 31: "Persistencia" como capa
 * separada). Todo lo que la UI y el servicio de overlay leen/escriben pasa
 * por aquí, para que ambos procesos vean siempre el mismo estado.
 */
class PreferencesRepository(private val context: Context) {

    private object Keys {
        val ACTIVE = booleanPreferencesKey("paperview_active")
        val PRESET_ID = stringPreferencesKey("preset_id")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val BLUE_REDUCTION = floatPreferencesKey("blue_reduction")
        val SATURATION = floatPreferencesKey("saturation")
        val CONTRAST = floatPreferencesKey("contrast")
        val INTENSITY = floatPreferencesKey("intensity")
        val OVERLAY_DIMMING = floatPreferencesKey("overlay_dimming")
        val BACKGROUND_WARMTH = floatPreferencesKey("background_warmth")
        val AUTO_ADAPT = booleanPreferencesKey("auto_adaptation")
        val TRANSITION_MS = longPreferencesKey("transition_speed_ms")
        val ONBOARDED = booleanPreferencesKey("onboarding_done")
        val BREAK_REMINDER_MIN = longPreferencesKey("break_reminder_minutes") // 0 = desactivado
    }

    val isActive: Flow<Boolean> = context.dataStore.data.map { it[Keys.ACTIVE] ?: false }
    val isOnboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDED] ?: false }
    val breakReminderMinutes: Flow<Long> = context.dataStore.data.map { it[Keys.BREAK_REMINDER_MIN] ?: 0L }

    val currentSettings: Flow<PaperViewSettings> = context.dataStore.data.map { prefs ->
        val base = PaperViewPresets.byId(prefs[Keys.PRESET_ID] ?: PaperViewPresets.PAPEL_NATURAL.id)
        PaperViewSettings(
            id = base.id,
            displayName = base.displayName,
            temperature = prefs[Keys.TEMPERATURE] ?: base.temperature,
            blueReduction = prefs[Keys.BLUE_REDUCTION] ?: base.blueReduction,
            saturation = prefs[Keys.SATURATION] ?: base.saturation,
            contrast = prefs[Keys.CONTRAST] ?: base.contrast,
            intensity = prefs[Keys.INTENSITY] ?: base.intensity,
            overlayDimming = prefs[Keys.OVERLAY_DIMMING] ?: base.overlayDimming,
            backgroundWarmth = prefs[Keys.BACKGROUND_WARMTH] ?: base.backgroundWarmth,
            autoAdaptationEnabled = prefs[Keys.AUTO_ADAPT] ?: base.autoAdaptationEnabled,
            transitionSpeedMs = prefs[Keys.TRANSITION_MS] ?: base.transitionSpeedMs,
        )
    }

    suspend fun setActive(active: Boolean) {
        context.dataStore.edit { it[Keys.ACTIVE] = active }
    }

    suspend fun setOnboarded() {
        context.dataStore.edit { it[Keys.ONBOARDED] = true }
    }

    suspend fun applyPreset(settings: PaperViewSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PRESET_ID] = settings.id
            prefs[Keys.TEMPERATURE] = settings.temperature
            prefs[Keys.BLUE_REDUCTION] = settings.blueReduction
            prefs[Keys.SATURATION] = settings.saturation
            prefs[Keys.CONTRAST] = settings.contrast
            prefs[Keys.INTENSITY] = settings.intensity
            prefs[Keys.OVERLAY_DIMMING] = settings.overlayDimming
            prefs[Keys.BACKGROUND_WARMTH] = settings.backgroundWarmth
            prefs[Keys.AUTO_ADAPT] = settings.autoAdaptationEnabled
            prefs[Keys.TRANSITION_MS] = settings.transitionSpeedMs
        }
    }

    /** Modificación fina de un único slider, marcando el preset como "personalizado". */
    suspend fun updateField(update: (PaperViewSettings) -> PaperViewSettings) {
        context.dataStore.edit { prefs ->
            val base = PaperViewSettings(
                id = prefs[Keys.PRESET_ID] ?: "personalizado",
                temperature = prefs[Keys.TEMPERATURE] ?: 0.35f,
                blueReduction = prefs[Keys.BLUE_REDUCTION] ?: 0.3f,
                saturation = prefs[Keys.SATURATION] ?: 0.42f,
                contrast = prefs[Keys.CONTRAST] ?: 0.5f,
                intensity = prefs[Keys.INTENSITY] ?: 0.5f,
                overlayDimming = prefs[Keys.OVERLAY_DIMMING] ?: 0f,
                backgroundWarmth = prefs[Keys.BACKGROUND_WARMTH] ?: 0.3f,
                autoAdaptationEnabled = prefs[Keys.AUTO_ADAPT] ?: true,
                transitionSpeedMs = prefs[Keys.TRANSITION_MS] ?: 1500L,
            )
            val updated = update(base).copy(id = "personalizado", displayName = "Personalizado")
            prefs[Keys.PRESET_ID] = updated.id
            prefs[Keys.TEMPERATURE] = updated.temperature
            prefs[Keys.BLUE_REDUCTION] = updated.blueReduction
            prefs[Keys.SATURATION] = updated.saturation
            prefs[Keys.CONTRAST] = updated.contrast
            prefs[Keys.INTENSITY] = updated.intensity
            prefs[Keys.OVERLAY_DIMMING] = updated.overlayDimming
            prefs[Keys.BACKGROUND_WARMTH] = updated.backgroundWarmth
        }
    }

    suspend fun setAutoAdaptation(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_ADAPT] = enabled }
    }

    suspend fun setBreakReminderMinutes(minutes: Long) {
        context.dataStore.edit { it[Keys.BREAK_REMINDER_MIN] = minutes }
    }

    suspend fun restoreRecommended() {
        applyPreset(PaperViewPresets.PAPEL_NATURAL)
    }
}
