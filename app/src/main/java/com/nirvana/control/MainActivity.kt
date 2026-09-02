package com.nirvana.control

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nirvana.control.bluetooth.BluetoothScanner
import com.nirvana.control.bluetooth.BluetrumSppManager
import com.nirvana.control.model.ConnectionState
import com.nirvana.control.ui.navigation.AppTab
import com.nirvana.control.ui.navigation.NirvanaBottomBar
import com.nirvana.control.ui.screens.*
import com.nirvana.control.ui.theme.NirvanaControlTheme
import com.nirvana.control.ui.theme.PureBlack

class MainActivity : ComponentActivity() {
    private lateinit var sppManager: BluetrumSppManager
    private lateinit var scanner: BluetoothScanner

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            checkAndAutoConnect()
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val isEnabled = btManager?.adapter?.isEnabled == true
        sppManager.setBluetoothEnabled(isEnabled)
        if (isEnabled) {
            checkAndAutoConnect()
        }
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                val isEnabled = state == BluetoothAdapter.STATE_ON
                sppManager.setBluetoothEnabled(isEnabled)
                if (isEnabled) {
                    checkAndAutoConnect()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sppManager = BluetrumSppManager.getInstance(this)
        scanner = BluetoothScanner(this)

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val isEnabled = btManager?.adapter?.isEnabled == true
        sppManager.setBluetoothEnabled(isEnabled)

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)

        requestRequiredPermissions()

        setContent {
            NirvanaControlTheme {
                val deviceState by sppManager.deviceState.collectAsStateWithLifecycle()
                val scannedDevices by scanner.scannedDevices.collectAsStateWithLifecycle()
                val isScanning by scanner.isScanning.collectAsStateWithLifecycle()

                var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
                var showScannerDialog by remember { mutableStateOf(false) }

                Scaffold(
                    bottomBar = {
                        NirvanaBottomBar(
                            currentTab = currentTab,
                            onTabSelected = { currentTab = it }
                        )
                    },
                    containerColor = PureBlack
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            AppTab.DASHBOARD -> {
                                DashboardScreen(
                                    deviceState = deviceState,
                                    onSetAncMode = { sppManager.setAncMode(it) },
                                    onSetGameMode = { sppManager.setGameMode(it) },
                                    onSetInEarDetection = { sppManager.setInEarDetection(it) },
                                    onEnableBluetooth = {
                                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                        enableBluetoothLauncher.launch(enableBtIntent)
                                    },
                                    onOpenScanner = {
                                        showScannerDialog = true
                                        scanner.startScan()
                                    },
                                    onConnect = { checkAndAutoConnect() },
                                    onDisconnect = { sppManager.disconnect() }
                                )
                            }
                            AppTab.EQUALIZER -> {
                                EqualizerScreen(
                                    currentGains = deviceState.equalizerGains,
                                    activePresetName = deviceState.activePresetName,
                                    onApplyGains = { gains, presetName ->
                                        sppManager.setEqualizerGains(gains, presetName)
                                    }
                                )
                            }
                            AppTab.SPATIAL -> {
                                SpatialStudioScreen(
                                    currentMode = deviceState.spatialAudioMode,
                                    onSetMode = { sppManager.setSpatialAudioMode(it) },
                                    onRecenter = { sppManager.recenterHeadTracking() }
                                )
                            }
                            AppTab.GESTURES -> {
                                GesturesScreen(
                                    keyMappings = deviceState.keyMappings,
                                    accidentalTouchGuard = deviceState.accidentalTouchGuard,
                                    onSetGesture = { gesture, func ->
                                        sppManager.setTouchGesture(gesture, func)
                                    },
                                    onSetAccidentalTouchGuard = { sppManager.setAccidentalTouchGuard(it) }
                                )
                            }
                            AppTab.SETTINGS -> {
                                SettingsScreen(
                                    deviceState = deviceState,
                                    onSetAutoConnect = { sppManager.setAutoConnectEnabled(it) },
                                    onSetInEarDetection = { sppManager.setInEarDetection(it) },
                                    onOpenScanner = {
                                        showScannerDialog = true
                                        scanner.startScan()
                                    },
                                    onRefreshInfo = { sppManager.refreshDeviceInfo() },
                                    onDisconnect = { sppManager.disconnect() }
                                )
                            }
                        }

                        if (showScannerDialog) {
                            ScannerDialog(
                                scannedDevices = scannedDevices,
                                isScanning = isScanning,
                                onStartScan = { scanner.startScan() },
                                onStopScan = { scanner.stopScan() },
                                onDeviceSelected = { device ->
                                    scanner.stopScan()
                                    showScannerDialog = false
                                    val mgr = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                                    val remoteDevice = mgr?.adapter?.getRemoteDevice(device.address)
                                    remoteDevice?.let { sppManager.connect(it) }
                                },
                                onDismiss = {
                                    scanner.stopScan()
                                    showScannerDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val isEnabled = btManager?.adapter?.isEnabled == true
        sppManager.setBluetoothEnabled(isEnabled)
        if (isEnabled && !sppManager.deviceState.value.isConnected) {
            checkAndAutoConnect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(bluetoothStateReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            checkAndAutoConnect()
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkAndAutoConnect() {
        if (sppManager.deviceState.value.isConnected) return

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter ?: return

        if (!adapter.isEnabled) {
            sppManager.setBluetoothEnabled(false)
            return
        }

        sppManager.setBluetoothEnabled(true)

        try {
            val paired = adapter.bondedDevices
            val nirvana = paired?.find {
                val name = it.name ?: ""
                name.contains("nirvana", ignoreCase = true) ||
                name.contains("boat", ignoreCase = true) ||
                name.contains("space", ignoreCase = true)
            }

            if (nirvana != null) {
                sppManager.connect(nirvana)
            }
        } catch (e: SecurityException) {
            // Missing Bluetooth permission
        }
    }
}
