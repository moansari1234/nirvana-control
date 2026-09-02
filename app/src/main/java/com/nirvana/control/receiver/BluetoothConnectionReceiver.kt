package com.nirvana.control.receiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nirvana.control.bluetooth.BluetrumSppManager
import com.nirvana.control.service.DeviceStatusService
import com.nirvana.control.util.AppLog as Log

class BluetoothConnectionReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BtConnectionReceiver"
    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val manager = BluetrumSppManager.getInstance(context)

        when (action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        Log.i(TAG, "Bluetooth turned ON")
                        manager.setBluetoothEnabled(true)
                        if (manager.deviceState.value.autoConnectEnabled) {
                            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                            val nirvana = btManager?.adapter?.bondedDevices?.find {
                                val name = it.name ?: ""
                                name.contains("nirvana", ignoreCase = true) ||
                                name.contains("boat", ignoreCase = true) ||
                                name.contains("space", ignoreCase = true)
                            }
                            if (nirvana != null) {
                                Log.i(TAG, "Auto-connecting to  after Bluetooth enabled")
                                manager.connect(nirvana)
                            }
                        }
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        Log.i(TAG, "Bluetooth turned OFF")
                        manager.setBluetoothEnabled(false)
                        val stopIntent = Intent(context, DeviceStatusService::class.java).apply {
                            this.action = DeviceStatusService.ACTION_STOP
                        }
                        context.stopService(stopIntent)
                    }
                }
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }

                val deviceName = device?.name ?: ""
                val isNirvana = deviceName.contains("nirvana", ignoreCase = true) ||
                        deviceName.contains("boat", ignoreCase = true) ||
                        deviceName.contains("space", ignoreCase = true)

                if (!isNirvana || device == null) return

                Log.i(TAG, "boAt device connected:  ()")
                if (manager.deviceState.value.autoConnectEnabled) {
                    manager.connect(device)
                    val serviceIntent = Intent(context, DeviceStatusService::class.java).apply {
                        this.action = DeviceStatusService.ACTION_START
                    }
                    try {
                        context.startForegroundService(serviceIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting foreground service: ")
                    }
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }

                val deviceName = device?.name ?: ""
                val isNirvana = deviceName.contains("nirvana", ignoreCase = true) ||
                        deviceName.contains("boat", ignoreCase = true) ||
                        deviceName.contains("space", ignoreCase = true)

                if (!isNirvana) return

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
