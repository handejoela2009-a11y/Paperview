package com.paperview.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paperview.app.data.PaperViewPresets
import com.paperview.app.data.PaperViewSettings
import com.paperview.app.data.PreferencesRepository
import com.paperview.app.service.LightSensorManager
import com.paperview.app.service.OverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PaperViewUiState(
    val isActive: Boolean = false,
    val settings: PaperViewSettings = PaperViewPresets.PAPEL_NATURAL,
    val hasOverlayPermission: Boolean = false,
    val lightSensorAvailable: Boolean = false,
    val isOnboarded: Boolean = true,
    val breakReminderMinutes: Long = 0L,
)

class PaperViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PreferencesRepository(application)
    private val lightSensorManager = LightSensorManager(application)

    // SYSTEM_ALERT_WINDOW no expone un Flow nativo de cambios: este contador
    // se incrementa manualmente (ver refreshPermissionState) para forzar una
    // reevaluación de hasOverlayPermission cuando el usuario vuelve de Ajustes.
    private val permissionRefreshTick = MutableStateFlow(0)

    val uiState = combine(
        repository.isActive,
        repository.currentSettings,
        repository.isOnboarded,
        repository.breakReminderMinutes,
        permissionRefreshTick,
    ) { active, settings, onboarded, breakMinutes, _ ->
        PaperViewUiState(
            isActive = active,
            settings = settings,
            hasOverlayPermission = OverlayService.hasOverlayPermission(application),
            lightSensorAvailable = lightSensorManager.isAvailable,
            isOnboarded = onboarded,
            breakReminderMinutes = breakMinutes,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaperViewUiState())

    fun refreshPermissionState() {
        // Se llama desde onResume de la Activity: el permiso pudo cambiar
        // en Ajustes del sistema mientras la app estaba en segundo plano.
        permissionRefreshTick.value += 1
    }

    fun toggleActive() {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val active = uiState.value.isActive
            if (!active) {
                if (!OverlayService.hasOverlayPermission(app)) return@launch
                repository.setActive(true)
                OverlayService.start(app)
            } else {
                repository.setActive(false)
                OverlayService.stop(app)
            }
        }
    }

    fun selectPreset(settings: PaperViewSettings) {
        viewModelScope.launch { repository.applyPreset(settings) }
    }

    fun updateTemperature(value: Float) = updateField { it.copy(temperature = value) }
    fun updateBlueReduction(value: Float) = updateField { it.copy(blueReduction = value) }
    fun updateSaturation(value: Float) = updateField { it.copy(saturation = value) }
    fun updateContrast(value: Float) = updateField { it.copy(contrast = value) }
    fun updateIntensity(value: Float) = updateField { it.copy(intensity = value) }
    fun updateOverlayDimming(value: Float) = updateField { it.copy(overlayDimming = value) }

    private fun updateField(transform: (PaperViewSettings) -> PaperViewSettings) {
        viewModelScope.launch { repository.updateField(transform) }
    }

    fun setAutoAdaptation(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoAdaptation(enabled) }
    }

    fun setBreakReminder(minutes: Long) {
        viewModelScope.launch { repository.setBreakReminderMinutes(minutes) }
    }

    fun restoreRecommended() {
        viewModelScope.launch { repository.restoreRecommended() }
    }

    fun completeOnboarding(chosenPreset: PaperViewSettings) {
        viewModelScope.launch {
            repository.applyPreset(chosenPreset)
            repository.setOnboarded()
        }
    }
}
