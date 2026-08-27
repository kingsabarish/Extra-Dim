package com.extradim.dim

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.extradim.ExtraDimApp
import com.extradim.MainActivity
import com.extradim.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the dim overlay alive. It reads the persisted
 * dim level and drives the shared [DimController]:
 *
 * - Started with [ACTION_START] -> shows the overlay at the saved level.
 * - Started with [ACTION_STOP]  -> removes the overlay and stops the service.
 * - Started with [ACTION_UPDATE]-> re-applies the (possibly changed) level.
 *
 * It also observes the repository's `isEnabled` flag and stops itself if dim
 * is turned off from the app, keeping the tile and the app in sync.
 */
class DimService : Service() {

    private val app: ExtraDimApp get() = application as ExtraDimApp

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("ExtraDimService", "onStartCommand action=$action")

        if (action == ACTION_STOP) {
            app.container.dimController.clear()
            app.container.brightnessController.restore()
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_UPDATE || action == ACTION_START) {
            // Apply the persisted level on START (fresh toggle from the tile or
            // app) and on UPDATE (slider moved). Reading once here means the
            // saved brightness is honoured even when dim was enabled first.
            applyPersistedLevel()
            ensureForeground()
            startObserving()
            // On a fresh start, also pull the system brightness down so the
            // overlay has a darker base to work on ("dim even more").
            if (action == ACTION_START) {
                app.container.brightnessController.lowerToFloor()
            }
        }

        return START_STICKY
    }

    private fun applyPersistedLevel() {
        scope.launch {
            app.container.settingsRepository.dimLevel.collect { level ->
                app.container.dimController.setDimLevel(level)
            }
        }
    }

    override fun onDestroy() {
        app.container.dimController.clear()
        app.container.brightnessController.restore()
        observeJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startObserving() {
        if (observeJob != null) return
        observeJob = scope.launch {
            app.container.settingsRepository.isEnabled.collectLatest { enabled ->
                if (!enabled) {
                    app.container.dimController.clear()
                    stopSelf()
                }
            }
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_quick_settings)
            .setContentTitle(getString(R.string.dim_notification_title))
            .setContentText(getString(R.string.dim_notification_text))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.dim_notification_title),
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "dim_service"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START = "com.extradim.action.START"
        const val ACTION_STOP = "com.extradim.action.STOP"
        const val ACTION_UPDATE = "com.extradim.action.UPDATE"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, DimService::class.java).setAction(ACTION_START))
        }

        fun update(context: Context) {
            context.startService(Intent(context, DimService::class.java).setAction(ACTION_UPDATE))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, DimService::class.java).setAction(ACTION_STOP))
        }
    }
}
