package com.extradim.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists the dim settings in a DataStore-backed preferences store.
 *
 * - [enabled] whether the dim overlay is currently active.
 * - [dimLevel] the requested dim level, 0f (brightest / no overlay) .. 1f
 *   (fully black / as dark as possible). The slider in the app maps to this.
 */
class SettingsRepository(private val context: Context) {

    private val Context.dataStore by preferencesDataStore(name = "extra_dim_settings")

    private val KEY_ENABLED = booleanPreferencesKey("enabled")
    private val KEY_DIM_LEVEL = floatPreferencesKey("dim_level")

    companion object {
        /** Dim level used when the user has never set one (visible, not transparent). */
        const val DEFAULT_DIM_LEVEL = 0.6f
    }

    val isEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_ENABLED] ?: false }

    val dimLevel: Flow<Float> = context.dataStore.data
        .map { (it[KEY_DIM_LEVEL] ?: DEFAULT_DIM_LEVEL).coerceIn(0f, 1f) }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setDimLevel(level: Float) {
        context.dataStore.edit { it[KEY_DIM_LEVEL] = level.coerceIn(0f, 1f) }
    }
}
