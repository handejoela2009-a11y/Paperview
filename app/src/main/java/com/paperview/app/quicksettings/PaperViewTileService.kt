package com.paperview.app.quicksettings

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.paperview.app.PaperViewApplication
import com.paperview.app.service.OverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Acceso rápido "ACTIVAR/DESACTIVAR PAPER" desde el panel de ajustes rápidos
 * de Android (sección 18). Respeta el mismo estado persistido que la app,
 * así que activar desde aquí o desde la UI principal es siempre coherente.
 */
class PaperViewTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        val repo = (application as PaperViewApplication).repository
        scope.launch {
            val active = repo.isActive.first()
            updateTile(active)
        }
    }

    override fun onClick() {
        super.onClick()
        val repo = (application as PaperViewApplication).repository
        scope.launch {
            val currentlyActive = repo.isActive.first()
            val next = !currentlyActive
            if (next && !OverlayService.hasOverlayPermission(this@PaperViewTileService)) {
                // No se puede activar honestamente sin el permiso: se deja el
                // tile en su estado real (inactivo) en lugar de fingir éxito.
                updateTile(false)
                return@launch
            }
            repo.setActive(next)
            if (next) {
                OverlayService.start(this@PaperViewTileService)
            } else {
                OverlayService.stop(this@PaperViewTileService)
            }
            updateTile(next)
        }
    }

    private fun updateTile(active: Boolean) {
        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "PaperView"
            updateTile()
        }
    }
}
