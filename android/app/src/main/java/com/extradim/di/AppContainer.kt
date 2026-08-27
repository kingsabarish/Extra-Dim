package com.extradim.di

import android.content.Context
import com.extradim.data.SettingsRepository
import com.extradim.dim.BrightnessController
import com.extradim.dim.DimController

/**
 * Minimal manual dependency container, shared app-wide. The application owns a
 * single [SettingsRepository], [DimController] and [BrightnessController] so the
 * activity and the Quick Settings tile observe and drive the same state.
 */
class AppContainer(context: Context) {
    val settingsRepository: SettingsRepository = SettingsRepository(context)
    val dimController: DimController = DimController(context)
    val brightnessController: BrightnessController = BrightnessController(context)
}
