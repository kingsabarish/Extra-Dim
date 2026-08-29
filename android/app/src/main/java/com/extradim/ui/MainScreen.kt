package com.extradim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.extradim.ui.theme.ExtraDimTheme

/**
 * The single-page dimmer UI.
 *
 * @param dimLevel persisted dim strength (0..1) pushed up from the ViewModel.
 * @param enabled whether the overlay is active.
 * @param overlayPermissionGranted whether the user has granted the
 *      "Display over other apps" permission needed for the overlay.
 * @param onDimLevelChanged drag the slider (called with dim level 0..1).
 * @param onEnabledChanged toggles the dim overlay on/off.
 * @param onRequestOverlayPermission opens the system permission screen.
 */
@Composable
fun MainScreen(
    dimLevel: Float,
    enabled: Boolean,
    overlayPermissionGranted: Boolean,
    onDimLevelChanged: (Float) -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onAddToQuickSettings: () -> Unit,
) {
    ExtraDimTheme {
        // The slider shows "brightness" (full by default); dim is its inverse.
        val brightness = 1f - dimLevel

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Extra Dim",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Dimming",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChanged,
                )
            }
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Brightness",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${(brightness * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Slider(
                value = brightness,
                onValueChange = { onDimLevelChanged(1f - it) },
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onAddToQuickSettings,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Add to Quick Settings")
            }

            if (!overlayPermissionGranted) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Grant \"Display over other apps\" so Extra Dim can dim the whole screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onRequestOverlayPermission,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Grant permission")
                }
            }
        }
    }
}
