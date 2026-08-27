package com.extradim.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.extradim.ExtraDimApp
import com.extradim.dim.DimService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the one-page dimmer UI. Keeps the slider value and the on/off state in
 * sync with the persistence layer (and therefore with the Quick Settings tile)
 * by exposing them as [StateFlow]s, and pushes changes to [DimService].
 *
 * - [dimLevel] is the persisted dim strength (0 = no dim up to 1 = full black).
 *   The slider presents it as brightness (1 - dimLevel), full by default.
 * - [enabled] reflects whether the overlay is active.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app: ExtraDimApp get() = getApplication()
    private val repository = app.container.settingsRepository

    val dimLevel: StateFlow<Float> = repository.dimLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    val enabled: StateFlow<Boolean> = repository.isEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Called while the user drags the slider: persist and live-apply if on. */
    fun onSliderChanged(level: Float) {
        viewModelScope.launch {
            repository.setDimLevel(level)
            if (repository.isEnabled.first()) {
                DimService.update(getApplication())
            }
        }
    }

    /** Toggle the overlay on/off. The dim level the user set is preserved. */
    fun setEnabled(value: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(value)
            if (value) {
                DimService.start(getApplication())
            } else {
                DimService.stop(getApplication())
            }
        }
    }
}
