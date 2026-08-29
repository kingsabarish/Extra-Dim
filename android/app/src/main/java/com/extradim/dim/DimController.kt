package com.extradim.dim

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlin.math.roundToInt

/**
 * Manages the single full-screen overlay window that dims the screen below the
 * system minimum. The overlay sits on top of whatever the system brightness
 * renders, so the system brightness and this overlay work in combination.
 *
 * The overlay is a plain color layer whose alpha encodes the dim level:
 * [dimLevel] = 0f -> fully transparent (no dimming), 1f -> fully black.
 */
class DimController(
    private val context: Context,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlay: View? = null

    /**
     * True only when the overlay window is currently attached to the screen.
     */
    val isActive: Boolean get() = overlay != null

    /**
     * Applies a new dim level. If the overlay is not yet shown it is added
     * first; otherwise the existing window's alpha is updated in place.
     */
    fun setDimLevel(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        val view = overlay
        if (view == null) {
            addOverlay(clamped)
        } else {
            view.background.alpha = alphaFor(clamped)
        }
    }

    /**
     * Removes the overlay window entirely (dims off). Calling when not active
     * is a no-op.
     */
    fun clear() {
        val view = overlay ?: return
        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) {
            // Window was already detached.
        } finally {
            overlay = null
        }
    }

    private fun addOverlay(level: Float) {
        val view = View(context).apply {
            setBackgroundColor(Color.BLACK)
            background.alpha = alphaFor(level)
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
            // Ensure view itself does not consume insets.
            fitsSystemWindows = false
        }

        // FLAG_LAYOUT_INSET_DECOR ensures the window decor is laid out under
        // system bars; FLAG_NOT_TOUCH_MODAL prevents touch modal clipping.
        // Together with LAYOUT_IN_SCREEN + LAYOUT_NO_LIMITS the window spans
        // the full display including the navigation bar (which was previously
        // left bright on some devices/modes).
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.TOP or Gravity.START

        // On Android 11+ the window would otherwise be inset to avoid the
        // navigation / status bars. Disabling insets guarantees edge-to-edge
        // coverage including the navigation bar.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.fitInsetsTypes = 0
            @Suppress("DEPRECATION")
            params.fitInsetsSides = 0
        }

        // Cover the full screen including the display cutout so the darkest
        // setting is truly black edge to edge.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        windowManager.addView(view, params)
        overlay = view
    }

    private fun alphaFor(level: Float): Int =
        (level * 255f).roundToInt().coerceIn(0, 255)
}
