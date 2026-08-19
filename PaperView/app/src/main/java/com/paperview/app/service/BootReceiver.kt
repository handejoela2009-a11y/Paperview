package com.paperview.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.paperview.app.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reanuda el overlay tras un reinicio SOLO si el usuario lo tenía activo y
 * el permiso de overlay sigue concedido. No reactiva nada por sí sola si
 * cualquiera de esas dos condiciones falta (sección 32: no fingir que algo
 * está activo cuando no lo está).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = PreferencesRepository(context)
                val wasActive = repo.isActive.first()
                if (wasActive && OverlayService.hasOverlayPermission(context)) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, OverlayService::class.java),
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
