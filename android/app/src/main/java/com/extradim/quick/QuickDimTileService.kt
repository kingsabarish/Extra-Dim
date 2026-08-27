package com.extradim.quick

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.extradim.ExtraDimApp
import com.extradim.dim.DimService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Quick Settings tile for Extra Dim.
 *
 * Tap toggles dim on/off. On Android 12–14 starting a foreground service from a
 * tile tap was forbidden, so the (transparent) [ToggleActivity] was used to get
 * a foreground context. On Android 15+ a tile tap is treated as a valid
 * foreground initiation, so we try to start the service directly and only fall
 * back to [ToggleActivity] when that is rejected. Either way the tile never
 * opens the app: it only flips the persisted `enabled` flag and asks
 * [DimService] to show/hide the overlay.
 *
 * The tile mirrors the app's enabled state via [onStartListening].
 */
class QuickDimTileService : TileService() {

    private val app: ExtraDimApp get() = application as ExtraDimApp
    private var listeningScope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        listeningScope = scope
        scope.launch {
            app.container.settingsRepository.isEnabled.collect { enabled ->
                updateTile(enabled)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        listeningScope?.cancel()
        listeningScope = null
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) {
            unlockAndRun { toggle() }
        } else {
            toggle()
        }
    }

    private fun toggle() {
        // Fresh scope every tap so the toggle always runs, even outside a
        // listening session (e.g. an unlocked tap straight after collapse).
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            val repo = app.container.settingsRepository
            val currentlyEnabled = repo.isEnabled.first()
            val next = !currentlyEnabled
            Log.d("ExtraDimTile", "toggle: currentlyEnabled=$currentlyEnabled next=$next")
            // Flip the persisted flag and let the service apply the dim level the
            // user already set (don't overwrite their brightness setting).
            repo.setEnabled(next)
            if (next) {
                startDimService()
            } else {
                DimService.stop(this@QuickDimTileService)
            }
            updateTile(next)
        }
    }

    private fun startDimService() {
        try {
            DimService.start(this@QuickDimTileService)
        } catch (e: Exception) {
            // Older Android, or the direct start was rejected: use the
            // transparent activity to obtain a foreground context.
            startToggleActivity()
        }
    }

    private fun startToggleActivity() {
        val intent = Intent(this, ToggleActivity::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            if (Build.VERSION.SDK_INT >= 28) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTile(enabled: Boolean) {
        qsTile?.let {
            it.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            it.updateTile()
        }
    }
}
