package com.extradim.di

import android.content.Context
import com.extradim.data.SettingsRepository
import com.extradim.dim.DimController

/**
 * Minimal manual dependency container, shared app-wide. The application owns a
 * single [SettingsRepository] and [DimController] so the activity and the
 * Quick Settings tile observe and drive the same state. System brightness is
 * no longer touched — dimming is overlay-only so auto-brightness is preserved.
 */
class AppContainer(context: Context) {
    val settingsRepository: SettingsRepository = SettingsRepository(context)
    val dimController: DimController = DimController(context)
}
