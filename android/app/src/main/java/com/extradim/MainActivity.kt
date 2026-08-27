package com.extradim

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.extradim.quick.QuickDimTileService
import com.extradim.ui.MainScreen
import com.extradim.ui.MainViewModel
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Permission state is refreshed on resume.
        }

    private val writeSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Write-settings state is refreshed on resume.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var overlayPermissionGranted by remember { mutableStateOf(Settings.canDrawOverlays(this)) }
            var writeBrightnessGranted by remember {
                mutableStateOf(Settings.System.canWrite(this))
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        overlayPermissionGranted = Settings.canDrawOverlays(this@MainActivity)
                        writeBrightnessGranted = Settings.System.canWrite(this@MainActivity)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val dimLevel by viewModel.dimLevel.collectAsStateWithLifecycle()
            val enabled by viewModel.enabled.collectAsStateWithLifecycle()

            MainScreen(
                dimLevel = dimLevel,
                enabled = enabled,
                overlayPermissionGranted = overlayPermissionGranted,
                canLowerSystemBrightness = writeBrightnessGranted,
                onDimLevelChanged = viewModel::onSliderChanged,
                onEnabledChanged = viewModel::setEnabled,
                onRequestOverlayPermission = {
                    overlayPermissionLauncher.launch(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName"),
                        ),
                    )
                },
                onRequestBrightnessPermission = {
                    writeSettingsLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        },
                    )
                },
                onAddToQuickSettings = { addQuickSettingsTile() },
            )
        }
    }

    /**
     * Prompts the system to add our Quick Settings tile (Android 13+). The system
     * shows its own confirmation dialog; the tile is then available to drag into
     * the QS panel.
     */
    private fun addQuickSettingsTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, "Add tiles from the Quick Settings editor", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val statusBarManager =
                getSystemService(Context.STATUS_BAR_SERVICE) as StatusBarManager
            statusBarManager.requestAddTileService(
                ComponentName(this, QuickDimTileService::class.java),
                getString(R.string.tile_label),
                Icon.createWithResource(this, R.drawable.ic_quick_settings),
                Executors.newSingleThreadExecutor(),
            ) { status ->
                val msg = when (status) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> "Extra Dim tile added"
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "Tile already added"
                    else -> "Could not add tile"
                }
                runOnUiThread {
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Open Quick Settings and edit tiles", Toast.LENGTH_LONG).show()
        }
    }
}
