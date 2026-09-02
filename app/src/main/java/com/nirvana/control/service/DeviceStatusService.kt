package com.nirvana.control.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nirvana.control.MainActivity
import com.nirvana.control.bluetooth.BluetrumSppManager
import com.nirvana.control.model.AncMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DeviceStatusService : Service() {
    companion object {
        const val CHANNEL_ID = "nirvana_status_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.nirvana.control.action.START"
        const val ACTION_STOP = "com.nirvana.control.action.STOP"
        const val ACTION_SET_ANC = "com.nirvana.control.action.SET_ANC"
        const val EXTRA_ANC_MODE = "extra_anc_mode"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var sppManager: BluetrumSppManager

    override fun onCreate() {
        super.onCreate()
        sppManager = BluetrumSppManager.getInstance(this)
        createNotificationChannel()

        sppManager.deviceState.onEach { state ->
            if (state.isConnected) {
                updateNotification(state)
            } else {
                stopSelf()
            }
        }.launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_SET_ANC -> {
                val modeOrdinal = intent.getIntExtra(EXTRA_ANC_MODE, 0)
                val mode = AncMode.entries.getOrElse(modeOrdinal) { AncMode.OFF }
                sppManager.setAncMode(mode)
            }
            else -> {
                val notification = buildNotification(sppManager.deviceState.value)
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Earbuds Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows live battery status and ANC quick controls"
            setShowBadge(false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun updateNotification(state: com.nirvana.control.model.DeviceState) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: com.nirvana.control.model.DeviceState): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun createAncPendingIntent(mode: AncMode, reqCode: Int): PendingIntent {
            val intent = Intent(this, DeviceStatusService::class.java).apply {
                action = ACTION_SET_ANC
                putExtra(EXTRA_ANC_MODE, mode.ordinal)
            }
            return PendingIntent.getService(
                this, reqCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val batteryText = buildString {
            if (state.leftBattery >= 0) append("L: %")
            if (state.rightBattery >= 0) {
                if (isNotEmpty()) append("  |  ")
                append("R: %")
            }
            if (state.caseBattery >= 0) {
                if (isNotEmpty()) append("  |  ")
                append("Case: %")
            }
            if (isEmpty()) append("Connected")
        }

        val ancLabel = when (state.ancMode) {
            AncMode.ANC_ON -> "ANC: ON"
            AncMode.TRANSPARENCY -> "ANC: Transparency"
            AncMode.OFF -> "ANC: Off"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("boAt Nirvana Space - ")
            .setContentText(batteryText)
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Noise Cancel", createAncPendingIntent(AncMode.ANC_ON, 101))
            .addAction(0, "Transparency", createAncPendingIntent(AncMode.TRANSPARENCY, 102))
            .addAction(0, "Off", createAncPendingIntent(AncMode.OFF, 103))
            .build()
    }
}
