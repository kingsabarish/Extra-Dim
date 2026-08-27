package com.extradim.quick

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.extradim.ExtraDimApp
import com.extradim.dim.DimService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Invisible activity launched by the Quick Settings tile to actually apply the
 * toggle. Starting the (foreground) dim service is only allowed from a
 * foreground context; a raw tile tap runs in the background and would throw a
 * [android.app.ForegroundServiceStartNotAllowedException]. This activity briefly
 * comes to the foreground (transparent + instant finish), starts/stops
 * [DimService] from the persisted enabled flag, then finishes with no visible
 * flicker.
 */
class ToggleActivity : ComponentActivity() {

    private val app: ExtraDimApp get() = application as ExtraDimApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            val enabled = app.container.settingsRepository.isEnabled.first()
            if (enabled) {
                DimService.start(this@ToggleActivity)
            } else {
                DimService.stop(this@ToggleActivity)
            }
            finish()
        }
    }
}
