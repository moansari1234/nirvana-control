package com.nirvana.control.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.nirvana.control.model.ScannedDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BluetoothScanner(private val context: Context) {
    companion object {
        private const val TAG = "BluetoothScanner"
        private const val SCAN_DURATION_MS = 12000L
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var stopScanJob: Job? = null

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: result.scanRecord?.deviceName ?: return
            val address = device.address
            val rssi = result.rssi

            val isNirvana = name.contains("nirvana", ignoreCase = true) ||
                    name.contains("boat", ignoreCase = true) ||
                    name.contains("space", ignoreCase = true)

            val isPaired = bluetoothAdapter?.bondedDevices?.any { it.address == address } == true

            _scannedDevices.update { list ->
                val existing = list.find { it.address == address }
                if (existing != null) {
                    list.map { if (it.address == address) it.copy(rssi = rssi, name = name) else it }
                } else {
                    list + ScannedDevice(
                        name = name,
                        address = address,
                        rssi = rssi,
                        isPaired = isPaired,
                        isNirvana = isNirvana
                    )
                }.sortedWith(
                    compareByDescending<ScannedDevice> { it.isNirvana }
                        .thenByDescending { it.isPaired }
                        .thenByDescending { it.rssi }
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan failed: ")
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (_isScanning.value) return
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return

        // First, add all paired devices
        val paired = bluetoothAdapter.bondedDevices.map { device ->
            val name = device.name ?: "Bluetooth Device"
            val isNirvana = name.contains("nirvana", ignoreCase = true) ||
                    name.contains("boat", ignoreCase = true) ||
                    name.contains("space", ignoreCase = true)
            ScannedDevice(
                name = name,
                address = device.address,
                rssi = -60,
                isPaired = true,
                isNirvana = isNirvana
            )
        }.sortedByDescending { it.isNirvana }

        _scannedDevices.value = paired
        _isScanning.value = true

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scan: ")
            _isScanning.value = false
            return
        }

        stopScanJob?.cancel()
        stopScanJob = scope.launch {
            delay(SCAN_DURATION_MS)
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan: ")
        }
        _isScanning.value = false
    }
}
