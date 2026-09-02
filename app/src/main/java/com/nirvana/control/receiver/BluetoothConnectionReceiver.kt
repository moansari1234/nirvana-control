package com.nirvana.control.receiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nirvana.control.bluetooth.BluetrumSppManager
import com.nirvana.control.service.DeviceStatusService

class BluetoothConnectionReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BtConnectionReceiver"
    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) ?: return
        val deviceName = device?.name ?: ""

        val isNirvana = deviceName.contains("nirvana", ignoreCase = true) ||
                deviceName.contains("boat", ignoreCase = true) ||
                deviceName.contains("space", ignoreCase = true)

        if (!isNirvana) return

        val manager = BluetrumSppManager.getInstance(context)

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                Log.i(TAG, "boAt device connected:  ()")
                // Check if auto-connect is enabled
                if (manager.deviceState.value.autoConnectEnabled) {
                    manager.connect(device!!)
                    val serviceIntent = Intent(context, DeviceStatusService::class.java).apply {
                        this.action = DeviceStatusService.ACTION_START
                    }
                    try {
                        context.startForegroundService(serviceIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting foreground service: ")
                    }
                } else {
                    Log.d(TAG, "Auto-connect is disabled. Remaining dormant.")
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                Log.i(TAG, "boAt device disconnected: ")
                manager.disconnect()
                val stopIntent = Intent(context, DeviceStatusService::class.java).apply {
                    this.action = DeviceStatusService.ACTION_STOP
                }
                context.stopService(stopIntent)
            }
        }
    }
}
