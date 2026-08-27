package com.extradim.dim

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import kotlin.math.min

/**
 * Lowers the device's system brightness toward its floor while Extra Dim is
 * active, in addition to the black overlay. The overlay alone can only darken
 * down to pure black, which is already the physical limit; dropping the system
 * brightness as well makes the *base* the overlay draws on much darker, so the
 * combined result is noticeably dimmer than the overlay by itself (this is the
 * "dim even more" behaviour).
 *
 * Requires the privileged [android.Manifest.permission.WRITE_SETTINGS] right,
 * which the user grants through [android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS].
 * Every operation is a no-op when the right is not held, so the overlay keeps
 * working without it.
 */
class BrightnessController(private val context: Context) {

    private val resolver: ContentResolver get() = context.contentResolver

    // System brightness is 0..255. We never force it to 0 (that would make the
    // overlay's slider meaningless); instead we pull it down to a low floor so
    // the overlay still has room to fade between "dim" and "black".
    private val floor = 6

    private var savedBrightness: Int? = null
    private var savedAutoMode: Int? = null

    /** True only when the app is allowed to write system brightness. */
    fun canWrite(): Boolean = Settings.System.canWrite(context)

    /** Drop system brightness to the floor, remembering the previous values. */
    fun lowerToFloor() {
        if (!canWrite()) return
        try {
            savedAutoMode = Settings.System.getInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
            )
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            )
            val current = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
            savedBrightness = current
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, min(current, floor))
        } catch (_: Exception) {
            savedBrightness = null
            savedAutoMode = null
        }
    }

    /** Restore the brightness and auto mode the user had before dimming. */
    fun restore() {
        if (!canWrite()) return
        try {
            savedBrightness?.let {
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, it)
            }
            savedAutoMode?.let {
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, it)
            }
        } catch (_: Exception) {
            // Best effort; nothing else we can do.
        }
        savedBrightness = null
        savedAutoMode = null
    }
}
