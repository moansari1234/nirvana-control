package com.nirvana.control

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
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
import com.nirvana.control.ui.components.LogViewerDialog
import com.nirvana.control.ui.navigation.AppTab
import com.nirvana.control.ui.navigation.NirvanaBottomBar
import com.nirvana.control.ui.screens.*
import com.nirvana.control.ui.theme.NirvanaControlTheme
import com.nirvana.control.ui.theme.PureBlack
import com.nirvana.control.util.AppLog as Log

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var sppManager: BluetrumSppManager
    private lateinit var scanner: BluetoothScanner

    private var bondedDevicesState = mutableStateOf<List<BluetoothDevice>>(emptyList())

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        Log.i(TAG, "Permissions result: allGranted=, permissions=")
        if (allGranted) {
            refreshBondedDevices()
            checkAndAutoConnect()
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val isEnabled = btManager?.adapter?.isEnabled == true
        Log.i(TAG, "Bluetooth enable activity result: isEnabled=")
        sppManager.setBluetoothEnabled(isEnabled)
        if (isEnabled) {
            refreshBondedDevices()
            checkAndAutoConnect()
        }
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                val isEnabled = state == BluetoothAdapter.STATE_ON
                Log.i(TAG, "BluetoothAdapter state changed:  (enabled=)")
                sppManager.setBluetoothEnabled(isEnabled)
                if (isEnabled) {
                    refreshBondedDevices()
                    checkAndAutoConnect()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate() started.")

        sppManager = BluetrumSppManager.getInstance(this)
        scanner = BluetoothScanner(this)

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val isEnabled = btManager?.adapter?.isEnabled == true
        Log.i(TAG, "Initial Bluetooth state: enabled=")
        sppManager.setBluetoothEnabled(isEnabled)

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)

        requestRequiredPermissions()

        setContent {
            NirvanaControlTheme {
                val deviceState by sppManager.deviceState.collectAsStateWithLifecycle()
                val scannedDevices by scanner.scannedDevices.collectAsStateWithLifecycle()
                val isScanning by scanner.isScanning.collectAsStateWithLifecycle()
                val bondedDevices by bondedDevicesState

                var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
                var showScannerDialog by remember { mutableStateOf(false) }
                var showLogDialog by remember { mutableStateOf(false) }

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
                                    pairedDevices = bondedDevices,
                                    onSelectDevice = { device ->
                                        Log.i(TAG, "User clicked to connect device:  []")
                                        sppManager.connect(device)
                                    },
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
                                    onOpenLogs = { showLogDialog = true },
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
                                    onOpenLogs = { showLogDialog = true },
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

                        if (showLogDialog) {
                            LogViewerDialog(onDismiss = { showLogDialog = false })
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume() called.")
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val isEnabled = btManager?.adapter?.isEnabled == true
        sppManager.setBluetoothEnabled(isEnabled)
        refreshBondedDevices()
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
            Log.i(TAG, "Requesting runtime permissions: ")
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            Log.i(TAG, "All required permissions already granted.")
            refreshBondedDevices()
            checkAndAutoConnect()
        }
    }

    @SuppressLint("MissingPermission")
    private fun refreshBondedDevices() {
        try {
            val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bonded = btManager?.adapter?.bondedDevices?.toList() ?: emptyList()
            bondedDevicesState.value = bonded
            Log.i(TAG, "Refreshed bonded devices list ( devices)")
        } catch (e: Exception) {
            Log.w(TAG, "Could not query bonded devices: ")
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkAndAutoConnect() {
        Log.i(TAG, "checkAndAutoConnect() invoked.")
        if (sppManager.deviceState.value.isConnected) {
            Log.i(TAG, "Already connected to SPP socket.")
            return
        }

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
        if (adapter == null) {
            Log.e(TAG, "BluetoothAdapter is null on this device!")
            return
        }

        if (!adapter.isEnabled) {
            Log.w(TAG, "Bluetooth is disabled.")
            sppManager.setBluetoothEnabled(false)
            return
        }

        sppManager.setBluetoothEnabled(true)

        try {
            val paired = adapter.bondedDevices ?: emptySet()
            Log.i(TAG, "Inspecting  bonded devices in system:")
            paired.forEach { dev ->
                Log.i(TAG, "  -> Bonded device: '' [] type= bondState=")
            }

            // 1. Try name heuristic match
            var candidateDevice: BluetoothDevice? = paired.find {
                val name = it.name ?: ""
                name.contains("nirvana", ignoreCase = true) ||
                name.contains("boat", ignoreCase = true) ||
                name.contains("space", ignoreCase = true)
            }

            // 2. Query A2DP audio profile proxy to see if earbuds are currently connected for music/audio!
            adapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                    try {
                        if (profile == BluetoothProfile.A2DP) {
                            val a2dp = proxy as? BluetoothA2dp
                            val connectedA2dp = a2dp?.connectedDevices ?: emptyList()
                            Log.i(TAG, "A2DP Profile active! Currently connected audio devices: ")
                            connectedA2dp.forEach { d ->
                                Log.i(TAG, "  -> Active A2DP device: '' []")
                            }

                            // If no device matched by name but an A2DP device is connected, connect to it!
                            if (candidateDevice == null && connectedA2dp.isNotEmpty()) {
                                val a2dpDevice = connectedA2dp[0]
                                Log.i(TAG, "Auto-targeting active A2DP device: '' []")
                                sppManager.connect(a2dpDevice)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in A2DP profile callback: ", e)
                    } finally {
                        try {
                            adapter.closeProfileProxy(BluetoothProfile.A2DP, proxy)
                        } catch (ignored: Exception) {}
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    Log.d(TAG, "A2DP profile proxy disconnected.")
                }
            }, BluetoothProfile.A2DP)

            if (candidateDevice != null) {
                Log.i(TAG, "Auto-connecting to detected Nirvana Space candidate: '' []")
                sppManager.connect(candidateDevice)
            } else {
                Log.w(TAG, "No device matching 'nirvana'/'boat'/'space' found. User can select from paired list.")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while querying bonded devices: ", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during checkAndAutoConnect: ", e)
        }
    }
}
